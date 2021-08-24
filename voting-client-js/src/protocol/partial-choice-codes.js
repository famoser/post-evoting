/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const forge = require('node-forge');

module.exports = (function () {
	'use strict';

	// generate partial choice codes

	// @param options {<Array<ZpGroupElement>} The vote option prime numbers
	// @param encryptionParms {model.EncryptionParms} The encryption parameters and key
	// @param exponent {BigInteger} The exponent
	// @param precomputedPCC {String[]} PrecomputedPCC

	const generatePartialChoiceCodes = function (
		options,
		encryptionParms,
		exponent,
		precomputedPCC,
	) {
		const pccPool = precomputedPCC || [];

		return options.map(function (o) {
			const pcc = pccPool[o.value.toString()];

			if (pcc) {
				return new forge.jsbn.BigInteger(pcc);
			} else {
				return o.value.modPow(exponent, encryptionParms.p);
			}
		});
	};

	return generatePartialChoiceCodes;
})();
