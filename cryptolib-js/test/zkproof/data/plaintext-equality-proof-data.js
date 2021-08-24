/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const CommonTestData = require('./common-data');
const elGamal = require('../../../src/elgamal');
const cryptoPolicy = require('../../../src/cryptopolicy');

module.exports = PlaintextEqualityProofTestData;

/**
 * Provides the data needed by the plaintext equality zero-knowledge proof of
 * knowledge unit tests.
 */
function PlaintextEqualityProofTestData() {
	const NUM_PLAINTEXT_ELEMENTS = 3;
	const PLAINTEXT_EQUALITY_PROOF_STRING_DATA =
		'Test Plaintext Equality Proof Auxilary Data';
	const OTHER_PLAINTEXT_EQUALITY_PROOF_STRING_DATA =
		PLAINTEXT_EQUALITY_PROOF_STRING_DATA + '0';

	const policy = cryptoPolicy.newInstance();
	policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
	const elGamalEncrypter = elGamal.newService({policy: policy}).newEncrypter();

	const zp224Policy = cryptoPolicy.newInstance();
	zp224Policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_224;
	const _zp224ElGamalService = elGamal.newService({policy: zp224Policy});
	const zp224ElGamalEncrypter = _zp224ElGamalService.newEncrypter();

	const commonTestData = new CommonTestData();
	const _group = commonTestData.getGroup();
	const _zp224Group = commonTestData.getZP224Group();
	const _anotherGroup = commonTestData.getAnotherGroup();

	const _primaryPublicKey =
		commonTestData.generateKeyPair(_group, NUM_PLAINTEXT_ELEMENTS).publicKey;
	const _secondaryPublicKey =
		commonTestData.generateKeyPair(_group, NUM_PLAINTEXT_ELEMENTS).publicKey;
	let _anotherPrimaryPublicKey;
	do {
		_anotherPrimaryPublicKey =
			commonTestData.generateKeyPair(_group, NUM_PLAINTEXT_ELEMENTS)
				.publicKey;
	} while (
		_anotherPrimaryPublicKey.elements.equals(_primaryPublicKey.elements));
	let _anotherSecondaryPublicKey;
	do {
		_anotherSecondaryPublicKey =
			commonTestData.generateKeyPair(_group, NUM_PLAINTEXT_ELEMENTS)
				.publicKey;
	} while (
		_anotherSecondaryPublicKey.elements.equals(_secondaryPublicKey.elements));
	const _publicKeyWithLessElements =
		commonTestData.generateKeyPair(_group, (NUM_PLAINTEXT_ELEMENTS - 1))
			.publicKey;
	const _publicKeyFromAnotherGroup =
		commonTestData.generateKeyPair(_anotherGroup, NUM_PLAINTEXT_ELEMENTS)
			.publicKey;
	const _zp224PublicKey =
		commonTestData.generateKeyPair(_zp224Group, NUM_PLAINTEXT_ELEMENTS,
			_zp224ElGamalService).publicKey;

	const plaintext = commonTestData.generateRandomGroupElements(
		_group, NUM_PLAINTEXT_ELEMENTS);
	let anotherPlaintext;
	do {
		anotherPlaintext = commonTestData.generateRandomGroupElements(
			_group, NUM_PLAINTEXT_ELEMENTS);
	} while (anotherPlaintext.equals(plaintext));
	const plaintextWithLessElements = commonTestData.generateRandomGroupElements(
		_group, (NUM_PLAINTEXT_ELEMENTS - 1));
	const plaintextFromAnotherGroup = commonTestData.generateRandomGroupElements(
		_anotherGroup, NUM_PLAINTEXT_ELEMENTS);
	const zp224Plaintext = commonTestData.generateRandomGroupElements(
		_zp224Group, NUM_PLAINTEXT_ELEMENTS);

	const _primaryEncryptedElements =
		elGamalEncrypter.init(_primaryPublicKey).encrypt(plaintext, {
			saveSecret: true
		});
	const _secondaryEncryptedElements =
		elGamalEncrypter.init(_secondaryPublicKey).encrypt(plaintext, {
			saveSecret: true
		});
	let _otherPrimaryEncryptedElements;
	do {
		_otherPrimaryEncryptedElements =
			elGamalEncrypter.init(_primaryPublicKey).encrypt(plaintext, {
				saveSecret: true
			});
	} while (_otherPrimaryEncryptedElements.secret.equals(
		_primaryEncryptedElements.secret) ||
	_otherPrimaryEncryptedElements.phis.equals(
		_primaryEncryptedElements.phis));
	let _otherSecondaryEncryptedElements;
	do {
		_otherSecondaryEncryptedElements =
			elGamalEncrypter.init(_secondaryPublicKey).encrypt(plaintext, {
				saveSecret: true
			});
	} while (_otherSecondaryEncryptedElements.secret.equals(
		_secondaryEncryptedElements.secret) ||
	_otherSecondaryEncryptedElements.phis.equals(
		_secondaryEncryptedElements.phis));
	const _encryptedElementsWithLessElements =
		elGamalEncrypter.init(_publicKeyWithLessElements)
			.encrypt(plaintextWithLessElements, {saveSecret: true});
	const _encryptedElementsFromAnotherGroup =
		elGamalEncrypter.init(_publicKeyFromAnotherGroup)
			.encrypt(plaintextFromAnotherGroup, {saveSecret: true});
	const _zp224EncryptedElements = zp224ElGamalEncrypter.init(_zp224PublicKey)
		.encrypt(zp224Plaintext, {saveSecret: true});

	this.getGroup = function () {
		return _group;
	};

	this.getZP224Group = function () {
		return _zp224Group;
	};

	this.getPrimaryPublicKey = function () {
		return _primaryPublicKey;
	};

	this.getSecondaryPublicKey = function () {
		return _secondaryPublicKey;
	};

	this.getAnotherPrimaryPublicKey = function () {
		return _anotherPrimaryPublicKey;
	};

	this.getAnotherSecondaryPublicKey = function () {
		return _anotherSecondaryPublicKey;
	};

	this.getPublicKeyWithLessElements = function () {
		return _publicKeyWithLessElements;
	};

	this.getPublicKeyFromAnotherGroup = function () {
		return _publicKeyFromAnotherGroup;
	};

	this.getZP224PublicKey = function () {
		return _zp224PublicKey;
	};

	this.getPrimaryEncryptedElements = function () {
		return _primaryEncryptedElements;
	};

	this.getSecondaryEncryptedElements = function () {
		return _secondaryEncryptedElements;
	};

	this.getOtherPrimaryEncryptedElements = function () {
		return _otherPrimaryEncryptedElements;
	};

	this.getOtherSecondaryEncryptedElements = function () {
		return _otherSecondaryEncryptedElements;
	};

	this.getEncryptedElementsWithLessElements = function () {
		return _encryptedElementsWithLessElements;
	};

	this.getEncryptedElementsFromAnotherGroup = function () {
		return _encryptedElementsFromAnotherGroup;
	};

	this.getZP224EncryptedElements = function () {
		return _zp224EncryptedElements;
	};

	this.getStringData = function () {
		return PLAINTEXT_EQUALITY_PROOF_STRING_DATA;
	};

	this.getOtherStringData = function () {
		return OTHER_PLAINTEXT_EQUALITY_PROOF_STRING_DATA;
	};
}
