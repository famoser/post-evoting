/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const asymmetricService = require('cryptolib-js/src/asymmetric').newService();
const signer = asymmetricService.newSigner();
const verifier = asymmetricService.newSignatureVerifier();
const codec = require('cryptolib-js/src/codec');

module.exports = (function () {
	'use strict';

	// receives and array of data , which will be concatenated and
	// signed with the given private key
	const signData = function (data, privateKey) {
		return codec.base64Encode(signer.init(privateKey).sign(data.join('')));
	};

	// receives and array of data , which will be concatenated and
	// used to verify the given signature with the given public key
	const verifySignature = function (data, publicKey, signature) {
		return verifier
			.init(publicKey)
			.verify(codec.base64Decode(signature), data.join(''));
	};

	return {
		signData: signData,
		verifySignature: verifySignature,
	};
})();
