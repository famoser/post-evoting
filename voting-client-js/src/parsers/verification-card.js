/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const keyStoreService = require('cryptolib-js/src/extendedkeystore').newService();
const codec = require('cryptolib-js/src/codec');

module.exports = (function () {
	'use strict';

	// Stores voting protocol parameters and keys
	/* jshint unused:false */
	const extractVerificationCard = function (verificationData, pass) {
		let ksObject, ks, voterSecretKey, aliases, egKey;

		try {
			ksObject = JSON.parse(
				codec.utf8Decode(codec.base64Decode(verificationData)),
			);
		} catch (e) {
			throw new Error('Invalid verification data: ' + e.message);
		}
		try {
			ks = keyStoreService.newExtendedKeyStore(ksObject, pass);
			aliases = Object.keys(ksObject.egPrivKeys);
			egKey = ks.getElGamalPrivateKey(aliases[0], pass);
			voterSecretKey = egKey.exponents[0].value;
		} catch (e) {
			throw new Error('Could not access keystore: ' + e.message);
		}

		return voterSecretKey;
	};

	return extractVerificationCard;
})();
