/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const { expect } = require('chai');

const CommonTestData = require('./data/common-data');
const ValidationTestData = require('./data/validation-data');
const elGamal = require('../../src/elgamal');
const cryptoPolicy = require('../../src/cryptopolicy');

const expectedThrownMessage = 'Expected Q to have a length of 256 for group type ZP_2048_256; Found 2047';

describe('The ElGamal cryptography module should be able to ...', function () {
	let _elGamalService;

	let _group;
	let _anotherGroup;
	let _publicKey;
	let _privateKey;
	let _publicKeyElements;
	let _publicKeyElementsFromAnotherGroup;
	let _privateKeyExponents;
	let _privateKeyExponentsFromAnotherGroup;

	let _elements;

	let _nonObject;
	let _emptyObject;
	let _nonBoolean;
	let _nonJsonString;
	let _nonArray;
	let _nonStringArray;
	let _nonObjectArray;
	let _emptyObjectArray;
	let _elementsFromAnotherGroup;
	let _publicKeyFromAnotherGroup;
	let _privateKeyFromAnotherGroup;

	let _elGamalEncrypter;
	let _elGamalDecrypter;
	let _encryptedElements;
	let _gamma;
	let _phis;
	let _encryptedElementsFromAnotherGroup;

	beforeEach(function () {
		const commonTestData = new CommonTestData();
		_group = commonTestData.getGroup();
		_anotherGroup = commonTestData.getLargeGroup();
		_publicKey = commonTestData.getPublicKey();
		_privateKey = commonTestData.getPrivateKey();
		_publicKeyElements = commonTestData.getPublicKeyElements();
		_privateKeyExponents = commonTestData.getPrivateKeyExponents();
		_elements = commonTestData.getZpGroupElements();

		const validationTestData = new ValidationTestData();
		_nonObject = validationTestData.getNonObject();
		_emptyObject = validationTestData.getEmptyObject();
		_nonBoolean = validationTestData.getNonBoolean();
		_nonJsonString = validationTestData.getNonJsonString();
		_nonArray = validationTestData.getNonArray();
		_nonStringArray = validationTestData.getNonStringArray();
		_nonObjectArray = validationTestData.getNonObjectArray();
		_emptyObjectArray = validationTestData.getEmptyObjectArray();
		_elementsFromAnotherGroup =
			validationTestData.getElementsFromAnotherGroup();
		_publicKeyElementsFromAnotherGroup =
			validationTestData.getPublicKeyElementsFromAnotherGroup();
		_publicKeyFromAnotherGroup =
			validationTestData.getPublicKeyFromAnotherGroup();
		_privateKeyExponentsFromAnotherGroup =
			validationTestData.getPrivateKeyExponentsFromAnotherGroup();
		_privateKeyFromAnotherGroup =
			validationTestData.getPrivateKeyFromAnotherGroup();

		const policy = cryptoPolicy.newInstance();
		policy.mathematical.groups.type =
			cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
		_elGamalService = elGamal.newService({policy: policy});

		_elGamalEncrypter = elGamal.newService({policy: policy}).newEncrypter().init(_publicKey);
		_elGamalDecrypter = elGamal.newService({policy: policy}).newDecrypter().init(_privateKey);

		const encryptedElementsAndSecret =
			_elGamalEncrypter.encrypt(_elements, {saveSecret: true});
		_gamma = encryptedElementsAndSecret.gamma;
		_phis = encryptedElementsAndSecret.phis;
		_encryptedElements = _elGamalService.newEncryptedElements(
			encryptedElementsAndSecret.gamma, encryptedElementsAndSecret.phis);

		_encryptedElementsFromAnotherGroup =
			elGamal.newService().newEncrypter()
				.init(_publicKeyFromAnotherGroup)
				.encrypt(_elementsFromAnotherGroup);
	});

	describe('create an ElGamal cryptography service that should be able to ..', function () {
		it('throw an error when being created, using an invalid secure random service object',
			function () {
				expect(function () {
					elGamal.newService({secureRandomService: null});
				}).to.throw();

				expect(function () {
					elGamal.newService({secureRandomService: _nonObject});
				}).to.throw();

				expect(function () {
					elGamal.newService({secureRandomService: _emptyObject});
				}).to.throw();
			});

		it('throw an error when being created, using an invalid mathematical service object',
			function () {
				expect(function () {
					elGamal.newService({mathematicalService: null});
				}).to.throw();

				expect(function () {
					elGamal.newService({mathematicalService: _nonObject});
				}).to.throw();

				expect(function () {
					elGamal.newService({mathematicalService: _emptyObject});
				}).to.throw();
			});

		it('throw an error when creating a new ElGamalPublicKey object, using invalid input data',
			function () {
				expect(function () {
					_elGamalService.newPublicKey(_group);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(undefined, _publicKeyElements);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(null, _publicKeyElements);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_nonObject, _publicKeyElements);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_emptyObject, _publicKeyElements);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_group, undefined);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_group, null);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_group, _nonArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_group, _nonObjectArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_group, _emptyObjectArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newPublicKey(_anotherGroup, _publicKeyElementsFromAnotherGroup);
				}).to.throw(expectedThrownMessage);

				expect(function () {
					_elGamalService.newPublicKey(_nonJsonString);
				}).to.throw();
			});

		it('throw an error when creating a new ElGamalPrivateKey object, using invalid input data',
			function () {
				expect(function () {
					_elGamalService.newPrivateKey(undefined, _privateKeyExponents);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(null, _privateKeyExponents);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_nonObject, _privateKeyExponents);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_emptyObject, _privateKeyExponents);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_group);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_group, undefined);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_group, null);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_group, _nonArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_group, _nonObjectArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_group, _emptyObjectArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newPrivateKey(_anotherGroup, _privateKeyExponentsFromAnotherGroup);
				}).to.throw(expectedThrownMessage);

				expect(function () {
					_elGamalService.newPrivateKey(_nonJsonString);
				}).to.throw();
			});

		it('throw an error when creating a new ElGamalKeyPair object, using invalid input data',
			function () {
				expect(function () {
					_elGamalService.newKeyPair(_publicKey);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(undefined, _privateKey);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(null, _privateKey);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(_nonObject, _privateKey);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(_emptyObject, _privateKey);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(_publicKey, undefined);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(_publicKey, null);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(_publicKey, _nonObject);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(_publicKey, _emptyObject);
				}).to.throw();

				expect(function () {
					_elGamalService.newKeyPair(_publicKeyFromAnotherGroup, _privateKeyFromAnotherGroup);
				}).to.throw(expectedThrownMessage);
			});

		it('throw an error when creating a new ElGamalEncryptedElements object, using invalid input data',
			function () {
				expect(function () {
					_elGamalService.newEncryptedElements(undefined, _phis);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(null, _phis);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_nonObject, _phis);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_emptyObject, _phis);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, undefined);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, null);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, _nonArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, _nonObjectArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, _emptyObjectArray);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, _phis, null);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, _phis, _nonObject);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_gamma, _phis, _emptyObject);
				}).to.throw();

				expect(function () {
					_elGamalService.newEncryptedElements(_encryptedElementsFromAnotherGroup.gamma, _encryptedElementsFromAnotherGroup.phis);
				}).to.throw(expectedThrownMessage);

				expect(function () {
					_elGamalService.newEncryptedElements(_nonJsonString);
				}).to.throw();
			});

		describe('Create an encrypter/decrypter pair that should ..', function () {
			it('throw an error when being initialized, using invalid input data',
				function () {
					expect(function () {
						_elGamalEncrypter.init();
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.init(undefined);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.init(null);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.init(_nonObject);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.init(_emptyObject);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.init(_publicKeyFromAnotherGroup);
					}).to.throw(expectedThrownMessage);

					expect(function () {
						_elGamalDecrypter.init();
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.init(undefined);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.init(null);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.init(_nonObject);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.init(_emptyObject);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.init(_privateKeyFromAnotherGroup);
					}).to.throw(expectedThrownMessage);
				});

			it('throw an error when encrypting an array of Zp group elements, using invalid input data',
				function () {
					expect(function () {
						_elGamalEncrypter.encrypt();
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(undefined);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(null);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_nonArray);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_nonObjectArray);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_emptyObjectArray);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(
							_elements, {useShortExponent: _nonBoolean});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_elements, {preComputation: null});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_elements, {preComputation: _nonObject});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(
							_elements, {preComputation: _emptyObject});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_elementsFromAnotherGroup);
					}).to.throw();
				});

			it('throw an error when encrypting an array of Zp group element strings, using invalid input data',
				function () {
					expect(function () {
						_elGamalEncrypter.encrypt();
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(undefined);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(null);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_nonArray);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_nonStringArray);
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_elements, {preComputation: null});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_elements, {preComputation: _nonObject});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(
							_elements, {preComputation: _emptyObject});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(
							_elements, {useShortExponent: _nonBoolean});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.encrypt(_elements, {saveSecret: _nonBoolean});
					}).to.throw();
				});

			it('throw an error when pre-computing an ElGamal encryption, using invalid input data',
				function () {
					expect(function () {
						_elGamalEncrypter.preCompute({useShortExponent: _nonBoolean});
					}).to.throw();

					expect(function () {
						_elGamalEncrypter.preCompute({saveSecret: _nonBoolean});
					}).to.throw();
				});

			it('throw an error when decrypting an ElGamalEncryptedElements object, using invalid input data',
				function () {
					expect(function () {
						_elGamalDecrypter.decrypt();
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.decrypt(undefined);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.decrypt(null);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.decrypt(_nonObject);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.decrypt(_emptyObject);
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.decrypt(
							_encryptedElements, {confirmMembership: null});
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.decrypt(
							_encryptedElements, {confirmMembership: _nonBoolean});
					}).to.throw();

					expect(function () {
						_elGamalDecrypter.decrypt(_encryptedElementsFromAnotherGroup);
					}).to.throw(expectedThrownMessage);
				});
		});
	});
});
