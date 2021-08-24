/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* jshint maxlen: 6666 */

const mathematicalService = require('cryptolib-js/src/mathematical').newService();
const secureRandomService = require('cryptolib-js/src/securerandom').newService();
const elGamalService = require('cryptolib-js/src/elgamal').newService();
const codec = require('cryptolib-js/src/codec');

module.exports = (function () {
	'use strict';

	const computeValues = function (encryptionParams, randomFactory, key) {
		const rve = randomFactory.nextBigInteger(256);
		const C0 = encryptionParams.g.modPow(rve, encryptionParams.p);
		const preC1 = key.elements.map(function (k) {
			return k.value.modPow(rve, encryptionParams.p);
		});

		return {
			rve: mathematicalService.newExponent(encryptionParams.q, rve).toJson(),
			C0: mathematicalService
				.newZpGroupElement(encryptionParams.p, encryptionParams.q, C0)
				.toJson(),
			preC1: preC1.map(function (x) {
				return mathematicalService
					.newZpGroupElement(encryptionParams.p, encryptionParams.q, x)
					.toJson();
			}),
		};
	};

	// Precompute ElGamal encryption values. NOTE: This method returns an object containing TWO objects of
	// type model.EncrypterValues, "options" and "choiceCodes".
	const precomputeEncryptionValues = function (serializedEncParams) {
		let serializedOptionsEncryptionKey, serializedChoiceCodesEncryptionKey;
		if (serializedEncParams.serializedOptionsEncryptionKey !== 'undefined') {
			serializedOptionsEncryptionKey =
				serializedEncParams.serializedOptionsEncryptionKey;
		}
		if (
			serializedEncParams.serializedChoiceCodesEncryptionKey !== 'undefined'
		) {
			serializedChoiceCodesEncryptionKey =
				serializedEncParams.serializedChoiceCodesEncryptionKey;
		}

		const encryptionParams = new OV.model.EncryptionParams({
			serializedP: serializedEncParams.serializedP,
			serializedQ: serializedEncParams.serializedQ,
			serializedG: serializedEncParams.serializedG,
			serializedOptionsEncryptionKey: serializedOptionsEncryptionKey,
			serializedChoiceCodesEncryptionKey: serializedChoiceCodesEncryptionKey,
		});

		const randomFactory = secureRandomService.newRandomGenerator();
		return {
			options: computeValues(
				encryptionParams,
				randomFactory,
				encryptionParams.optionsEncryptionKey,
			),
			choicecodes: computeValues(
				encryptionParams,
				randomFactory,
				encryptionParams.choiceCodesEncryptionKey,
			),
		};
	};

	// Deserialize ElGamal encryption values. NOTE: This method returns an object containing TWO objects of
	// type model.EncrypterValues, "options" and "choiceCodes".
	const deserializeEncryptionValues = function (serializedEncryptionValues) {
		const deserializeValues = function (serializedValues) {
			const gamma = mathematicalService.newZpGroupElement(serializedValues.C0);
			const phis = serializedValues.preC1.map(function (serializedPhi) {
				return mathematicalService.newZpGroupElement(serializedPhi);
			});
			const secret = mathematicalService.newExponent(serializedValues.rve);

			return elGamalService.newEncryptedElements(gamma, phis, secret);
		};

		return {
			options: deserializeValues(serializedEncryptionValues.options),
			choicecodes: deserializeValues(serializedEncryptionValues.choicecodes),
		};
	};

	return {
		deserializeEncryptionValues: deserializeEncryptionValues,
		precomputeEncryptionValues: precomputeEncryptionValues,
	};
})();
