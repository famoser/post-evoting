/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const { expect } = require('chai');

const ValidationTestData = require('../data/validation-data');
const zkProof = require('../../../src/zkproof');
const cryptoPolicy = require('../../../src/cryptopolicy');

let _proofService;
let _nonObject;
let _emptyObject;
let _nonJsonString;
let _nonArray;
let _emptyArray;
let _nonObjectArray;
let _emptyObjectArray;
let _hash;
let _values;
let _exponents;
let _phiOutputs;
let _policy;

function beforeEachHook() {
	_policy = cryptoPolicy.newInstance();
	_policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;

	_proofService = zkProof.newService({policy: _policy});

	const testData = new ValidationTestData();
	_nonObject = testData.getNonObject();
	_emptyObject = testData.getEmptyObject();
	_nonJsonString = testData.getNonJsonString();
	_nonArray = testData.getNonArray();
	_emptyArray = testData.getEmptyArray();
	_nonObjectArray = testData.getNonObjectArray();
	_emptyObjectArray = testData.getEmptyObjectArray();
	_hash = testData.getProofHash();
	_values = testData.getProofValues();
	_exponents = testData.getPreComputationExponents();
	_phiOutputs = testData.getPreComputationPhiOutputs();
}

describe('The zero-knowledge proof module should be able to ...', function () {

	beforeEach(function () {
		beforeEachHook();
	});

	describe('create a zero-knowledge proof service that should be able to ...', function () {
		it('throw an error when being created, using an invalid cryptographic policy',
			function () {
				expect(function () {
					zkProof.newService({policy: null});
				}).to.throw();

				expect(function () {
					zkProof.newService({policy: _nonObject});
				}).to.throw();

				expect(function () {
					zkProof.newService({policy: _emptyObject});
				}).to.throw();
			});

		it('throw an error when being created, using an invalid secure random service object',
			function () {
				expect(function () {
					zkProof.newService({secureRandomService: null});
				}).to.throw();

				expect(function () {
					zkProof.newService({secureRandomService: _nonObject});
				}).to.throw();

				expect(function () {
					zkProof.newService({secureRandomService: _emptyObject});
				}).to.throw();
			});

		it('throw an error when being created, using an invalid mathematical service object',
			function () {
				expect(function () {
					zkProof.newService({mathematicalService: null});
				}).to.throw();

				expect(function () {
					zkProof.newService({mathematicalService: _nonObject});
				}).to.throw();

				expect(function () {
					zkProof.newService({mathematicalService: _emptyObject});
				}).to.throw();
			});

		it('throw an error when being created, using an invalid message digest service object',
			function () {
				expect(function () {
					zkProof.newService({messageDigestService: null});
				}).to.throw();

				expect(function () {
					zkProof.newService({messageDigestService: _nonObject});
				}).to.throw();

				expect(function () {
					zkProof.newService({messageDigestService: _emptyObject});
				}).to.throw();
			});

		it('throw an error when creating a new ZeroKnowledgeProof object, using invalid input data',
			function () {
				expect(function () {
					_proofService.newProof(undefined, _values);
				}).to.throw();

				expect(function () {
					_proofService.newProof(null, _values);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_nonObject, _values);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_emptyObject, _values);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_hash);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_hash, undefined);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_hash, null);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_hash, _nonArray);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_hash, _emptyArray);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_hash, _nonObjectArray);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_hash, _emptyObjectArray);
				}).to.throw();

				expect(function () {
					_proofService.newProof(_nonJsonString);
				}).to.throw();
			});

		it('throw an error when creating a new ZeroKnowledgeProofPreComputation object, using invalid input data',
			function () {
				expect(function () {
					_proofService.newPreComputation(undefined, _phiOutputs);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(null, _phiOutputs);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_nonArray, _phiOutputs);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_emptyArray, _phiOutputs);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_nonObjectArray, _phiOutputs);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_exponents);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_exponents, undefined);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_exponents, null);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_exponents, _nonArray);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_exponents, _emptyArray);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_exponents, _nonObjectArray);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_exponents, _emptyObjectArray);
				}).to.throw();

				expect(function () {
					_proofService.newPreComputation(_nonJsonString);
				}).to.throw();

				expect(function () {
					_policy = cryptoPolicy.newInstance();
					_policy.mathematical.groups.type =
						cryptoPolicy.options.mathematical.groups.type.QR_2048;

					const proofServiceQR2048 = zkProof.newService({policy: _policy});

					proofServiceQR2048.newPreComputation(_exponents, _phiOutputs);
				}).to.throw('Expected Q to have a length of 2047 for group type QR_2048; Found 256');
			});
	});
});
