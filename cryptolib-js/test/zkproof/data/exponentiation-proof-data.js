/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const CommonTestData = require('./common-data');
const mathematical = require('../../../src/mathematical');

module.exports = ExponentiationProofTestData;

/**
 * Provides the data needed by the exponentiation zero-knowledge proof of
 * knowledge unit tests.
 */
function ExponentiationProofTestData() {
	const NUM_BASE_ELEMENTS = 3;
	const EXPONENTIATION_PROOF_STRING_DATA =
		'Test Exponentation Proof Auxilary Data';
	const OTHER_EXPONENTIATION_PROOF_STRING_DATA =
		EXPONENTIATION_PROOF_STRING_DATA + '0';

	const mathRandomGenerator = mathematical.newService().newRandomGenerator();

	const commonTestData = new CommonTestData();
	const _group = commonTestData.getGroup();
	const _anotherGroup = commonTestData.getAnotherGroup();
	const _zp224group = commonTestData.getZP224Group();

	const _secret = mathRandomGenerator.nextExponent(_group);
	let _anotherSecret;
	do {
		_anotherSecret = mathRandomGenerator.nextExponent(_group);
	} while (_anotherSecret.value.equals(_secret.value));
	const _secretFromAnotherGroup = mathRandomGenerator.nextExponent(_anotherGroup);
	const _zp224secret = mathRandomGenerator.nextExponent(_zp224group);

	const _baseElements =
		commonTestData.generateRandomGroupElements(_group, NUM_BASE_ELEMENTS);
	let _otherBaseElements;
	do {
		_otherBaseElements =
			commonTestData.generateRandomGroupElements(_group, NUM_BASE_ELEMENTS);
	} while (_otherBaseElements.equals(_baseElements));
	const _baseElementsFromAnotherGroup =
		commonTestData.generateRandomGroupElements(
			_anotherGroup, NUM_BASE_ELEMENTS);
	const _zp224baseElements =
		commonTestData.generateRandomGroupElements(_zp224group, NUM_BASE_ELEMENTS);

	const _exponentiatedElements =
		commonTestData.exponentiateElements(_baseElements, _secret);
	const _otherExponentiatedElements =
		commonTestData.exponentiateElements(_baseElements, _anotherSecret);
	const tooManyBaseElements =
		commonTestData.generateRandomGroupElements(_group, NUM_BASE_ELEMENTS + 1);
	const _tooManyExponentiatedElements =
		commonTestData.exponentiateElements(tooManyBaseElements, _secret);
	const _exponentiatedElementsFromAnotherGroup =
		commonTestData.exponentiateElements(
			_baseElementsFromAnotherGroup, _secretFromAnotherGroup);
	const _zp224exponentiatedElements =
		commonTestData.exponentiateElements(_zp224baseElements, _zp224secret);

	this.getGroup = function () {
		return _group;
	};

	this.getZP224Group = function () {
		return _zp224group;
	};

	this.getSecret = function () {
		return _secret;
	};

	this.getAnotherSecret = function () {
		return _anotherSecret;
	};

	this.getSecretFromAnotherGroup = function () {
		return _secretFromAnotherGroup;
	};

	this.getBaseElements = function () {
		return _baseElements;
	};

	this.getOtherBaseElements = function () {
		return _otherBaseElements;
	};

	this.getBaseElementsFromAnotherGroup = function () {
		return _baseElementsFromAnotherGroup;
	};

	this.getZP224BaseElements = function () {
		return _zp224baseElements;
	};

	this.getExponentiatedElements = function () {
		return _exponentiatedElements;
	};

	this.getOtherExponentiatedElements = function () {
		return _otherExponentiatedElements;
	};

	this.getTooManyExponentiatedElements = function () {
		return _tooManyExponentiatedElements;
	};

	this.getExponentiatedElementsFromAnotherGroup = function () {
		return _exponentiatedElementsFromAnotherGroup;
	};

	this.getZP224ExponentiatedElements = function () {
		return _zp224exponentiatedElements;
	};

	this.getStringData = function () {
		return EXPONENTIATION_PROOF_STRING_DATA;
	};

	this.getOtherStringData = function () {
		return OTHER_EXPONENTIATION_PROOF_STRING_DATA;
	};
}
