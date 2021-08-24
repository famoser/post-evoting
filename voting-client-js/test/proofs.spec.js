/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* jshint jasmine:true */
/* global OV */

const forge = require('node-forge');
const mathematicalService = require('cryptolib-js/src/mathematical/index').newService();
const proofService = require('cryptolib-js/src/zkproof/index').newService();
const elgamalService = require('cryptolib-js/src/elgamal/index').newService();
const arrayCompressor = mathematicalService.newArrayCompressor();
const codec = require('cryptolib-js/src/codec/index');

describe('Proofs api', function () {
	'use strict';

	const TD = require('./mocks/testdata.json');

	it('should generate exponentiation proof', function () {
		const startVotingKey = TD.startVotingKey;
		const eventId = TD.eventId;
		const encParms = OV.parseEncryptionParams(TD.authResponse);
		const verificationCard = TD.authResponse.verificationCard;

		const startVotingKeyUnpacked = OV.parseStartVotingKey(
			startVotingKey,
			eventId,
		);

		// encrypt vote
		const serializedEncrypterValues = OV.precomputeEncrypterValues(encParms);
		const encrypterValues = OV.deserializeEncrypterValues(
			serializedEncrypterValues,
		);
		const keyValue = forge.jsbn.BigInteger.ONE;
		const element = mathematicalService.newZpGroupElement(
			encParms.p,
			encParms.q,
			keyValue,
		);
		const messages = [element];
		const encryptedOptions = OV.encryptRho(
			messages,
			encParms,
			encrypterValues,
		);

		const verificationCardPublicKey = JSON.parse(
			codec.utf8Decode(
				codec.base64Decode(verificationCard.signedVerificationPublicKey),
			),
		);
		const verificationCardSecret = OV.parseVerificationCard(
			verificationCard.verificationCardKeystore,
			startVotingKeyUnpacked.keystoreSymmetricEncryptionKey,
		);

		// generate the proof

		const cipherTextExponentiations = OV.generateCipherTextExponentiations(
			encParms,
			encryptedOptions,
			verificationCardSecret,
		);

		const exponentiationProof = OV.generateExponentiationProof(
			encParms,
			cipherTextExponentiations,
			verificationCardPublicKey.publicKey,
		);

		// prove it

		const generator = mathematicalService.newZpGroupElement(
			encParms.p,
			encParms.q,
			encParms.g,
		);

		const publicKey = elgamalService.newPublicKey(
			codec.utf8Decode(codec.base64Decode(verificationCardPublicKey.publicKey)),
		).elements[0];

		const verifies = proofService
			.newExponentiationProofHandler(encParms.group)
			.init([generator].concat(cipherTextExponentiations.cipherTextBases))
			.verify(
				exponentiationProof,
				[publicKey].concat(cipherTextExponentiations.cipherTextExponentiation),
			);
		expect(verifies).toBe(true);
	});

	// --------------------------------------------------------------

	it('should generate plaintext equality proof', function () {
		const startVotingKey = TD.startVotingKey;
		const eventId = TD.eventId;
		const encParms = OV.parseEncryptionParams(TD.authResponse);
		const verificationCard = TD.authResponse.verificationCard;

		const startVotingKeyUnpacked = OV.parseStartVotingKey(
			startVotingKey,
			eventId,
		);
		const verificationCardSecret = OV.parseVerificationCard(
			verificationCard.verificationCardKeystore,
			startVotingKeyUnpacked.keystoreSymmetricEncryptionKey,
		);

		// encrypt vote

		const serializedEncrypterValues = OV.precomputeEncrypterValues(encParms);
		const encrypterValues = OV.deserializeEncrypterValues(
			serializedEncrypterValues,
		);
		const keyValue = forge.jsbn.BigInteger.ONE;
		const element = mathematicalService.newZpGroupElement(
			encParms.p,
			encParms.q,
			keyValue,
		);
		const messages = [element];
		const encryptedOptions = OV.encryptRho(
			messages,
			encParms,
			encrypterValues,
		);

		// generate and encrypt partial choice return codes

		const partialChoiceCodes = OV.generatePartialChoiceCodes(
			messages,
			encParms,
			verificationCardSecret,
		);
		const encryptedPCC = OV.encryptPartialChoiceCodes(
			partialChoiceCodes,
			encParms,
		);

		// generate the proof

		const cipherTextExponentiations = OV.generateCipherTextExponentiations(
			encParms,
			encryptedOptions,
			verificationCardSecret,
		);

		const plaintextEqualityProof = OV.generatePlaintextEqualityProof(
			encParms,
			encryptedOptions,
			encryptedPCC,
			verificationCardSecret,
			cipherTextExponentiations,
		);

		// prove it

		const primaryCiphertext = (function () {
			const exponentiatedVoteCiphertext = mathematicalService
				.newGroupHandler()
				.exponentiateElements(
					encParms.group,
					[encryptedOptions.gamma].concat(encryptedOptions.phis), // base elements
					mathematicalService.newExponent(encParms.q, verificationCardSecret), // exponent
				);

			return {
				gamma: exponentiatedVoteCiphertext[0],
				phis: exponentiatedVoteCiphertext.slice(1),
			};
		})();

		const secondaryCipherText = (function () {
			const compressedPRCphis = arrayCompressor.compressZpGroupElements(
				encryptedPCC.phis,
			);

			const gamma = encryptedPCC.gamma;

			return {
				gamma: gamma,
				phis: [compressedPRCphis],
			};
		})();

		const ebPubKey = encParms.optionsEncryptionKey;
		const prcPubKey = encParms.choiceCodesEncryptionKey;

		const primaryPublicKey = ebPubKey;
		const secondaryPublicKey = (function () {
			const compressedSecondaryPublicKey = arrayCompressor.compressZpGroupElements(
				prcPubKey.elements,
			);

			return {
				group: encParms.group,
				elements: [compressedSecondaryPublicKey],
			};
		})();

		const verifies = proofService
			.newPlaintextEqualityProofHandler(encParms.group)
			.init(primaryPublicKey, secondaryPublicKey)
			.verify(plaintextEqualityProof, primaryCiphertext, secondaryCipherText);
		expect(verifies).toBe(true);
	});
});
