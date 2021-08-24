/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global OV */
/* jshint maxlen: 6666 */

const mathematicalService = require('cryptolib-js/src/mathematical/index').newService();
const arrayCompressor = mathematicalService.newArrayCompressor();
const proofService = require('cryptolib-js/src/zkproof/index').newService();
const elgamalService = require('cryptolib-js/src/elgamal/index').newService();
const codec = require('cryptolib-js/src/codec/index');
const forge = require('node-forge');

describe('Precomputations api', function () {
	'use strict';

	const TD = require('./mocks/testdata.json');

	let eventId,
		encParms,
		messages,
		startVotingKey,
		verificationCard,
		verificationCardSecret,
		verificationCardPublicKey;

	beforeEach(function () {
		encParms = OV.parseEncryptionParams(TD.authResponse);
		messages = [
			mathematicalService.newZpGroupElement(
				encParms.p,
				encParms.q,
				new forge.jsbn.BigInteger('1'),
			),
		];
		eventId = TD.eventId;
		startVotingKey = OV.parseStartVotingKey(TD.startVotingKey, eventId);
		encParms = OV.parseEncryptionParams(TD.authResponse);
		verificationCard = TD.authResponse.verificationCard;
		verificationCardPublicKey = JSON.parse(
			codec.utf8Decode(
				codec.base64Decode(verificationCard.signedVerificationPublicKey),
			),
		);
		verificationCardSecret = OV.parseVerificationCard(
			verificationCard.verificationCardKeystore,
			startVotingKey.keystoreSymmetricEncryptionKey,
		);
	});

	const encryptAndProve = function (
		precomputedEncrypterValues,
		precomputedProofValues,
	) {
		const start = Date.now();

		// generate pcc and encrypt

		const encryptedOptions = OV.encryptRho(
			messages,
			encParms,
			precomputedEncrypterValues,
		);
		const partialChoiceCodes = OV.generatePartialChoiceCodes(
			messages,
			encParms,
			verificationCardSecret,
		);
		const encryptedPCC = OV.encryptPartialChoiceCodes(
			partialChoiceCodes,
			encParms,
			precomputedEncrypterValues,
		);

		// generate proofs

		const cipherTextExponentiations = OV.generateCipherTextExponentiations(
			encParms,
			encryptedOptions,
			verificationCardSecret,
		);
		const exponentiationProof = OV.generateExponentiationProof(
			encParms,
			cipherTextExponentiations,
			verificationCardPublicKey.publicKey,
			precomputedProofValues,
		);
		const plaintextEqualityProof = OV.generatePlaintextEqualityProof(
			encParms,
			encryptedOptions,
			encryptedPCC,
			verificationCardSecret,
			cipherTextExponentiations,
			precomputedProofValues,
		);

		const elapsed = Date.now() - start;

		// prove them

		const primaryCiphertext = (function () {
			const exponentiatedVoteCiphertext = mathematicalService
				.newGroupHandler()
				.exponentiateElements(
					encParms.group,
					[encryptedOptions.gamma].concat(encryptedOptions.phis), // base elements
					mathematicalService.newExponent(encParms.q, verificationCardSecret), // exponent
					true, // validate
				);

			return {
				gamma: exponentiatedVoteCiphertext[0],
				phis: exponentiatedVoteCiphertext.slice(1),
				computationalValues: {
					gamma: exponentiatedVoteCiphertext[0],
					phis: exponentiatedVoteCiphertext.slice(1),
				},
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
				computationalValues: {
					gamma: gamma,
					phis: [compressedPRCphis],
				},
			};
		})();

		const ebPubKey = encParms.optionsEncryptionKey;
		const prcPubKey = encParms.choiceCodesEncryptionKey;

		const primaryPublicKey = (function () {
			const firstSubKey = [];
			firstSubKey.push(ebPubKey.elements[0]);

			return {
				group: encParms.group,
				elements: firstSubKey,
			};
		})();

		const secondaryPublicKey = (function () {
			const compressedSecondaryPublicKey = arrayCompressor.compressZpGroupElements(
				prcPubKey.elements,
			);

			return {
				group: encParms.group,
				elements: [compressedSecondaryPublicKey],
			};
		})();

		// verify plain text eq proof
		expect(
			proofService
				.newPlaintextEqualityProofHandler(encParms.group)
				.init(primaryPublicKey, secondaryPublicKey)
				.verify(plaintextEqualityProof, primaryCiphertext, secondaryCipherText),
		).toBe(true);

		// verify exponentiation proof

		const generator = mathematicalService.newZpGroupElement(
			encParms.p,
			encParms.q,
			encParms.g,
		);
		const publicKey = elgamalService.newPublicKey(
			codec.utf8Decode(codec.base64Decode(verificationCardPublicKey.publicKey)),
		).elements[0];

		expect(
			proofService
				.newExponentiationProofHandler(encParms.group)
				.init([generator].concat(cipherTextExponentiations.cipherTextBases))
				.verify(
					exponentiationProof,
					[publicKey].concat(
						cipherTextExponentiations.cipherTextExponentiation,
					),
				),
		).toBe(true);

		return elapsed;
	};

	it('should create a vote without precomputations', function () {
		encryptAndProve();
	});

	it('should create a vote with precomputations', function () {
		const serializedEncrypterValues = OV.precomputeEncrypterValues(encParms);
		const precomputedEncrypterValues = OV.deserializeEncrypterValues(
			serializedEncrypterValues,
		);
		const precomputedProofValues = OV.precomputeProofs(
			encParms,
			serializedEncrypterValues,
		);
		encryptAndProve(
			precomputedEncrypterValues,
			precomputedProofValues,
		);
	});
});
