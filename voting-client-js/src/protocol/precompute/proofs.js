/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global OV */
/* jshint maxlen: 6666 */

const proofService = require('cryptolib-js/src/zkproof').newService();
const mathematicalService = require('cryptolib-js/src/mathematical').newService();
const arrayCompressor = mathematicalService.newArrayCompressor();
module.exports = (function () {
	'use strict';

	const precomputeExponentiationProof = function (encrypterValues, encParams) {
		if (!encrypterValues) {
			return null;
		} else {
			const generator = mathematicalService.newZpGroupElement(
				encParams.p,
				encParams.q,
				encParams.g,
			);

			const exponentiationProofHandler = proofService
				.newExponentiationProofHandler(encParams.group)
				.init([generator, encrypterValues.options.gamma]);
			return exponentiationProofHandler.preCompute();
		}
	};

	const precomputePlainTextEqualityProof = function (encParams) {
		const ebPubKey = encParams.optionsEncryptionKey;
		const prcPubKey = encParams.choiceCodesEncryptionKey;
		const primaryPublicKey = (function () {
			const firstSubKey = [];
			firstSubKey.push(ebPubKey.elements[0]);

			return {
				group: encParams.group,
				elements: firstSubKey,
			};
		})();

		const secondaryPublicKey = (function () {
			const compressedSecondaryPublicKey = arrayCompressor.compressZpGroupElements(
				prcPubKey.elements,
			);

			return {
				group: encParams.group,
				elements: [compressedSecondaryPublicKey],
			};
		})();

		const plaintextEqualityProoffHandler = proofService
			.newPlaintextEqualityProofHandler(encParams.group)
			.init(primaryPublicKey, secondaryPublicKey);
		return plaintextEqualityProoffHandler.preCompute();
	};

	const precomputeProofs = function (
		serializedEncParams,
		serializedEncrypterValues,
	) {

		const encParams = new OV.model.EncryptionParams({
			serializedP: serializedEncParams.serializedP,
			serializedQ: serializedEncParams.serializedQ,
			serializedG: serializedEncParams.serializedG,
			serializedOptionsEncryptionKey:
			serializedEncParams.serializedOptionsEncryptionKey,
			serializedChoiceCodesEncryptionKey:
			serializedEncParams.serializedChoiceCodesEncryptionKey,
		});

		let encrypterValues = null;
		if (serializedEncrypterValues) {
			encrypterValues = OV.deserializeEncrypterValues(
				serializedEncrypterValues,
			);
		}

		return {
			exponentiation: precomputeExponentiationProof(
				encrypterValues,
				encParams,
			).toJson(),
			plaintextEquality: precomputePlainTextEqualityProof(encParams).toJson(),
		};
	};

	return {
		precomputeProofs: precomputeProofs,
	};
})();
