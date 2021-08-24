/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const { expect } = require('chai');

const ExponentiationProofTestData = require('../data/exponentiation-proof-data');
const ValidationTestData = require('../data/validation-data');
const zkProof = require('../../../src/zkproof');
const cryptoPolicy = require('../../../src/cryptopolicy');


let _proofService;

let _group;
let _zp224group;
let _secret;
let _secretFromAnotherGroup;
let _baseElements;
let _baseElementsFromAnotherGroup;
let _zp224BaseElements;
let _exponentiatedElements;
let _tooManyExponentiatedElements;
let _exponentiatedElementsFromAnotherGroup;
let _zp224ExponentiatedElements;
let _nonObject;
let _emptyObject;
let _nonArray;
let _emptyArray;
let _nonObjectArray;
let _emptyObjectArray;
let _nonAuxiliaryData;
let _proofHandler;
let _proof;
let _policy;

const expectedText = 'Expected Q to have a length of 256 for group type ZP_2048_256; Found 224';

function beforeEachFunction() {
	_policy = cryptoPolicy.newInstance();
	_policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;

	_proofService = zkProof.newService({policy: _policy});

	const testData = new ExponentiationProofTestData();
	_group = testData.getGroup();
	_zp224group = testData.getZP224Group();
	_secret = testData.getSecret();
	_secretFromAnotherGroup = testData.getSecretFromAnotherGroup();
	_baseElements = testData.getBaseElements();
	_baseElementsFromAnotherGroup = testData.getBaseElementsFromAnotherGroup();
	_zp224BaseElements = testData.getZP224BaseElements();
	_exponentiatedElements = testData.getExponentiatedElements();
	_exponentiatedElementsFromAnotherGroup = testData.getExponentiatedElementsFromAnotherGroup();
	_tooManyExponentiatedElements = testData.getTooManyExponentiatedElements();
	_zp224ExponentiatedElements = testData.getZP224ExponentiatedElements();

	const validationTestData = new ValidationTestData();
	_nonObject = validationTestData.getNonObject();
	_emptyObject = validationTestData.getEmptyObject();
	_nonArray = validationTestData.getNonArray();
	_emptyArray = validationTestData.getEmptyArray();
	_nonObjectArray = validationTestData.getNonObjectArray();
	_emptyObjectArray = validationTestData.getEmptyObjectArray();
	_nonAuxiliaryData = validationTestData.getNonAuxiliaryData();

	_proofHandler = _proofService.newExponentiationProofHandler(_group).init(_baseElements);
	_proof = _proofHandler.generate(_secret, _exponentiatedElements);
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
			it('throw an error when being created, using invalid input data',
				function () {
					expect(function () {
						_proofService.newExponentiationProofHandler();
					}).to.throw();

					expect(function () {
						_proofService.newExponentiationProofHandler(undefined);
					}).to.throw();

					expect(function () {
						_proofService.newExponentiationProofHandler(null);
					}).to.throw();

					expect(function () {
						_proofService.newExponentiationProofHandler(_nonObject);
					}).to.throw();

					expect(function () {
						_proofService.newExponentiationProofHandler(_emptyObject);
					}).to.throw();

					expect(function () {
						_proofService.newExponentiationProofHandler(_zp224group);
					}).to.throw(expectedText);
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
			it('throw an error when being initialized, using invalid input data',
				function () {
					expect(function () {
						_proofHandler.init(undefined);
					}).to.throw();

					expect(function () {
						_proofHandler.init(null);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_nonArray);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_emptyArray);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_nonObjectArray);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_emptyObjectArray);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_baseElementsFromAnotherGroup);
					}).to.throw();

					expect(function () {
						_proofHandler.init(_zp224BaseElements);
					}).to.throw(expectedText);
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
			it('throw an error when generating an exponentiation proof, using an invalid secret',
				function () {
					expect(function () {
						_proofHandler.generate(undefined, _exponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(null, _exponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_nonObject, _exponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_emptyObject, _exponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secretFromAnotherGroup, _exponentiatedElements);
					}).to.throw();
				});

			it('throw an error when generating an exponentiation proof, using invalid exponentiated elements',
				function () {
					expect(function () {
						_proofHandler.generate(_secret, undefined);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, null);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, _nonArray);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, _emptyArray);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, _nonObjectArray);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, _emptyObjectArray);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, _tooManyExponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(
							_secret, _exponentiatedElementsFromAnotherGroup);
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, _zp224ExponentiatedElements);
					}).to.throw(expectedText);
				});

			it('throw an error when generating a plaintext proof, using invalid optional data',
				function () {
					expect(function () {
						_proofHandler.generate(_secret, _exponentiatedElements, {data: _nonAuxiliaryData});
					}).to.throw();

					expect(function () {
						_proofHandler.generate(_secret, _exponentiatedElements, {preComputation: _nonObject});
					}).to.throw();
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
			it('throw an error when verifying an exponentiation proof, using an invalid proof',
				function () {
					expect(function () {
						_proofHandler.verify(undefined, _exponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(null, _exponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_nonObject, _exponentiatedElements);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_emptyObject, _exponentiatedElements);
					}).to.throw();
				});

			it('throw an error when verifying an exponentiation proof, using invalid exponentiated elements',
				function () {
					expect(function () {
						_proofHandler.verify(_proof, undefined);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, null);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _nonArray);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _emptyArray);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _nonObjectArray);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _emptyObjectArray);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _exponentiatedElementsFromAnotherGroup);
					}).to.throw();

					expect(function () {
						_proofHandler.verify(_proof, _zp224ExponentiatedElements);
					}).to.throw(expectedText);
				});

			it('throw an error when verifying an exponentiation proof, using invalid optional data',
				function () {
					expect(function () {
						_proofHandler.verify(_proof, _exponentiatedElements, {data: _nonAuxiliaryData});
					}).to.throw();
				});
		});
	});
});
