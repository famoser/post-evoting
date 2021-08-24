/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global OV */
const mathematicalService = require('cryptolib-js/src/mathematical').newService();
const forge = require('node-forge');
/* jshint maxlen: 6666 */

module.exports = (function () {
	'use strict';

	// Precompute a partial choice code

	const precomputePartialChoiceCode = function (
		serializedEncParams,
		serializedOption,
		serializedExponent,
	) {
		// nothing to do if we already have this partial choice code

		let codesPool = OV.session('pccPool');
		if (!codesPool) {
			// no pool yet?
			codesPool = [];
			OV.session('pccPool', codesPool); // create it
		} else if (codesPool[serializedOption]) {
			// pcc already known?
			return; // we're done
		}

		// get encryption params if not already cached

		let encryptionParms = OV.session('encParams');
		if (!encryptionParms) {
			encryptionParms = new OV.model.EncryptionParams(serializedEncParams);
			OV.session('encParams', encryptionParms);
		}

		// get exponent if not already cached

		let exponent = OV.session('maskExponent');
		if (!exponent) {
			exponent = new forge.jsbn.BigInteger(serializedExponent);
			OV.session('maskExponent', exponent); // cache it
		}

		// deserialize option

		const option = mathematicalService.newZpGroupElement(
			encryptionParms.p,
			encryptionParms.q,
			new forge.jsbn.BigInteger(serializedOption),
		);

		// compute and store

		const pcc = option.value.modPow(exponent, encryptionParms.p);
		codesPool[serializedOption] = pcc.toString();
	};

	const getPrecomputedPartialChoiceCodes = function () {
		return OV.session('pccPool') || [];
	};

	return {
		precomputePartialChoiceCode: precomputePartialChoiceCode,
		getPrecomputedPartialChoiceCodes: getPrecomputedPartialChoiceCodes,
	};
})();
