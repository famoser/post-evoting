/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const forge = require('node-forge');

const codec = require('cryptolib-js/src/codec');

module.exports = (function () {
	'use strict';

	// generate the confirmation key for confirmation message

	// @param ballotCastingKey {String} 9 numerical digits
	// @param encryptionParms {model.EncryptionParms} The encryption parameters and key
	// @param verificationCardSecretKey {BigInteger} verification card secret key

	// @return {String} base 64 encoded confirmation key

	const generateConfirmationKey = function (
		ballotCastingKey,
		encryptionParms,
		verificationCardSecretKey,
	) {
		// compute encoded BCK

		const bck = new forge.jsbn.BigInteger(ballotCastingKey);
		const big2 = new forge.jsbn.BigInteger('2');
		const encodedBCK = bck.modPow(big2, encryptionParms.p);

		// compute confirmation message

		const confirmationKey = encodedBCK.modPow(
			verificationCardSecretKey,
			encryptionParms.p,
		);

		return codec.base64Encode(confirmationKey.toString());
	};

	return generateConfirmationKey;
})();
