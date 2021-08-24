/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global require */
/* global OV */

module.exports = (function () {
	'use strict';

	const XMLHttpRequest = XMLHttpRequest || require('xhr2');
	const Q = require('q');
	const config = require('./config.js');
	const session = require('./session.js');

	const createConfirmRequest = function (ballotCastingKey) {
		// create confirmation message

		let confirmationKey;

		confirmationKey = OV.generateConfirmationKey(
			ballotCastingKey,
			session('encParams'),
			session('verificationCardSecret'),
		);

		// sign confirmation message

		const dataToSign = [
			confirmationKey,
			session('authenticationToken').signature,
			session('votingCardId'),
			config('electionEventId'),
		];
		const confirmationKeySignature = OV.signData(
			dataToSign,
			session('credentials').voterPrivateKey,
		);

		// send confirmation

		return {
			credentialId: session('credentials').credentialId,
			confirmationMessage: {
				confirmationKey: confirmationKey,
				signature: confirmationKeySignature,
			},
			certificate: session('credentials').certificate,
		};
	};

	const JSONparse = function (response) {
		let ret;
		try {
			ret = JSON.parse(response);
		} catch (ignore) {
			ret = {
				error: response,
			};
		}
		return ret;
	};

	const processConfirmResponse = function (confirmResponse) {
		return OV.parseCastResponse(
			confirmResponse,
			session('credentials').certificate,
			session('certificates')['authenticationTokenSignerCert'],
			session('ballotBoxCert'),
			session('votingCardId'),
			config('electionEventId'),
			session('verificationCardId'),
		);
	};

	// generate and send confirmationmessage

	const castVote = function (ballotCastingKey) {
		const deferred = Q.defer();

		const reqData = createConfirmRequest(ballotCastingKey);

		// send confirmation

		const endpoint = config('endpoints.confirmations')
			.replace('{tenantId}', config('tenantId'))
			.replace('{electionEventId}', config('electionEventId'))
			.replace('{votingCardId}', session('votingCardId'));

		const xhr = new XMLHttpRequest();
		xhr.open('POST', config('host') + endpoint);
		xhr.onreadystatechange = function () {
			let response;

			if (xhr.readyState === 4) {
				if (xhr.status === 200) {
					response = JSONparse(this.responseText);
					if (response.valid) {
						let result;
						try {
							result = processConfirmResponse(response);
							deferred.resolve(result);
						} catch (e) {
							deferred.reject(e.message);
						}
					} else {
						if (response.validationError) {
							deferred.reject(response);
						} else {
							deferred.reject('invalid vote');
						}
					}
				} else {
					const resp = this.responseText ? JSONparse(this.responseText) : null;
					if (resp && resp.validationError) {
						deferred.reject(resp);
					} else {
						deferred.reject(xhr.status);
					}
				}
			}
		};
		xhr.onerror = function () {
			try {
				deferred.reject(xhr.status);
			} catch (e) {
				//This block is intentionally left blank
			}
		};
		xhr.setRequestHeader('Accept', 'application/json');
		xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
		xhr.setRequestHeader(
			'authenticationToken',
			JSON.stringify(session('authenticationToken')),
		);
		xhr.send(JSON.stringify(reqData));

		return deferred.promise;
	};

	return {
		castVote: castVote,
		createConfirmRequest: createConfirmRequest,
		processConfirmResponse: processConfirmResponse,
	};
})();
