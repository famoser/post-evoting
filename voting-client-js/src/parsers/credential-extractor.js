/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* jshint maxlen: 6666 */
const codec = require('cryptolib-js/src/codec');
const keyStoreService = require('cryptolib-js/src/extendedkeystore').newService();

module.exports = (function () {
	'use strict';

	// Stores voting protocol parameters and keys

	const extractCredentials = function (credentialData, pass) {
		let ksObject, ks, certificateAuth, certificateSign;

		try {
			ksObject = JSON.parse(
				codec.utf8Decode(codec.base64Decode(credentialData.data)),
			);
		} catch (e) {
			throw new Error('Invalid credential data');
		}

		try {
			ks = keyStoreService.newExtendedKeyStore(ksObject, pass);
			certificateAuth = ks.getCertificateBySubject('Auth ' + credentialData.id);
			certificateSign = ks.getCertificateBySubject('Sign ' + credentialData.id);
		} catch (e) {
			throw new Error('Could not access keystore');
		}

		return {
			//credentialData.id
			voterPrivateKey: ks.getPrivateKey('sign', pass),
			authPrivateKey: ks.getPrivateKey('auth_sign', pass),
			certificate: certificateSign,
			certificateAuth: certificateAuth,
			credentialId: credentialData.id,
		};
	};
	return extractCredentials;
})();
