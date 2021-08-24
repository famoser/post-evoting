/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const CommonTestData = require('./common-data');
const ExponentiationProofTestData = require('./exponentiation-proof-data');
const zkProof = require('../../../src/zkproof');
const cryptoPolicy = require('../../../src/cryptopolicy');

module.exports = ValidationTestData;

/**
 * Provides the input validation data needed by the zero-knowledge proof service
 * unit tests.
 */
function ValidationTestData() {
	let policy;
	policy = cryptoPolicy.newInstance();
	policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;

	const proofService = zkProof.newService({policy: policy});
	const commonTestData = new CommonTestData();
	const exponentiationProofData = new ExponentiationProofTestData();

	const _nonObject = 999;
	const _emptyObject = {};
	const _nonString = 999;
	const _emptyString = '';
	const _nonJsonString = 'Not a JSON string';
	const _nonArray = '';
	const _emptyArray = [];
	const _nonObjectArray = ['1', '2', '3'];
	const _emptyObjectArray = [{}, {}, {}];
	const _nonAuxiliaryData = 999;

	const group = commonTestData.getGroup();
	const secret = exponentiationProofData.getSecret();
	const baseElements = exponentiationProofData.getBaseElements();
	const exponentiatedElements = exponentiationProofData.getExponentiatedElements();
	const proof =
		proofService.newExponentiationProofHandler(group).init(baseElements).generate(secret, exponentiatedElements, '');
	const _proofHash = proof.hash;
	const _proofValues = proof.values;
	const preComputation = proofService.newExponentiationProofHandler(group).init(baseElements).preCompute();
	const _preComputationExponents = preComputation.exponents;
	const _preComputationPhiOutputs = preComputation.phiOutputs;

	this.getNonObject = function () {
		return _nonObject;
	};

	this.getEmptyObject = function () {
		return _emptyObject;
	};

	this.getNonString = function () {
		return _nonString;
	};

	this.getEmptyString = function () {
		return _emptyString;
	};

	this.getNonJsonString = function () {
		return _nonJsonString;
	};

	this.getNonArray = function () {
		return _nonArray;
	};

	this.getEmptyArray = function () {
		return _emptyArray;
	};

	this.getNonObjectArray = function () {
		return _nonObjectArray;
	};

	this.getEmptyObjectArray = function () {
		return _emptyObjectArray;
	};

	this.getNonAuxiliaryData = function () {
		return _nonAuxiliaryData;
	};

	this.getProofHash = function () {
		return _proofHash;
	};

	this.getProofValues = function () {
		return _proofValues;
	};

	this.getPreComputationExponents = function () {
		return _preComputationExponents;
	};

	this.getPreComputationPhiOutputs = function () {
		return _preComputationPhiOutputs;
	};
}
