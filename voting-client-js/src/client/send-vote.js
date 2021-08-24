/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* jshint ignore: start */
/* zglobal require */
/* zglobal forge */
/* zglobal CL */
/* zglobal OV */
/* zglobal self */

const mathematicalService = require('cryptolib-js/src/mathematical').newService();
const arrayCompressor = mathematicalService.newArrayCompressor();
const forge = require('node-forge');

module.exports = (function () {
	'use strict';

	const XMLHttpRequest = XMLHttpRequest || require('xhr2');
	const _ = require('lodash');
	const Q = require('q');
	const config = require('./config.js');
	const session = require('./session.js');

	// lenient json parse

	const jsonparse = function (x) {
		try {
			return JSON.parse(x);
		} catch (ignore) {
			return {};
		}
	};

	// create a vote request

	const createVoteRequest = function (
		encodedVotingOptionsInt,
		serializedEncrypterValues,
		precomputedPCC,
		precomputeProofValues,
		correctness
	) {

		if (session('encParams').optionsEncryptionKey.elements.length !== 1) {
			throw new Error('The election public key should have exactly 1 element');
		}

		// convert encodedVotingOptionsInt to group elements

		const encodedVotingOptions = encodedVotingOptionsInt.map(function (o) {
			return mathematicalService.newZpGroupElement(
				session('encParams').p,
				session('encParams').q,
				new forge.jsbn.BigInteger(o),
			);
		});

		// get correctnessIds

		const correctnessIds = encodedVotingOptionsInt.map(function (o) {
			return correctness[o] || [];
		});

		let encrypterValues = null;
		if (serializedEncrypterValues) {
			encrypterValues = OV.deserializeEncrypterValues(
				serializedEncrypterValues,
			);
		}

		// encrypt rho

		const rho = arrayCompressor.compressZpGroupElements(encodedVotingOptions);

		// encryptedOptions represent E1 from the protocol
		const encryptedOptions = OV.encryptRho(
			rho,
			session('encParams'),
			encrypterValues,
		);

		// generate partial choice codes

		const partialChoiceCodes = OV.generatePartialChoiceCodes(
			encodedVotingOptions,
			session('encParams'),
			session('verificationCardSecret'),
			precomputedPCC,
		);

		// encrypt partial choice codes

		const encryptedPCC = OV.encryptPartialChoiceCodes(
			partialChoiceCodes,
			session('encParams'),
			encrypterValues,
		);

		// generate cipher text exponentiations

		const cipherTextExponentiations = OV.generateCipherTextExponentiations(
			session('encParams'),
			encryptedOptions,
			session('verificationCardSecret'),
		);

		// generate exponentiation proof

		const exponentiationProof = OV.generateExponentiationProof(
			session('encParams'),
			cipherTextExponentiations,
			session('verificationCardPublicKey').publicKey,
			precomputeProofValues,
		);

		// generate plaintextEquality proof

		const plaintextEqualityProof = OV.generatePlaintextEqualityProof(
			session('encParams'),
			encryptedOptions,
			encryptedPCC,
			session('verificationCardSecret'),
			cipherTextExponentiations,
			precomputeProofValues,
		);

		// sign vote

		const serializedEncryptedOptions = `${encryptedOptions.gamma.value.toString()};${encryptedOptions.phis[0].value.toString()}`;

		let serializedEncryptedPCC = encryptedPCC.gamma.value.toString();
		_.each(encryptedPCC.phis, function (phi) {
			serializedEncryptedPCC += ';' + phi.value.toString();
		});
		const serializedCiphertextExponentiations =
			`${cipherTextExponentiations.cipherTextExponentiation[0].value.toString()};${cipherTextExponentiations.cipherTextExponentiation[1].value.toString()}`;

		const serializedCorrectnessIds = JSON.stringify(correctnessIds);

		const dataToSign = [
			serializedEncryptedOptions,
			serializedCorrectnessIds,
			session('verificationCardPublicKey').signature,
			session('authenticationToken').signature,
			session('votingCardId'),
			config('electionEventId'),
		];
		const signature = OV.signData(dataToSign, session('credentials').voterPrivateKey);

		return {
			encryptedOptions: serializedEncryptedOptions,
			encryptedPartialChoiceCodes: serializedEncryptedPCC,
			correctnessIds: serializedCorrectnessIds,
			verificationCardPublicKey: session('verificationCardPublicKey').publicKey,
			verificationCardPKSignature: session('verificationCardPublicKey')
				.signature,
			signature: signature,
			certificate: session('credentials').certificate,
			credentialId: session('credentials').credentialId,
			exponentiationProof: exponentiationProof.toJson(),
			plaintextEqualityProof: plaintextEqualityProof.toJson(),
			cipherTextExponentiations: serializedCiphertextExponentiations
		};
	};

	// process vote response

	const processVoteResponse = function (response) {
		return response.choiceCodes.split(';');
	};

	// process vote

	const processVote = function (deferred, response) {
		if (response.valid && response.choiceCodes) {
			try {
				const result = processVoteResponse(response);
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
	};

	// encrypt and send the vote

	const sendVote = function (
		options,
		encrypterValues,
		precomputedPCC,
		precomputeProofValues,
		correctness
	) {
		const deferred = Q.defer();

		const voteRequestData = createVoteRequest(
			options,
			encrypterValues,
			precomputedPCC,
			precomputeProofValues,
			correctness
		);

		// send vote

		const endpoint = config('endpoints.votes')
			.replace('{tenantId}', config('tenantId'))
			.replace('{electionEventId}', config('electionEventId'))
			.replace('{votingCardId}', session('votingCardId'));

		const xhr = new XMLHttpRequest();
		xhr.open('POST', config('host') + endpoint);
		xhr.onreadystatechange = function () {
			if (xhr.readyState === 4) {
				if (xhr.status === 200) {
					processVote(deferred, JSON.parse(this.responseText));
				} else {
					let response = jsonparse(this.responseText);
					if (!response || typeof response !== 'object') {
						response = {};
					}
					// IE11 fails to provide the 401 status to xhr2 (yields 0 instead)
					response.httpStatus = xhr.status || 401;
					response.httpStatusText = xhr.statusText;
					deferred.reject(response);
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
		xhr.send(JSON.stringify(voteRequestData));

		return deferred.promise;
	};

	return {
		sendVote: sendVote,
		createVoteRequest: createVoteRequest,
		processVoteResponse: processVoteResponse,
	};
})();
