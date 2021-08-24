/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true, mocha: true, expr:true */
'use strict';

const { assert, expect } = require('chai');

const ExponentiationProofTestData = require('./data/exponentiation-proof-data');
const cryptoPolicy = require('../../src/cryptopolicy');
const zkProof = require('../../src/zkproof');
const messageDigest = require('../../src/messagedigest');
const mathematical = require('../../src/mathematical');
const secureRandom = require('../../src/securerandom');
const codec = require('../../src/codec');


let _proofService;
let _group;
let _secret;
let _anotherSecret;
let _baseElements;
let _otherBaseElements;
let _exponentiatedElements;
let _otherExponentiatedElements;
let _data;
let _otherData;
let _proofHandler;
let _proof;
let _preComputation;
let _policy;

function beforeEachFunction() {
	_policy = cryptoPolicy.newInstance();
	_policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
	_proofService = zkProof.newService({policy: _policy});

	const testData = new ExponentiationProofTestData();
	_group = testData.getGroup();
	_secret = testData.getSecret();
	_anotherSecret = testData.getAnotherSecret();
	_baseElements = testData.getBaseElements();
	_otherBaseElements = testData.getOtherBaseElements();
	_exponentiatedElements = testData.getExponentiatedElements();
	_otherExponentiatedElements = testData.getOtherExponentiatedElements();
	_data = testData.getStringData();
	_otherData = testData.getOtherStringData();

	_proofHandler = _proofService.newExponentiationProofHandler(_group).init(_baseElements);
	_proof = _proofHandler.generate(_secret, _exponentiatedElements);
	_preComputation = _proofHandler.preCompute();
}

const describeText1 = 'The zero-knowledge proof module should be able to ...';
const describeText2 = 'create a zero-knowledge proof service that should be able to ...';
const describeText3 = 'create an exponentiation proof handler that should be able to ...';

describe(describeText1, function () {
	beforeEach(function () {
		beforeEachFunction();
	});

	describe(describeText2, function () {
		describe(describeText3, function () {
			it('generate and verify an exponentiation proof', function () {
				const verified = _proofHandler.verify(_proof, _exponentiatedElements);
				assert.isTrue(verified);
			});

			it('generate and verify an exponentiation proof, using a specified cryptographic policy',
				function () {
					const policy = cryptoPolicy.newInstance();
					policy.proofs.messageDigest.algorithm = cryptoPolicy.options.proofs.messageDigest.algorithm.SHA512_224;
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;

					const proofService = zkProof.newService({policy: policy});
					const proofHandler = proofService.newExponentiationProofHandler(_group).init(_baseElements);

					const proof = proofHandler.generate(_secret, _exponentiatedElements);

					const verified = proofHandler.verify(proof, _exponentiatedElements);
					assert.isTrue(verified);
				});

			it('generate and verify an exponentiation proof, using a specified message digest service object',
				function () {
					const policy = cryptoPolicy.newInstance();
					policy.proofs.messageDigest.algorithm = cryptoPolicy.options.proofs.messageDigest.algorithm.SHA512_224;
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;

					const messageDigestService = messageDigest.newService({policy: policy});

					const proofService = zkProof.newService({policy: policy, messageDigestService: messageDigestService});
					const proofHandler = proofService.newExponentiationProofHandler(_group).init(_baseElements);

					const proof = proofHandler.generate(_secret, _exponentiatedElements);

					const verified = proofHandler.verify(proof, _exponentiatedElements);
					assert.isTrue(verified);
				});

			it('generate and verify an exponentiation proof, using a specified secure random service object',
				function () {
					const proofService = zkProof.newService({policy: _policy, secureRandomService: secureRandom});
					const proofHandler = proofService.newExponentiationProofHandler(_group).init(_baseElements);

					const proof = proofHandler.generate(_secret, _exponentiatedElements);

					const verified = proofHandler.verify(proof, _exponentiatedElements);
					assert.isTrue(verified);
				});

			it('generate and verify an exponentiation proof, using a specified mathematical service object',
				function () {
					const proofService = zkProof.newService({policy: _policy, mathematicalService: mathematical.newService({policy: _policy})});
					const proofHandler = proofService.newExponentiationProofHandler(_group).init(_baseElements);

					const proof = proofHandler.generate(_secret, _exponentiatedElements);

					const verified = proofHandler.verify(proof, _exponentiatedElements);
					assert.isTrue(verified);
				});

			it('generate and verify a exponentiation proof, using provided auxiliary data',
				function () {
					// Data as string.
					let proof = _proofHandler.generate(_secret, _exponentiatedElements, {data: _data});
					let verified = _proofHandler.verify(proof, _exponentiatedElements, {data: _data});
					assert.isTrue(verified);

					// Data as bytes.
					proof = _proofHandler.generate(_secret, _exponentiatedElements, {data: codec.utf8Encode(_data)});
					verified = _proofHandler.verify(proof, _exponentiatedElements, {data: codec.utf8Encode(_data)});
					assert.isTrue(verified);
				});

			it('generate and verify an exponentiation proof, using a pre-computation',
				function () {
					const proof = _proofHandler.generate(_secret, _exponentiatedElements, {preComputation: _preComputation});

					const verified = _proofHandler.verify(proof, _exponentiatedElements);
					assert.isTrue(verified);
				});

			it('generate and verify an exponentiation proof, using empty string auxiliary data',
				function () {
					const proof = _proofHandler.generate(_secret, _exponentiatedElements, {data: ''});

					const verified = _proofHandler.verify(proof, _exponentiatedElements, {data: ''});
					assert.isTrue(verified);
				});

			it('pre-compute, generate and verify a exponentiation proof, using method chaining',
				function () {
					const preComputation = _proofService.newExponentiationProofHandler(_group)
						.init(_baseElements)
						.preCompute();

					const proof = _proofService.newExponentiationProofHandler(_group)
						.init(_baseElements)
						.generate(_secret, _exponentiatedElements, {data: _data, preComputation: preComputation});

					const verified = _proofService.newExponentiationProofHandler(_group)
						.init(_baseElements)
						.verify(proof, _exponentiatedElements, {data: _data});
					assert.isTrue(verified);
				});

			it('create a new exponentiation ZeroKnowledgeProof object', function () {
				const hash = _proof.hash;
				const values = _proof.values;
				const proof = _proofService.newProof(hash, values);

				const verified = _proofHandler.verify(proof, _exponentiatedElements);
				assert.isTrue(verified);
			});

			it('create a new exponentiation ZeroKnowledgeProofPreComputation object',
				function () {
					const exponents = _preComputation.exponents;
					const phiOutputs = _preComputation.phiOutputs;
					const preComputation = _proofService.newPreComputation(exponents, phiOutputs);

					const proof = _proofHandler.generate(_secret, _exponentiatedElements, {preComputation: preComputation});

					const verified = _proofHandler.verify(proof, _exponentiatedElements);
					assert.isTrue(verified);
				});

			it('serialize and deserialize an exponentiation proof', function () {
				const proofJson = _proof.toJson();

				const proof = _proofService.newProof(proofJson);

				const verified = _proofHandler.verify(proof, _exponentiatedElements);
				assert.isTrue(verified);
			});

			it('serialize and deserialize an exponentiation proof pre-computation',
				function () {
					const preComputationJson = _preComputation.toJson();

					const preComputation = _proofService.newPreComputation(preComputationJson);

					const proof = _proofHandler.generate(_secret, _exponentiatedElements, {preComputation: preComputation});

					const verified = _proofHandler.verify(proof, _exponentiatedElements);
					assert.isTrue(verified);
				});
		});
	});
});

describe(describeText1, function () {
	beforeEach(function () {
		beforeEachFunction();
	});

	describe(describeText2, function () {
		describe(describeText3, function () {
			it('fail to verify an exponentiation proof that was generated with a secret not used to exponentiate',
				function () {
					const proof = _proofHandler.generate(_anotherSecret, _exponentiatedElements);

					const verified = _proofHandler.verify(proof, _exponentiatedElements);
					assert.isFalse(verified);
				});

			it('fail to verify an exponentiation proof that was generated with base elements not used to exponentiate',
				function () {
					_proofHandler.init(_otherBaseElements);

					const proof = _proofHandler.generate(_secret, _exponentiatedElements);

					const verified = _proofHandler.verify(proof, _exponentiatedElements);
					assert.isFalse(verified);
				});

			it('fail to verify an exponentiation proof that was generated with base elements exponentiated to another secret',
				function () {
					const proof = _proofHandler.generate(_secret, _otherExponentiatedElements);

					const verified = _proofHandler.verify(proof, _otherExponentiatedElements);
					assert.isFalse(verified);
				});

			it('fail to verify an exponentiation proof when using base elements not used to exponentiate',
				function () {
					const verified = _proofHandler.init(_otherBaseElements).verify(_proof, _exponentiatedElements);
					assert.isFalse(verified);
				});

			it('fail to verify an exponentiation proof when using elements exponentiated to the wrong secret',
				function () {
					const verified = _proofHandler.verify(_proof, _otherExponentiatedElements);
					assert.isFalse(verified);
				});

			it('fail to verify an exponentiation proof when using the wrong data',
				function () {
					const proof = _proofHandler.generate(_secret, _exponentiatedElements, {data: _data});

					const verified = _proofHandler.verify(proof, _exponentiatedElements, {data: _otherData});
					assert.isFalse(verified);
				});

			it('throw an error when generating a proof before the handler has been initialized with base elements',
				function () {
					const proofHandler = _proofService.newExponentiationProofHandler(_group);

					expect(function () {
						proofHandler.generate(_secret, _exponentiatedElements);
					}).to.throw();
				});

			it('throw an error when pre-computing a proof before the handler has been initialized with base elements',
				function () {
					const proofHandler = _proofService.newExponentiationProofHandler(_group);

					expect(function () {
						proofHandler.preCompute();
					}).to.throw();
				});

			it('throw an error when verifying a proof before the handler has been initialized with base elements',
				function () {
					const proofHandler = _proofService.newExponentiationProofHandler(_group);

					expect(function () {
						proofHandler.verify(_proof, _exponentiatedElements);
					}).to.throw();
				});
		});
	});
});
