/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global OV */

const codec = require('cryptolib-js/src/codec');

module.exports = (function () {
	'use strict';

	const jsrsasign = require('jsrsasign');

	const parseEncryptionParams = function (response) {
		const verificationCardSet = response.verificationCardSet.data
			? response.verificationCardSet.data
			: response.verificationCardSet;

		const ballotBox = jsrsasign.jws.JWS.parse(response.ballotBox.signature)
			.payloadObj.objectToSign;

		const electionPublicKey = jsrsasign.jws.JWS.parse(
			response.ballotBox.electionPublicKey.signature,
		).payloadObj.objectToSign;

		return new OV.model.EncryptionParams({
			serializedP: ballotBox.encryptionParameters.p,
			serializedQ: ballotBox.encryptionParameters.q,
			serializedG: ballotBox.encryptionParameters.g,
			serializedOptionsEncryptionKey: codec.utf8Decode(
				codec.base64Decode(electionPublicKey.publicKey),
			),
			serializedChoiceCodesEncryptionKey: codec.utf8Decode(
				codec.base64Decode(verificationCardSet.choicesCodesEncryptionPublicKey),
			),
		});
	};

	return parseEncryptionParams;
})();
