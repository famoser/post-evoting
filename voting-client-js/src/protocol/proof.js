/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* jshint maxlen: 6666 */

const proofService = require('cryptolib-js/src/zkproof').newService();
const mathematicalService = require('cryptolib-js/src/mathematical').newService();
const elgamalService = require('cryptolib-js/src/elgamal').newService();
const arrayCompressor = mathematicalService.newArrayCompressor();
const codec = require('cryptolib-js/src/codec');

module.exports = (function () {
	'use strict';

	const generateCipherTextExponentiations = function (
		encryptionParms,
		voteEncryption,
		verificationCardSecret,
	) {
		const exponent = mathematicalService.newExponent(
			encryptionParms.q,
			verificationCardSecret,
		);
		const cipherTextBases = [voteEncryption.gamma].concat(voteEncryption.phis[0]);
		const cipherTextExponentiation = mathematicalService
			.newGroupHandler()
			.exponentiateElements(encryptionParms.group, cipherTextBases, exponent);

		return {
			cipherTextBases: cipherTextBases,
			cipherTextExponentiation: cipherTextExponentiation,
			exponent: exponent,
		};
	};

	const generateExponentiationProof = function (
		encryptionParms,
		cipherTextExponentiations,
		verificationCardPublicKey,
	) {

		const publicKey = elgamalService.newPublicKey(
			codec.utf8Decode(codec.base64Decode(verificationCardPublicKey)),
		).elements[0];
		const generator = mathematicalService.newZpGroupElement(
			encryptionParms.p,
			encryptionParms.q,
			encryptionParms.g,
		);

		return proofService
			.newExponentiationProofHandler(encryptionParms.group)
			.init([generator].concat(cipherTextExponentiations.cipherTextBases))
			.generate(
				cipherTextExponentiations.exponent,
				[publicKey].concat(cipherTextExponentiations.cipherTextExponentiation),
			);
	};

	const generatePlaintextEqualityProof = function (
		encryptionParms,
		voteEncryption,
		prcEncryption,
		verificationCardSecret,
		cipherTextExponentiations,
		precomputeProofValues,
	) {
		const generatorOptions = {};
		if (precomputeProofValues && precomputeProofValues.plaintextEquality) {
			generatorOptions.preComputation = proofService.newPreComputation(
				precomputeProofValues.plaintextEquality,
			);
		}

		const ebPubKey = encryptionParms.optionsEncryptionKey;
		const prcPubKey = encryptionParms.choiceCodesEncryptionKey;

		const primaryCiphertext = (function () {
			// use cipherTextExponentiations if supplied, else compute them
			const exponentiatedVoteCiphertext = cipherTextExponentiations
				? cipherTextExponentiations.cipherTextExponentiation
				: mathematicalService.newGroupHandler().exponentiateElements(
					encryptionParms.group,
					[voteEncryption.gamma].concat(voteEncryption.phis[0]), // base elements
					mathematicalService.newExponent(
						encryptionParms.q,
						verificationCardSecret,
					), // exponent
				);

			return {
				gamma: exponentiatedVoteCiphertext[0],
				phis: exponentiatedVoteCiphertext.slice(1),
			};
		})();

		const primaryPublicKey = (function () {
			const firstSubKey = [];
			firstSubKey.push(ebPubKey.elements[0]);

			return {
				group: encryptionParms.group,
				elements: firstSubKey,
			};
		})();

		const primaryWitness = voteEncryption.secret.multiply(
			mathematicalService.newExponent(
				encryptionParms.q,
				verificationCardSecret,
			),
		);

		const secondaryCipherText = (function () {
			const compressedPRCphis = arrayCompressor.compressZpGroupElements(
				prcEncryption.phis,
			);

			const gamma = prcEncryption.gamma;

			return {
				gamma: gamma,
				phis: [compressedPRCphis],
			};
		})();

		const secondaryPublicKey = (function () {
			const compressedSecondaryPublicKey = arrayCompressor.compressZpGroupElements(
				prcPubKey.elements,
			);

			return {
				group: encryptionParms.group,
				elements: [compressedSecondaryPublicKey],
			};
		})();

		const secondaryWitness = prcEncryption.secret;

		return proofService
			.newPlaintextEqualityProofHandler(encryptionParms.group)
			.init(primaryPublicKey, secondaryPublicKey)
			.generate(
				primaryWitness,
				secondaryWitness,
				primaryCiphertext,
				secondaryCipherText,
				generatorOptions,
			);
	};

	return {
		generateCipherTextExponentiations: generateCipherTextExponentiations,
		generateExponentiationProof: generateExponentiationProof,
		generatePlaintextEqualityProof: generatePlaintextEqualityProof,
	};
})();
