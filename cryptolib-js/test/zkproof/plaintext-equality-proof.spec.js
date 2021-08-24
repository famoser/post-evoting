/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true, mocha: true, expr:true */
'use strict';

const { assert, expect } = require('chai');

const PlaintextEqualityProofTestData =
	require('./data/plaintext-equality-proof-data');
const cryptoPolicy = require('../../src/cryptopolicy');
const zkProof = require('../../src/zkproof');
const messageDigest = require('../../src/messagedigest');
const mathematical = require('../../src/mathematical');
const secureRandom = require('../../src/securerandom');
const elGamal = require('../../src/elgamal');
const codec = require('../../src/codec');

describe('The zero-knowledge proof module should be able to ...', function () {
	let _proofService;
	let _group;
	let _primarySecret;
	let _primaryCiphertext;
	let _secondarySecret;
	let _secondaryCiphertext;
	let _anotherPrimarySecret;
	let _anotherPrimaryCiphertext;
	let _anotherSecondarySecret;
	let _anotherSecondaryCiphertext;
	let _primaryPublicKey;
	let _secondaryPublicKey;
	let _anotherPrimaryPublicKey;
	let _anotherSecondaryPublicKey;
	let _data;
	let _otherData;
	let _proofHandler;
	let _proof;
	let _preComputation;
	let _policy;

	beforeEach(function () {
		_policy = cryptoPolicy.newInstance();
		_policy.mathematical.groups.type =
			cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
		_proofService = zkProof.newService({policy: _policy});

		const testData = new PlaintextEqualityProofTestData();
		const elGamalService = elGamal.newService({policy: _policy});
		_group = testData.getGroup();
		const primaryEncryptedElements = testData.getPrimaryEncryptedElements();
		_primarySecret = primaryEncryptedElements.secret;
		_primaryCiphertext = elGamalService.newEncryptedElements(
			primaryEncryptedElements.gamma, primaryEncryptedElements.phis);
		const secondaryEncryptedElements = testData.getSecondaryEncryptedElements();
		_secondarySecret = secondaryEncryptedElements.secret;
		_secondaryCiphertext = elGamalService.newEncryptedElements(
			secondaryEncryptedElements.gamma, secondaryEncryptedElements.phis);
		const otherPrimaryEncryptedElements =
			testData.getOtherPrimaryEncryptedElements();
		_anotherPrimarySecret = otherPrimaryEncryptedElements.secret;
		_anotherPrimaryCiphertext = elGamalService.newEncryptedElements(
			otherPrimaryEncryptedElements.gamma,
			otherPrimaryEncryptedElements.phis);
		const otherSecondaryEncryptedElements =
			testData.getOtherSecondaryEncryptedElements();
		_anotherSecondarySecret = otherSecondaryEncryptedElements.secret;
		_anotherSecondaryCiphertext = elGamalService.newEncryptedElements(
			otherSecondaryEncryptedElements.gamma,
			otherSecondaryEncryptedElements.phis);
		_primaryPublicKey = testData.getPrimaryPublicKey();
		_secondaryPublicKey = testData.getSecondaryPublicKey();
		_anotherPrimaryPublicKey = testData.getAnotherPrimaryPublicKey();
		_anotherSecondaryPublicKey = testData.getAnotherSecondaryPublicKey();
		_data = testData.getStringData();
		_otherData = testData.getOtherStringData();

		_proofHandler = _proofService.newPlaintextEqualityProofHandler(_group).init(
			_primaryPublicKey, _secondaryPublicKey);
		_proof = _proofHandler.generate(
			_primarySecret, _secondarySecret, _primaryCiphertext,
			_secondaryCiphertext);
		_preComputation = _proofHandler.preCompute();
	});

	beforeEach(function () {
		_proofHandler = _proofService.newPlaintextEqualityProofHandler(_group).init(
			_primaryPublicKey, _secondaryPublicKey);
	});

	describe('create a zero-knowledge proof service that should be able to ..', function () {
		describe('create a plaintext equality proof handler that should be able to', function () {
			it('generate and verify a plaintext equality proof', function () {
				const verified = _proofHandler.verify(
					_proof, _primaryCiphertext, _secondaryCiphertext);
				assert.isTrue(verified);
			});

			it('generate and verify a plaintext equality proof, using a specified cryptographic policy',
				function () {
					const policy = cryptoPolicy.newInstance();
					policy.proofs.messageDigest.algorithm =
						cryptoPolicy.options.proofs.messageDigest.algorithm.SHA512_224;
					policy.mathematical.groups.type =
						cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
					const proofService = zkProof.newService({policy: policy});
					const proofHandler =
						proofService.newPlaintextEqualityProofHandler(_group).init(
							_primaryPublicKey, _secondaryPublicKey);

					_proof = proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = proofHandler.verify(
						_proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('generate and verify a plaintext equality proof, using a specified message digest service',
				function () {
					const policy = cryptoPolicy.newInstance();
					policy.proofs.messageDigest.algorithm =
						cryptoPolicy.options.proofs.messageDigest.algorithm.SHA512_224;
					policy.mathematical.groups.type =
						cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
					const messageDigestService =
						messageDigest.newService({policy: policy});

					const proofService =
						zkProof.newService({policy: policy, messageDigestService: messageDigestService});
					const proofHandler =
						proofService.newPlaintextEqualityProofHandler(_group).init(
							_primaryPublicKey, _secondaryPublicKey);

					_proof = proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = proofHandler.verify(
						_proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('generate and verify a plaintext equality proof, using a specified secure random service object',
				function () {
					const proofService = zkProof.newService(
						{policy: _policy, secureRandomService: secureRandom});
					const proofHandler =
						proofService.newPlaintextEqualityProofHandler(_group).init(
							_primaryPublicKey, _secondaryPublicKey);

					_proof = proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = proofHandler.verify(
						_proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('generate and verify a plaintext equality proof, using a specified mathematical service object',
				function () {
					const proofService = zkProof.newService(
						{policy: _policy, mathematicalService: mathematical.newService({policy: _policy})});
					const proofHandler =
						proofService.newPlaintextEqualityProofHandler(_group).init(
							_primaryPublicKey, _secondaryPublicKey);

					_proof = proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = proofHandler.verify(
						_proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('generate and verify a plaintext proof, using provided auxiliary data',
				function () {
					// Data as string.
					let proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext, {data: _data});
					let verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext, {data: _data});
					assert.isTrue(verified);

					// Data as bytes.
					proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext, {data: codec.utf8Encode(_data)});
					verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext,
						{data: codec.utf8Encode(_data)});
					assert.isTrue(verified);
				});

			it('generate and verify a plaintext equality proof, using a pre-computation',
				function () {
					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext, {preComputation: _preComputation});

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('generate and verify a plaintext equality proof, using empty auxiliary data',
				function () {
					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext, {data: ''});

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext, {data: ''});
					assert.isTrue(verified);
				});

			it('pre-compute, generate and verify a plaintext proof, using method chaining',
				function () {
					const preComputation =
						_proofService.newPlaintextEqualityProofHandler(_group)
							.init(_primaryPublicKey, _secondaryPublicKey)
							.preCompute();

					const proof = _proofService.newPlaintextEqualityProofHandler(_group)
						.init(_primaryPublicKey, _secondaryPublicKey)
						.generate(
							_primarySecret, _secondarySecret,
							_primaryCiphertext, _secondaryCiphertext,
							{data: _data, preComputation: preComputation});

					const verified = _proofService.newPlaintextEqualityProofHandler(_group)
						.init(_primaryPublicKey, _secondaryPublicKey)
						.verify(
							proof, _primaryCiphertext,
							_secondaryCiphertext, {data: _data});
					assert.isTrue(verified);
				});

			it('create a new plaintext equality ZeroKnowledgeProof object',
				function () {
					const hash = _proof.hash;
					const values = _proof.values;
					const proof = _proofService.newProof(hash, values);

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('create a new plaintext equality ZeroKnowledgeProofPreComputation object',
				function () {
					const exponents = _preComputation.exponents;
					const phiOutputs = _preComputation.phiOutputs;
					const preComputation =
						_proofService.newPreComputation(exponents, phiOutputs);

					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext, {preComputation: preComputation});

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('serialize and deserialize a plaintext equality proof', function () {
				const proofJson = _proof.toJson();

				const proof = _proofService.newProof(proofJson);

				const verified = _proofHandler.verify(
					proof, _primaryCiphertext, _secondaryCiphertext);
				assert.isTrue(verified);
			});

			it('serialize and deserialize a plaintext equality proof pre-computation',
				function () {
					const preComputationJson = _preComputation.toJson();

					const preComputation =
						_proofService.newPreComputation(preComputationJson);

					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext, {preComputation: preComputation});

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isTrue(verified);
				});

			it('fail to verify a plaintext equality proof that was generated with a primary secret not used to generate the primary ciphertext',
				function () {
					const proof = _proofHandler.generate(
						_anotherPrimarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof that was generated with a secondary secret not used to generate the secondary ciphertext',
				function () {
					const proof = _proofHandler.generate(
						_primarySecret, _anotherSecondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof that was generated with a primary publicKey not used to generate the primary ciphertext',
				function () {
					_proofHandler.init(_anotherPrimaryPublicKey, _secondaryPublicKey);

					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof that was generated with a secondary publicKey not used to generate the secondary ciphertext',
				function () {
					_proofHandler.init(_primaryPublicKey, _anotherSecondaryPublicKey);

					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext);

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof that was generated with a primary ciphertext not generated from the primary secret',
				function () {
					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _anotherPrimaryCiphertext,
						_secondaryCiphertext);

					const verified = _proofHandler.verify(
						proof, _anotherPrimaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof that was generated with a secondary ciphertext not generated from the secondary secret',
				function () {
					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_anotherSecondaryCiphertext);

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _anotherSecondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof when using a primary public key not used to generate the proof',
				function () {
					const verified =
						_proofHandler.init(_anotherPrimaryPublicKey, _secondaryPublicKey)
							.verify(_proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof when using a secondary public key not used to generate the proof',
				function () {
					const verified =
						_proofHandler.init(_primaryPublicKey, _anotherSecondaryPublicKey)
							.verify(_proof, _primaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof when using a primary ciphertext not used to generate the proof',
				function () {
					const verified = _proofHandler.verify(
						_proof, _anotherPrimaryCiphertext, _secondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof when using a secondary ciphertext not used to generate the proof',
				function () {
					const verified = _proofHandler.verify(
						_proof, _primaryCiphertext, _anotherSecondaryCiphertext);
					assert.isFalse(verified);
				});

			it('fail to verify a plaintext equality proof when using auxiliary data not used to generate the proof',
				function () {
					const proof = _proofHandler.generate(
						_primarySecret, _secondarySecret, _primaryCiphertext,
						_secondaryCiphertext, {data: _data});

					const verified = _proofHandler.verify(
						proof, _primaryCiphertext, _secondaryCiphertext,
						{data: _otherData});
					assert.isFalse(verified);
				});

			it('throw an error when generating a proof before the handler has been initialized with any pubilc key',
				function () {
					const proofHandler =
						_proofService.newPlaintextEqualityProofHandler(_group);

					expect(function () {
						proofHandler.generate(
							_primarySecret, _secondarySecret, _primaryCiphertext,
							_secondaryCiphertext);
					}).to.throw();
				});

			it('throw an error when pre-computing a proof before the handler has been initialized with any pubilc key',
				function () {
					const proofHandler =
						_proofService.newPlaintextEqualityProofHandler(_group);

					expect(function () {
						proofHandler.preCompute();
					}).to.throw();
				});

			it('throw an error when verifying a proof before the handler has been initialized with any pubilc key',
				function () {
					const proofHandler =
						_proofService.newPlaintextEqualityProofHandler(_group);

					expect(function () {
						proofHandler.verify(
							_proof, _primaryCiphertext, _secondaryCiphertext);
					}).to.throw();
				});
		});
	});
});
