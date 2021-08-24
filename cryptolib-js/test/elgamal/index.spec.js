/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true, mocha: true, expr:true */
'use strict';

const { assert, expect } = require('chai');

const CommonTestData = require('./data/common-data');
const elGamal = require('../../src/elgamal');
const mathematical = require('../../src/mathematical');
const secureRandom = require('../../src/securerandom');
const cryptoPolicy = require('../../src/cryptopolicy');

describe('The ElGamal cryptography module should be able to ...', function () {
	let _elGamalSmallService;
	let _secureRandomService;
	let _mathService;
	let _mathRandomGenerator;
	let _group;
	let _publicKey;
	let _privateKey;
	let _publicKeyElements;
	let _privateKeyExponents;
	let _keyPair;
	let _elements;
	let _elementValueStrings;
	let _largePublicKey;
	let _largePrivateKey;
	let _elementsFromLargeGroup;
	let _elementsFromLargeNonQrGroup;
	let _identityElements;
	let _shortExponentBitLength;
	let _elGamalEncrypter;
	let _elGamalDecrypter;
	let _encryptedElements;
	let _elGamalEncrypterLargeGroup;
	let _elGamalDecrypterLargeGroup;
	let _encrypterRandomGenerator;
	let _decrypterRandomGenerator;
	let _smallPolicy;
	let _elGamalLargeService;

	beforeEach(function () {
		_smallPolicy = cryptoPolicy.newInstance();
		_smallPolicy.mathematical.groups.type =
			cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
		_elGamalSmallService = elGamal.newService({policy: _smallPolicy});
		_secureRandomService = secureRandom;
		_mathService =
			mathematical.newService({policy: _smallPolicy, secureRandomService: _secureRandomService});
		_mathRandomGenerator = _mathService.newRandomGenerator();

		const testData = new CommonTestData();
		_group = testData.getGroup();
		_publicKey = testData.getPublicKey();
		_privateKey = testData.getPrivateKey();
		_publicKeyElements = testData.getPublicKeyElements();
		_privateKeyExponents = testData.getPrivateKeyExponents();
		_keyPair = testData.getKeyPair();
		_elements = testData.getZpGroupElements();
		_elementValueStrings = testData.getZpGroupElementValueStrings();
		_largePublicKey = testData.getLargePublicKey();
		_largePrivateKey = testData.getLargePrivateKey();
		_elementsFromLargeGroup = testData.getElementsFromLargeZpSubgroup();
		_elementsFromLargeNonQrGroup =
			testData.getElementsFromLargeNonQrZpSubgroup();
		_identityElements = testData.getIdentityElements();
		_shortExponentBitLength = testData.getShortExponentBitLength();

		_elGamalEncrypter = _elGamalSmallService.newEncrypter().init(_publicKey);
		_elGamalDecrypter = _elGamalSmallService.newDecrypter().init(_privateKey);

		_encryptedElements = _elGamalEncrypter.encrypt(_elements);

		_elGamalLargeService = elGamal.newService();

		_elGamalEncrypterLargeGroup =
			_elGamalLargeService.newEncrypter().init(_largePublicKey);
		_elGamalDecrypterLargeGroup =
			_elGamalLargeService.newDecrypter().init(_largePrivateKey);
	});

	beforeEach(function () {
		_encrypterRandomGenerator =
			secureRandom.newService().newRandomGenerator();

		_decrypterRandomGenerator =
			secureRandom.newService().newRandomGenerator();
	});

	describe('create an ElGamal cryptography service that should be able to ..', function () {
		it('create a new ElGamal key pair', function () {
			validatePublicKey(_keyPair.publicKey, _group);
			validatePrivateKey(_keyPair.privateKey, _group);
		});

		it('create a new ElGamalEncryptedElements object', function () {
			const encryptedElements = _elGamalEncrypter.encrypt(_elements);

			const gamma = encryptedElements.gamma;
			const phis = encryptedElements.phis;

			const newEncryptedElements =
				_elGamalSmallService.newEncryptedElements(gamma, phis);

			const decryptedElements = _elGamalDecrypter.decrypt(newEncryptedElements);

			checkDecryptedElements(decryptedElements, _elements);
		});

		it('create a new ElGamalEncryptedElements object, with a saved secret',
			function () {
				const encryptedElementsAndSecret =
					_elGamalEncrypter.encrypt(_elements, {saveSecret: true});

				const gamma = encryptedElementsAndSecret.gamma;
				const phis = encryptedElementsAndSecret.phis;
				const secret = encryptedElementsAndSecret.secret;

				const newEncryptedElementsAndSecret =
					_elGamalSmallService.newEncryptedElements(gamma, phis, secret);

				assert.isTrue(newEncryptedElementsAndSecret.secret.equals(secret));

				const encryptedElements = _elGamalSmallService.newEncryptedElements(
					newEncryptedElementsAndSecret.gamma,
					newEncryptedElementsAndSecret.phis);

				const decryptedElements = _elGamalDecrypter.decrypt(encryptedElements);
				checkDecryptedElements(decryptedElements, _elements);
			});

		it('serialize and deserialize an ElGamal public key', function () {
			const publicKeyJson = _publicKey.toJson();

			const publicKeyFromJson = _elGamalSmallService.newPublicKey(publicKeyJson);

			validatePublicKey(publicKeyFromJson, _group);
		});

		it('serialize and deserialize an ElGamal private key', function () {
			const privateKeyJson = _privateKey.toJson();

			const privateKeyFromJson = _elGamalSmallService.newPrivateKey(privateKeyJson);

			validatePrivateKey(privateKeyFromJson, _group);
		});

		it('serialize and deserialize ElGamal encrypted elements', function () {
			const encryptedElements = _elGamalEncrypter.encrypt(_elements);

			const encryptedElementsJson = encryptedElements.toJson();
			const encryptedElementsFromJson =
				_elGamalSmallService.newEncryptedElements(encryptedElementsJson);

			const decryptedElements =
				_elGamalDecrypter.decrypt(encryptedElementsFromJson);

			checkDecryptedElements(decryptedElements, _elements);
		});

		it('serialize and deserialize an encryption pre-computation', function () {
			const preComputation = _elGamalEncrypter.preCompute();

			const preComputationJson = preComputation.toJson();
			const preComputationFromJson =
				_elGamalSmallService.newEncryptedElements(preComputationJson);

			const encryptedElements = _elGamalEncrypter.encrypt(
				_elements, {preComputation: preComputationFromJson});

			const decryptedElements = _elGamalDecrypter.decrypt(encryptedElements);

			checkDecryptedElements(decryptedElements, _elements);
		});

		describe('create an encrypter/decrypter pair that should be able to ..', function () {
			it('encrypt and decrypt some group elements', function () {
				const decryptedElements = _elGamalDecrypter.decrypt(_encryptedElements);

				checkDecryptedElements(decryptedElements, _elements);
			});

			it('encrypt and decrypt some group elements, using a specified secure random service object',
				function () {
					const elGamalService =
						elGamal.newService({policy: _smallPolicy, secureRandomService: _secureRandomService});
					const elGamalEncrypter =
						elGamalService.newEncrypter().init(_publicKey);
					const elGamalDecrypter =
						elGamalService.newDecrypter().init(_privateKey);

					const encryptedElements = elGamalEncrypter.encrypt(_elements);
					const decryptedElements = elGamalDecrypter.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, _elements);
				});

			it('encrypt and decrypt some group elements, using a specified mathematical service object',
				function () {
					const elGamalService =
						elGamal.newService({mathematicalService: _mathService});
					const elGamalEncrypter =
						elGamalService.newEncrypter().init(_publicKey);
					const elGamalDecrypter =
						elGamalService.newDecrypter().init(_privateKey);

					const encryptedElements = elGamalEncrypter.encrypt(_elements);
					const decryptedElements = elGamalDecrypter.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, _elements);
				});

			it('encrypt and decrypt some group element value strings', function () {
				const encryptedElements = _elGamalEncrypter.encrypt(_elementValueStrings);

				const decryptedElements = _elGamalDecrypter.decrypt(encryptedElements);

				checkDecryptedElements(decryptedElements, _elements);
			});

			it('decrypt some group elements, with the check group membership flag set to true',
				function () {
					const decryptedElements = _elGamalDecrypter.decrypt(
						_encryptedElements, {checkMembership: true});

					checkDecryptedElements(decryptedElements, _elements);
				});

			it('decrypt some group elements, with check group membership flag set to false',
				function () {
					const decryptedElements = _elGamalDecrypter.decrypt(
						_encryptedElements, {checkMembership: false});

					checkDecryptedElements(decryptedElements, _elements);
				});

			it('throw an error when decrypting some group elements, the check group membership flag is set to true and the check fails',
				function () {
					expect(function () {
						_elGamalDecrypterLargeGroup.decrypt(
							_encryptedElements, {checkMembership: true});
					}).to.throw();
				});

			it('encrypt and decrypt some group elements, using an encryption pre-computation',
				function () {
					const preComputation = _elGamalEncrypter.preCompute();
					const encryptedElements = _elGamalEncrypter.encrypt(
						_elements, {preComputation: preComputation});

					const decryptedElements = _elGamalDecrypter.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, _elements);
				});

			it('encrypt and decrypt some group elements and retrieve the secret exponent',
				function () {
					const encryptedElementsAndSecret =
						_elGamalEncrypter.encrypt(_elements, {saveSecret: true});

					checkSecretExponent(
						_publicKey, encryptedElementsAndSecret, _elements);

					const encryptedElements = _elGamalSmallService.newEncryptedElements(
						encryptedElementsAndSecret.gamma,
						encryptedElementsAndSecret.phis);

					const decryptedElements = _elGamalDecrypter.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, _elements);
				});

			it('encrypt and decrypt some group elements, using an encryption pre-computation, and retrieve the secret exponent after pre-computing',
				function () {
					const preComputationAndSecret =
						_elGamalEncrypter.preCompute({saveSecret: true});

					checkSecretExponent(
						_publicKey, preComputationAndSecret, _identityElements);

					const preComputation = _elGamalSmallService.newEncryptedElements(
						preComputationAndSecret.gamma, preComputationAndSecret.phis);

					const encryptedElements = _elGamalEncrypter.encrypt(
						_elements, {preComputation: preComputation});

					const decryptedElements = _elGamalDecrypter.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, _elements);
				});

			it('encrypt and decrypt some group elements, using encrypted elements generated with a short exponent',
				function () {
					const encryptedElementsAndSecret = _elGamalEncrypterLargeGroup.encrypt(
						_elementsFromLargeGroup,
						{useShortExponent: true, saveSecret: true});
					expect(encryptedElementsAndSecret.secret.value.bitLength())
						.not.to.be.above(_shortExponentBitLength);

					const encryptedElements = _elGamalLargeService.newEncryptedElements(
						encryptedElementsAndSecret.gamma,
						encryptedElementsAndSecret.phis);

					const decryptedElements =
						_elGamalDecrypterLargeGroup.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, _elementsFromLargeGroup);
				});

			it('encrypt and decrypt some group elements, using an encryption pre-computation generated with a short exponent',
				function () {
					const preComputationAndSecret = _elGamalEncrypterLargeGroup.preCompute(
						{useShortExponent: true, saveSecret: true});
					expect(preComputationAndSecret.secret.value.bitLength())
						.not.to.be.above(_shortExponentBitLength);

					const preComputation = _elGamalLargeService.newEncryptedElements(
						preComputationAndSecret.gamma, preComputationAndSecret.phis);

					const encryptedElements = _elGamalEncrypterLargeGroup.encrypt(
						_elementsFromLargeGroup, {preComputation: preComputation});

					const decryptedElements =
						_elGamalDecrypterLargeGroup.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, _elementsFromLargeGroup);
				});

			it('encrypt and decrypt some group elements, when the public key and private key undergo compression',
				function () {
					const encryptedElements = _elGamalEncrypter.encrypt([_elements[0]]);

					const decryptedElements = _elGamalDecrypter.decrypt(encryptedElements);

					checkDecryptedElements(decryptedElements, [_elements[0]]);
				});

			it('throw an error when encrypting before the encrypter has been initialized with a public key',
				function () {
					const encrypter = _elGamalSmallService.newEncrypter();

					expect(function () {
						encrypter.encrypt(_elements);
					}).to.throw();
				});

			it('throw an error when decrypting before the decrypter has been initialized with a private key',
				function () {
					const decrypter = _elGamalSmallService.newDecrypter();

					expect(function () {
						decrypter.decrypt(_encryptedElements);
					}).to.throw();
				});

			it('throw an error when attempting to decrypt non-group members and group membership is being checked',
				function () {
					expect(function () {
						const encryptedElements =
							_elGamalEncrypterLargeGroup.encrypt(_elementsFromLargeGroup);

						_elGamalDecrypter.decrypt(
							encryptedElements, {checkMembership: true});
					}).to.throw();
				});

			it('throw an error when attempting to ElGamal encrypt, using a short exponent, for a group that is not of type quadratic residue',
				function () {
					expect(function () {
						_elGamalEncrypterLargeGroup.encrypt(
							_elementsFromLargeNonQrGroup, {useShortExponent: true});
					}).to.throw();
				});

			it('throw an error when attempting to ElGamal encrypt, using a short exponent, for a group whose order is smaller than this exponent',
				function () {
					expect(function () {
						_elGamalEncrypter.encrypt(_elements, {useShortExponent: true});
					}).to.throw();
				});
		});
	});

	function validatePublicKey(publicKey, group) {
		const elements = publicKey.elements;
		const numElements = elements.length;
		expect(numElements).to.equal(_publicKeyElements.length);
		for (let i = 0; i < numElements; i++) {
			expect(elements[i].value.toString())
				.to.equal(_publicKeyElements[i].value.toString());
		}

		assert.isTrue(publicKey.group.equals(group));
	}

	function validatePrivateKey(privateKey, group) {
		const exponents = privateKey.exponents;
		const numExponents = exponents.length;
		expect(numExponents).to.equal(_privateKeyExponents.length);
		for (let i = 0; i < numExponents; i++) {
			expect(exponents[i].value.toString())
				.to.equal(_privateKeyExponents[i].value.toString());
		}

		assert.isTrue(privateKey.group.equals(group));
	}

	function checkDecryptedElements(decryptedElements, elements) {
		expect(decryptedElements.length).to.equal(elements.length);

		for (let i = 0; i < decryptedElements.length; i++) {
			assert.isTrue(decryptedElements[i].equals(elements[i]));
		}
	}

	function checkSecretExponent(
		publicKey, encryptedElementsAndSecret, elements) {
		const publicKeyElements = publicKey.elements;

		const gamma = encryptedElementsAndSecret.gamma;
		const phis = encryptedElementsAndSecret.phis;
		const secret = encryptedElementsAndSecret.secret;

		assert.isTrue((_group.generator.exponentiate(secret)).equals(gamma));
		for (let i = 0; i < phis.length; i++) {
			assert.isTrue((publicKeyElements[i].exponentiate(secret).multiply(elements[i])).equals(phis[i]));
		}
	}
});
