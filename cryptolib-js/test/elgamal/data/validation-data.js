/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const CommonTestData = require('./common-data');

module.exports = ValidationTestData;

/**
 * Provides the input validation data needed by the ElGamal service unit tests.
 */
function ValidationTestData() {
	const commonTestData = new CommonTestData();

	const _nonObject = 999;
	const _emptyObject = {};

	const _nonBoolean = '';
	const _nonJsonString = 'Not a JSON string';
	const _nonArray = '';
	const _nonStringArray = [];
	_nonStringArray.push(1);
	_nonStringArray.push(2);
	_nonStringArray.push(3);
	const _nonObjectArray = [];
	_nonObjectArray.push(1);
	_nonObjectArray.push(2);
	_nonObjectArray.push(3);
	const _emptyObjectArray = [];
	_emptyObjectArray.push({});
	_emptyObjectArray.push({});
	_emptyObjectArray.push({});

	const _publicKeyFromAnotherGroup = commonTestData.getLargePublicKey();
	const _publicKeyElementsFromAnotherGroup = commonTestData.getLargePublicKeyElements();
	const _privateKeyFromAnotherGroup = commonTestData.getLargePrivateKey();
	const _privateKeyExponentsFromAnotherGroup = commonTestData.getExponentsFromLargeZpSubgroup();
	const _elementsFromAnotherGroup =
		commonTestData.getElementsFromLargeZpSubgroup();

	this.getNonObject = function () {
		return _nonObject;
	};

	this.getEmptyObject = function () {
		return _emptyObject;
	};

	this.getNonBoolean = function () {
		return _nonBoolean;
	};

	this.getNonJsonString = function () {
		return _nonJsonString;
	};

	this.getNonArray = function () {
		return _nonArray;
	};

	this.getNonStringArray = function () {
		return _nonStringArray;
	};

	this.getNonObjectArray = function () {
		return _nonObjectArray;
	};

	this.getEmptyObjectArray = function () {
		return _emptyObjectArray;
	};

	this.getPublicKeyElementsFromAnotherGroup = function () {
		return _publicKeyElementsFromAnotherGroup;
	};

	this.getPublicKeyFromAnotherGroup = function () {
		return _publicKeyFromAnotherGroup;
	};

	this.getPrivateKeyExponentsFromAnotherGroup = function () {
		return _privateKeyExponentsFromAnotherGroup;
	};

	this.getPrivateKeyFromAnotherGroup = function () {
		return _privateKeyFromAnotherGroup;
	};

	this.getElementsFromAnotherGroup = function () {
		return _elementsFromAnotherGroup;
	};
}
