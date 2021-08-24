/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* jshint maxlen: 6666 */

const elGamalService = require('cryptolib-js/src/elgamal').newService();
const mathematicalService = require('cryptolib-js/src/mathematical').newService();

module.exports = (function () {
	'use strict';

	/**
	 * Encrypt the "options" elements using a newly initialized ElGamal algorithm encrypter.
	 *
	 * @param options {Array<ZpGroupElement>} The voting options expressed as prime numbers.
	 * @param encryptionKey {ElGamalKeys} The encryption keys.
	 * @param encrypterValues (optional) An object containing two objects of type model.EncrypterValues. One object contains the pre-computations
	 *                          for the voting options encryption ("options") and the other object the pre-computations for the partial
	 *                          choice return codes pCC encryption.
	 */
	const encryptEG = function (options, encryptionKey, encrypterValues) {
		const encrypter = elGamalService.newEncrypter().init(encryptionKey);

		// prepare precomputed encrypterValues if supplied

		const encrypterOptions = {saveSecret: true};
		if (encrypterValues) {
			encrypterOptions.preComputation = encrypterValues;
		}

		return encrypter.encrypt(options, encrypterOptions);
	};

	/**
	 * Encrypted rho (the product of selected primes). Voting options are encoded as small primes.
	 *
	 * @param rho {ZpGroupElement} The multiplied selected voting options. Voting options are encoded as small prime numbers.
	 * @param encryptionParams {model.EncryptionParams} The parameters of the mathematical subgroup (p, q, and g)
	 *                          and the choice return codes encryption public key pk_CCR and the election public key EL_pk.
	 * @param encrypterValues (optional) An object containing two objects of type model.EncrypterValues. One object contains the pre-computations
	 *                          for the voting options encryption ("options") and the other object the pre-computations for the partial
	 *                          choice return codes pCC encryption.
	 */
	const encryptRho = function (
		rho,
		encryptionParams,
		encrypterValues,
	) {
		let egEncrypterValues;
		if (encrypterValues && encrypterValues.options) {
			egEncrypterValues = encrypterValues.options;
		}

		// Take only the first element of the election public key.
		const publicKey = {
			group: encryptionParams.optionsEncryptionKey.group,
			elements: [encryptionParams.optionsEncryptionKey.elements[0]]
		}

		return encryptEG([rho], publicKey, egEncrypterValues);
	};

	/**
	 * Encrypt the partial choice return codes using the choice return codes encryption public key, the p and q groups, and the precomputed encrypter values.
	 *
	 * @param pCCBigIntegers {Array<BigInteger>} The partial choice return codes as big integers.
	 * @param encryptionParams {model.EncryptionParams} The parameters of the mathematical subgroup (p, q, and g)
	 *                                                  and the choice return codes encryption public key pk_CCR and the election public key EL_pk.
	 * @param encrypterValues (optional) An object containing two objects of type model.EncrypterValues. One object contains the pre-computations
	 *                          for the voting options encryption ("options") and the other object the pre-computations for the partial
	 *                          choice return codes pCC encryption.
	 */
	const encryptPartialChoiceCodes = function (
		pCCBigIntegers,
		encryptionParams,
		encrypterValues,
	) {
		// We convert the partial Choice Return Codes to group elements.
		const pCC = pCCBigIntegers.map(function (x) {
			return mathematicalService.newZpGroupElement(
				encryptionParams.group.p,
				encryptionParams.group.q,
				x,
			);
		});

		let egEncrypterValues = null;
		if (encrypterValues && encrypterValues.choicecodes) {
			egEncrypterValues = encrypterValues.choicecodes;
		}

		return encryptEG(
			pCC,
			encryptionParams.choiceCodesEncryptionKey,
			egEncrypterValues,
		);
	};

	return {
		encryptRho: encryptRho,
		encryptPartialChoiceCodes: encryptPartialChoiceCodes
	};
})();
