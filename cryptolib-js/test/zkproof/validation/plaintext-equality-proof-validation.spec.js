/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const { expect } = require('chai');

const PlaintextEqualityProofTestData =
	require('../data/plaintext-equality-proof-data');
const ValidationTestData = require('../data/validation-data');
const zkProof = require('../../../src/zkproof');
const elGamal = require('../../../src/elgamal');
const cryptoPolicy = require('../../../src/cryptopolicy');

const expectedThrowMessage = 'Expected Q to have a length of 256 for group type ZP_2048_256; Found 224';

describe('The zero-knowledge proof module should be able to ...', function () {
	let _proofService;
	let _group;
	let _zp224Group;
	let _primarySecret;
	let _primaryCiphertext;
	let _secondarySecret;
	let _secondaryCiphertext;
	let _primarySecretFromAnotherGroup;
	let _primaryCiphertextFromAnotherGroup;
	let _secondarySecretFromAnotherGroup;
	let _secondaryCiphertextFromAnotherGroup;
	let _primaryCiphertextWithLessElements;
	let _secondaryCiphertextWithLessElements;
	let _zp224Ciphertext;
	let _primaryPublicKey;
	let _secondaryPublicKey;
	let _primaryPublicKeyWithLessElements;
	let _primaryPublicKeyFromAnotherGroup;
	let _secondaryPublicKeyFromAnotherGroup;
	let _zp224PublicKey;
	let _nonObject;
	let _emptyObject;
	let _nonAuxiliaryData;
	let _proofHandler;
	let _proof;
	let _policy;
	let _zp224Policy;

	beforeEach(function () {
		_policy = cryptoPolicy.newInstance();
		_policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
		_zp224Policy = cryptoPolicy.newInstance();
		_zp224Policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.ZP_2048_224;

		_proofService = zkProof.newService({policy: _policy});

		const testData = new PlaintextEqualityProofTestData();
		const elGamalService = elGamal.newService({policy: _policy});
		const zp224ElGamalService = elGamal.newService({policy: _zp224Policy});
		_group = testData.getGroup();
		_zp224Group = testData.getZP224Group();
		const primaryEncryptedElements = testData.getPrimaryEncryptedElements();
		_primarySecret = primaryEncryptedElements.secret;
		_primaryCiphertext = elGamalService.newEncryptedElements(
			primaryEncryptedElements.gamma, primaryEncryptedElements.phis);
		const secondaryEncryptedElements = testData.getSecondaryEncryptedElements();
		_secondarySecret = secondaryEncryptedElements.secret;
		_secondaryCiphertext = elGamalService.newEncryptedElements(
			secondaryEncryptedElements.gamma, secondaryEncryptedElements.phis);
		const encryptedElementsFromAnotherGroup =
			testData.getEncryptedElementsFromAnotherGroup();
		_primarySecretFromAnotherGroup = encryptedElementsFromAnotherGroup.secret;
		const _zp224EncryptedElements = testData.getZP224EncryptedElements();
		_zp224Ciphertext = zp224ElGamalService.newEncryptedElements(
			_zp224EncryptedElements.gamma, _zp224EncryptedElements.phis);
		_primaryCiphertextFromAnotherGroup = elGamalService.newEncryptedElements(
			encryptedElementsFromAnotherGroup.gamma,
			encryptedElementsFromAnotherGroup.phis);
		_secondarySecretFromAnotherGroup = _primarySecretFromAnotherGroup;
		_secondaryCiphertextFromAnotherGroup = _primaryCiphertextFromAnotherGroup;
		const encryptedElementsWithLessElements =
			testData.getEncryptedElementsWithLessElements();
		_primaryCiphertextWithLessElements = elGamalService.newEncryptedElements(
			encryptedElementsWithLessElements.gamma,
			encryptedElementsWithLessElements.phis);
		_secondaryCiphertextWithLessElements = _primaryCiphertextWithLessElements;
		_primaryPublicKey = testData.getPrimaryPublicKey();
		_secondaryPublicKey = testData.getSecondaryPublicKey();
		_primaryPublicKeyWithLessElements = testData.getPublicKeyWithLessElements();
		_primaryPublicKeyFromAnotherGroup = testData.getPublicKeyFromAnotherGroup();
		_zp224PublicKey = testData.getZP224PublicKey();
		_secondaryPublicKeyFromAnotherGroup =
			testData.getPublicKeyFromAnotherGroup();

		const validationTestData = new ValidationTestData();
		_nonObject = validationTestData.getNonObject();
		_emptyObject = validationTestData.getEmptyObject();
		_nonAuxiliaryData = validationTestData.getNonAuxiliaryData();

		_proofHandler = _proofService.newPlaintextEqualityProofHandler(_group).init(
			_primaryPublicKey, _secondaryPublicKey);
		_proof = _proofHandler.generate(
			_primarySecret, _secondarySecret, _primaryCiphertext,
			_secondaryCiphertext);
	});

	beforeEach(function () {
		_proofHandler = _proofService.newPlaintextEqualityProofHandler(_group).init(
			_primaryPublicKey, _secondaryPublicKey);
	});

	describe('create a zero-knowledge proof service that should be able to ..', function () {
		describe('create a plaintext equality proof handler that should be able to', function () {
			it('throw an error when being created, using invalid input data',
				function () {
					expect(function () {
						_proofService.newPlaintextEqualityProofHandler();
					}).to.throw();

					expect(function () {
						_proofService.newPlaintextEqualityProofHandler(undefined);
					}).to.throw();

					expect(function () {
						_proofService.newPlaintextEqualityProofHandler(null);
					}).to.throw();

					expect(function () {
						_proofService.newPlaintextEqualityProofHandler(_nonObject);
					}).to.throw();

					expect(function () {
						_proofService.newPlaintextEqualityProofHandler(_emptyObject);
					}).to.throw();

					expect(function () {
						_proofService.newPlaintextEqualityProofHandler(_zp224Group);
					}).to.throw(expectedThrowMessage);
				});

			it('throw an error when being initialized, using an invalid primary public key',
				function () {
					expect(function () {
						_proofHandler.init(undefined, _secondaryPublicKey);
					}).to.throw();

					expect(function () {
						_proofHandler.init(null, _secondaryPublicKey);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_nonObject, _secondaryPublicKey);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_emptyObject, _secondaryPublicKey);
					}).to.throw();

					expect(function () {
						_proofHandler.init(
							_primaryPublicKeyWithLessElements, _secondaryPublicKey);
					}).to.throw();

					expect(function () {
						_proofHandler.init(
							_primaryPublicKeyFromAnotherGroup, _secondaryPublicKey);
					}).to.throw();

					expect(function () {
						_proofHandler.init(
							_zp224PublicKey, _secondaryPublicKey);
					}).to.throw(expectedThrowMessage);
				});

			it('throw an error when being initialized, using an invalid secondary public key',
				function () {
					expect(function () {
						_proofHandler.init(_primaryPublicKey);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_primaryPublicKey, undefined);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_primaryPublicKey, null);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_primaryPublicKey, _nonObject);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_primaryPublicKey, _emptyObject);
					}).to.throw();

					expect(function () {
						_proofHandler.init(
							_primaryPublicKey, _secondaryPublicKeyFromAnotherGroup);
					}).to.throw();

					expect(function () {
						_proofHandler.init(
							_primaryPublicKey, _zp224PublicKey);
					}).to.throw(expectedThrowMessage);
				});

			it('throw an error when generating a plaintext equality proof, using an invalid primary secret',
				function () {
					expect(function () {
						_proofHandler.generate(
							undefined, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							null, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_nonObject, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_emptyObject, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecretFromAnotherGroup, _secondarySecret,
							_primaryCiphertext, _secondaryCiphertext);
					}).to.throw();
				});

			it('throw an error when generating a plaintext equality proof, using an invalid secondary secret',
				function () {
					expect(function () {
						_proofHandler.generate(
							_primarySecret, undefined, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, null, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _nonObject, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _emptyObject, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecretFromAnotherGroup,
							_primaryCiphertext, _secondaryCiphertext);
					}).to.throw();
				});

			it('throw an error when generating a plaintext equality proof, using an invalid primary ciphertext',
				function () {
					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, undefined,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, null, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _nonObject,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _emptyObject,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret,
							_primaryCiphertextWithLessElements, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret,
							_primaryCiphertextFromAnotherGroup, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret,
							_zp224Ciphertext, _secondaryCiphertext);
					}).to.throw(expectedThrowMessage);
				});

			it('throw an error when generating a plaintext equality proof, using an invalid secondary ciphertext',
				function () {
					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							undefined);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext, null);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_nonObject);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_emptyObject);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertextWithLessElements);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertextFromAnotherGroup);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_zp224Ciphertext);
					}).to.throw(expectedThrowMessage);
				});

			it('throw an error when generating a simple plaintext equality proof, using invalid optional data',
				function () {
					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertext, {data: _nonAuxiliaryData});
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertext, {preComputation: _nonObject});
					}).to.throw();
				});

			it('throw an error when verifying a plaintext equality proof, using an invalid proof',
				function () {
					expect(function () {
						_proofHandler.verify(
							undefined, _primaryCiphertext, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							null, _primaryCiphertext, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_nonObject, _primaryCiphertext, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_emptyObject, _primaryCiphertext, _secondaryCiphertext);
					}).to.throw();
				});

			it('throw an error when verifying a plaintext equality proof, using an invalid primary ciphertext',
				function () {
					expect(function () {
						_proofHandler.verify(_proof, undefined, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, null, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _nonObject, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _emptyObject, _secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_proof, _primaryCiphertextWithLessElements,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_proof, _primaryCiphertextFromAnotherGroup,
							_secondaryCiphertext);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_proof, _zp224Ciphertext,
							_secondaryCiphertext);
					}).to.throw(expectedThrowMessage);
				});

			it('throw an error when verifying a plaintext equality proof, using an invalid secondary ciphertext',
				function () {
					expect(function () {
						_proofHandler.verify(_proof, _primaryCiphertext, undefined);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _primaryCiphertext, null);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _primaryCiphertext, _nonObject);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _primaryCiphertext, _emptyObject);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_proof, _primaryCiphertext,
							_secondaryCiphertextWithLessElements);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_proof, _primaryCiphertext,
							_secondaryCiphertextFromAnotherGroup);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(
							_proof, _primaryCiphertext,
							_zp224Ciphertext);
					}).to.throw(expectedThrowMessage);
				});

			it('throw an error when verifying a simple plaintext equality proof, using invalid optional data',
				function () {
					expect(function () {
						_proofHandler.verify(
							_proof, _primaryCiphertext, _secondaryCiphertext,
							{data: _nonAuxiliaryData});
					}).to.throw();
				});
		});
	});
});
