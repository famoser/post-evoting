/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const {expect} = require('chai');

const mathematical = require('../../src/mathematical');
const validator = require('../../src/input-validator');
const CommonTestData = require('./data/common-data');
const ValidationTestData = require('./data/validation-data');
const cryptoPolicy = require('../../src/cryptopolicy');

describe('The mathematical module should be able to ...', function () {
	let _mathService;
	let _mathArrayCompressor;
	let _mathRandomGenerator;
	let _mathGroupHandler;

	let _g;
	let _p;
	let _q;
	let _anotherQ;
	let _group;
	let _anotherGroup;

	let _elementValue;
	let _element;
	let _exponentValue;
	let _exponent;
	let _elements;
	let _mixedElements;
	let _exponents;
	let _elementFromAnotherGroup;
	let _exponentFromAnotherGroup;
	let _multiGroupElementValues;
	let _minCertaintyLevel;

	let _nonObject;
	let _emptyObject;
	let _nonBoolean;
	let _nonNumber;
	let _nonPositiveNumber;
	let _nonJsonString;
	let _nonArray;
	let _nonBigInteger;
	let _nonObjectArray;
	let _emptyObjectArray;
	let _tooSmallModulus;
	let _tooSmallOrder;
	let _tooLargeOrder;
	let _tooSmallGenerator;
	let _tooLargeGenerator;
	let _tooSmallElementValue;
	let _tooLargeElementValue;
	let _tooSmallModulusBitLength;
	let _tooLowCertaintyLevel;
	let _tooLargeNumMembersRequired;

	beforeEach(function () {
		_mathService = mathematical.newService();
		_mathArrayCompressor = _mathService.newArrayCompressor();
		_mathRandomGenerator = _mathService.newRandomGenerator();
		_mathGroupHandler = _mathService.newGroupHandler();

		const commonTestData = new CommonTestData();
		_p = commonTestData.getP();
		_q = commonTestData.getQ();
		_anotherQ = commonTestData.getAnotherQ();
		_g = commonTestData.getG();
		_group = commonTestData.getGroup();
		_anotherGroup = commonTestData.getAnotherGroup();
		_elementValue = commonTestData.getElementValue();
		_element = commonTestData.getElement();
		_exponentValue = commonTestData.getExponentValue();
		_exponent = commonTestData.getExponent();
		_elements = commonTestData.getElements();
		_mixedElements = commonTestData.getMixedElements();
		_exponents = commonTestData.getExponents();
		_elementFromAnotherGroup = commonTestData.getElementFromAnotherGroup();
		_exponentFromAnotherGroup = commonTestData.getExponentFromAnotherGroup();
		_multiGroupElementValues = commonTestData.multiGroupElementValues();
		_minCertaintyLevel = commonTestData.getMinimumCertaintyLevel();

		const validationTestData = new ValidationTestData();
		_nonObject = validationTestData.getNonObject();
		_emptyObject = validationTestData.getEmptyObject();
		_nonBoolean = validationTestData.getNonBoolean();
		_nonNumber = validationTestData.getNonNumber();
		_nonPositiveNumber = validationTestData.getNonPositiveNumber();
		_nonJsonString = validationTestData.getNonJsonString();
		_nonArray = validationTestData.getNonArray();
		_nonBigInteger = validationTestData.getNonBigInteger();
		_nonObjectArray = validationTestData.getNonObjectArray();
		_emptyObjectArray = validationTestData.getEmptyObjectArray();
		_tooSmallModulus = validationTestData.getTooSmallModulus();
		_tooSmallOrder = validationTestData.getTooSmallOrder();
		_tooLargeOrder = validationTestData.getTooLargeOrder();
		_tooSmallGenerator = validationTestData.getTooSmallGenerator();
		_tooLargeGenerator = validationTestData.getTooLargeGenerator();
		_tooSmallElementValue = validationTestData.getTooSmallElementValue();
		_tooLargeElementValue = validationTestData.getTooLargeElementValue();
		_tooSmallModulusBitLength = validationTestData.getTooSmallModulusBitLength();
		_tooLowCertaintyLevel = validationTestData.getTooLowCertaintyLevel();
		_tooLargeNumMembersRequired = validationTestData.getTooLargeNumMembersRequired();
	});

	describe('create a mathematical service that should be able to ..', function () {
		it('throw an error when being created, using an invalid secure random service object',
			function () {
				expect(function () {
					mathematical.newService({secureRandomService: null});
				}).to.throw();

				expect(function () {
					mathematical.newService({secureRandomService: _nonObject});
				}).to.throw();

				expect(function () {
					mathematical.newService({secureRandomService: _emptyObject});
				}).to.throw();
			});

		it('throw an error when creating a new ZpSubgroup object, using an invalid modulus p or JSON representation',
			function () {
				expect(function () {
					_mathService.newZpSubgroup(undefined, _q, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(null, _q, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_nonBigInteger, _q, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_tooSmallModulus, _q, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_nonJsonString);
				}).to.throw();
			});

		it('throw an error when creating a new ZpSubgroup object, using an invalid order q',
			function () {
				expect(function () {
					_mathService.newZpSubgroup(_p, undefined, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, null, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _nonBigInteger, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _tooSmallOrder, _g);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _tooLargeOrder, _g);
				}).to.throw();
			});

		it('throw an error when creating a new ZpSubgroup object, using an invalid generator',
			function () {
				expect(function () {
					_mathService.newZpSubgroup(_p, _q);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _q, undefined);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _q, null);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _q, _nonBigInteger);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _q, _tooSmallGenerator);
				}).to.throw();

				expect(function () {
					_mathService.newZpSubgroup(_p, _q, _tooLargeGenerator);
				}).to.throw();
			});

		it('throw an error when creating a new ZpGroupElement object, using an invalid modulus p or JSON representation',
			function () {
				expect(function () {
					_mathService.newZpGroupElement(undefined, _q, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(null, _q, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_nonBigInteger, _q, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_tooSmallModulus, _q, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_nonJsonString);
				}).to.throw();
			});

		it('throw an error when creating a new ZpGroupElement object, using an invalid order q',
			function () {
				expect(function () {
					_mathService.newZpGroupElement(_p, undefined, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, null, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _nonBigInteger, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _tooSmallOrder, _elementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _tooLargeOrder, _elementValue);
				}).to.throw();
			});

		it('throw an error when creating a new ZpGroupElement object, using an invalid element value',
			function () {
				expect(function () {
					_mathService.newZpGroupElement(_p, _q);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _q, undefined);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _q, null);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _q, _nonBigInteger);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _q, _tooSmallElementValue);
				}).to.throw();

				expect(function () {
					_mathService.newZpGroupElement(_p, _q, _tooLargeElementValue);
				}).to.throw();
			});

		it('throw an error when creating a new Exponent object, using an invalid order q or JSON representation',
			function () {
				expect(function () {
					_mathService.newExponent(undefined, _exponentValue);
				}).to.throw();

				expect(function () {
					_mathService.newExponent(null, _exponentValue);
				}).to.throw();

				expect(function () {
					_mathService.newExponent(_nonBigInteger, _exponentValue);
				}).to.throw();

				expect(function () {
					_mathService.newExponent(_tooSmallOrder, _exponentValue);
				}).to.throw();

				expect(function () {
					_mathService.newExponent(_nonJsonString);
				}).to.throw();
			});

		it('throw an error when creating a new Exponent object, using an invalid exponent value',
			function () {
				expect(function () {
					_mathService.newExponent(_q);
				}).to.throw();

				expect(function () {
					_mathService.newExponent(_q, undefined);
				}).to.throw();

				expect(function () {
					_mathService.newExponent(_q, null);
				}).to.throw();

				expect(function () {
					_mathService.newExponent(_q, _nonBigInteger);
				}).to.throw();
			});

		it('throw an error when creating a new quadratic residue Zp subgroup, using invalid input data',
			function () {
				expect(function () {
					_mathService.newQuadraticResidueGroup(_p);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(undefined, _q);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(null, _q);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(_nonBigInteger, _q);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(_tooSmallModulus, _q);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(_p, undefined);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(_p, null);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(_p, _nonBigInteger);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(_p, _tooSmallOrder);
				}).to.throw();

				expect(function () {
					_mathService.newQuadraticResidueGroup(_p, _tooLargeOrder);
				}).to.throw();
			});

		it('throw an error when checking if a group matches the configured policy',
			function () {
				expect(function () {
					const policy = cryptoPolicy.newInstance();
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.ZP_2048_224;

					const mathServiceZP224 = mathematical.newService({policy: policy});

					mathServiceZP224.checkGroupMatchesPolicy(_group);
				}).to.throw('Expected Q to have a length of 224 for group type ZP_2048_224; Found 2047');

				expect(function () {
					const policy = cryptoPolicy.newInstance();
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;

					const mathServiceZP256 = mathematical.newService({policy: policy});

					mathServiceZP256.checkGroupMatchesPolicy(_group);
				}).to.throw('Expected Q to have a length of 256 for group type ZP_2048_256; Found 2047');

				expect(function () {
					const policy = cryptoPolicy.newInstance();
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.QR_3072;

					const mathServiceQR3072 = mathematical.newService({policy: policy});

					mathServiceQR3072.checkGroupMatchesPolicy(_group);
				}).to.throw('Expected Q to have a length of 3071 for group type QR_3072; Found 2047');

				expect(function () {
					const policy = cryptoPolicy.newInstance();
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.QR_2048;

					const mathServiceQR2048 = mathematical.newService({policy: policy});

					mathServiceQR2048.checkGroupMatchesPolicy(_anotherGroup);
				}).to.throw('Expected Q to have a length of 2047 for group type QR_2048; Found 256');

				expect(function () {
					const policy = cryptoPolicy.newInstance();
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.QR_2048;

					const mathServiceQR2048 = mathematical.newService({policy: policy});

					mathServiceQR2048.checkGroupMatchesPolicy(_group);
				}).to.throw('Expected P to equal 2Q + 1 for group type QR_2048');

				expect(function () {
					const policy = cryptoPolicy.newInstance();
					policy.mathematical.groups.type = null;

					const mathServiceZP2047 = mathematical.newService({policy: policy});

					mathServiceZP2047.checkGroupMatchesPolicy(_group);
				}).to.throw('Invalid group type; Found null');

				expect(function () {
					const policy = cryptoPolicy.newInstance();
					policy.mathematical.groups.type = cryptoPolicy.options.mathematical.groups.type.QR_2048;

					const mathServiceZP2047 = mathematical.newService({policy: policy});

					mathServiceZP2047.checkGroupArrayMatchesPolicy(_mixedElements);
				}).to.throw('Expected P to equal 2Q + 1 for group type QR_2048');
			});


		describe('create a new ZpSubgroup object that should ..', function () {
			it('throw an error when checking for group membership, using invalid input data',
				function () {
					expect(function () {
						_group.isGroupMember();
					}).to.throw();

					expect(function () {
						_group.isGroupMember(undefined);
					}).to.throw();

					expect(function () {
						_group.isGroupMember(null);
					}).to.throw();

					expect(function () {
						_group.isGroupMember(_nonObject);
					}).to.throw();

					expect(function () {
						_group.isGroupMember(_emptyObject);
					}).to.throw();
				});

			it('throw an error when checking for equality with another ZpSubgroup object, using invalid input data',
				function () {
					expect(function () {
						_group.equals();
					}).to.throw();

					expect(function () {
						_group.equals(undefined);
					}).to.throw();

					expect(function () {
						_group.equals(null);
					}).to.throw();

					expect(function () {
						_group.equals(_nonObject);
					}).to.throw();

					expect(function () {
						_group.equals(_emptyObject);
					}).to.throw();
				});
		});

		describe('create a new ZpGroupElement object that should ..', function () {
			it('throw an error when multiplying itself with another ZpGroupElement object, using invalid input data',
				function () {
					expect(function () {
						_element.multiply();
					}).to.throw();

					expect(function () {
						_element.multiply(undefined);
					}).to.throw();

					expect(function () {
						_element.multiply(null);
					}).to.throw();

					expect(function () {
						_element.multiply(_nonObject);
					}).to.throw();

					expect(function () {
						_element.multiply(_emptyObject);
					}).to.throw();

					expect(function () {
						_element.multiply(_elementFromAnotherGroup);
					}).to.throw();
				});

			it('throw an error when exponentiating itself with an Exponent object, using invalid input data',
				function () {
					expect(function () {
						_element.exponentiate();
					}).to.throw();

					expect(function () {
						_element.exponentiate(undefined);
					}).to.throw();

					expect(function () {
						_element.exponentiate(null);
					}).to.throw();

					expect(function () {
						_element.exponentiate(_nonObject);
					}).to.throw();

					expect(function () {
						_element.exponentiate(_emptyObject);
					}).to.throw();

					expect(function () {
						_element.exponentiate(_exponentFromAnotherGroup);
					}).to.throw();
				});

			it('throw an error when comparing itself to another ZpGroupElement object, using invalid input data',
				function () {
					expect(function () {
						_element.equals();
					}).to.throw();

					expect(function () {
						_element.equals(undefined);
					}).to.throw();

					expect(function () {
						_element.equals(null);
					}).to.throw();

					expect(function () {
						_element.equals(_nonObject);
					}).to.throw();

					expect(function () {
						_element.equals(_emptyObject);
					}).to.throw();
				});
		});

		describe('create a new Exponent object that should ..', function () {
			it('throw an error when adding itself to another Exponent object, using invalid input data',
				function () {
					expect(function () {
						_exponent.add();
					}).to.throw();

					expect(function () {
						_exponent.add(undefined);
					}).to.throw();

					expect(function () {
						_exponent.add(null);
					}).to.throw();

					expect(function () {
						_exponent.add(_nonObject);
					}).to.throw();

					expect(function () {
						_exponent.add(_emptyObject);
					}).to.throw();

					expect(function () {
						_exponent.add(_exponentFromAnotherGroup);
					}).to.throw();
				});

			it('throw an error when subtracting another Exponent object from itself, using invalid input data',
				function () {
					expect(function () {
						_exponent.subtract();
					}).to.throw();

					expect(function () {
						_exponent.subtract(undefined);
					}).to.throw();

					expect(function () {
						_exponent.subtract(null);
					}).to.throw();

					expect(function () {
						_exponent.subtract(_nonObject);
					}).to.throw();

					expect(function () {
						_exponent.subtract(_emptyObject);
					}).to.throw();

					expect(function () {
						_exponent.add(_exponentFromAnotherGroup);
					}).to.throw();
				});

			it('throw an error when multiplying itself with another Exponent object, using invalid input data',
				function () {
					expect(function () {
						_exponent.multiply();
					}).to.throw();

					expect(function () {
						_exponent.multiply(undefined);
					}).to.throw();

					expect(function () {
						_exponent.multiply(null);
					}).to.throw();

					expect(function () {
						_exponent.multiply(_nonObject);
					}).to.throw();

					expect(function () {
						_exponent.multiply(_emptyObject);
					}).to.throw();

					expect(function () {
						_exponent.add(_exponentFromAnotherGroup);
					}).to.throw();
				});

			it('throw an error when comparing itself to another Exponent object, using invalid input data',
				function () {
					expect(function () {
						_exponent.equals();
					}).to.throw();

					expect(function () {
						_exponent.equals(undefined);
					}).to.throw();

					expect(function () {
						_exponent.equals(null);
					}).to.throw();

					expect(function () {
						_exponent.equals(_nonObject);
					}).to.throw();

					expect(function () {
						_exponent.equals(_emptyObject);
					}).to.throw();
				});
		});

		describe(
			'create a new MathematicalArrayCompressor object that should ..',
			function () {
				it('throw an error when compressing an array of Zp group elements, using invalid input data',
					function () {
						expect(function () {
							_mathArrayCompressor.compressZpGroupElements();
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressZpGroupElements(undefined);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressZpGroupElements(null);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressZpGroupElements(_nonArray);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressZpGroupElements(_nonObjectArray);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressZpGroupElements(_emptyObjectArray);
						}).to.throw();
					});

				it('throw an error when compressing an array of exponents, using invalid input data',
					function () {
						expect(function () {
							_mathArrayCompressor.compressExponents();
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressExponents(undefined);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressExponents(null);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressExponents(_nonArray);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressExponents(_nonObjectArray);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressExponents(_emptyObjectArray);
						}).to.throw();
					});

				it('throw an error when compressing the trailing elements of an array of Zp group elements, using invalid input data',
					function () {
						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_elements);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(undefined, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(null, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_nonArray, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_nonObjectArray, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_emptyObjectArray, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_elements, undefined);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_elements, null);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_elements, _nonNumber);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingZpGroupElements(_elements, _nonPositiveNumber);
						}).to.throw();
					});

				it('throw an error when compressing the trailing exponents of an array of exponents, using invalid input data',
					function () {
						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(_exponents);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(undefined, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(null, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(_nonArray, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(_nonObjectArray, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(_emptyObjectArray, 2);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(_exponents, undefined);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(
								_exponents, null);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(_exponents, _nonNumber);
						}).to.throw();

						expect(function () {
							_mathArrayCompressor.compressTrailingExponents(_exponents, _nonPositiveNumber);
						}).to.throw();
					});
			});

		describe(
			'create a new MathematicalRandomGenerator object that should ..',
			function () {
				it('throw an error when generating a random exponent, using invalid input data',
					function () {
						expect(function () {
							_mathRandomGenerator.nextExponent();
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextExponent(undefined);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextExponent(null);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextExponent(_nonObject);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextExponent(_emptyObject);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextExponent(_group, {secureRandomGenerator: _nonObject});
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextExponent(_group, {secureRandomGenerator: _emptyObject});
						}).to.throw();
					});

				it('throw an error when generating a random Zp group element, using invalid input data',
					function () {
						expect(function () {
							_mathRandomGenerator.nextZpGroupElement();
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextZpGroupElement(undefined);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextZpGroupElement(null);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextZpGroupElement(_nonObject);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextZpGroupElement(_emptyObject);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextZpGroupElement(_group, {secureRandomGenerator: _nonObject});
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextZpGroupElement(_group, {secureRandomGenerator: _emptyObject});
						}).to.throw();
					});

				it('throw an error when generating a random quadratic residue group, using invalid input data',
					function () {
						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(16);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(undefined, _minCertaintyLevel);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(null, _minCertaintyLevel);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(_nonNumber, _minCertaintyLevel);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(_nonPositiveNumber, _minCertaintyLevel);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(_tooSmallModulusBitLength, _minCertaintyLevel);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(16, undefined);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(16, null);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(16, _nonNumber);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(16, _nonPositiveNumber);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(16, _tooLowCertaintyLevel);
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(
								16, _minCertaintyLevel,
								{secureRandomGenerator: _nonObject});
						}).to.throw();

						expect(function () {
							_mathRandomGenerator.nextQuadraticResidueGroup(
								16, _minCertaintyLevel,
								{secureRandomGenerator: _emptyObject});
						}).to.throw();
					});
			});

		describe('create a new MathematicalGroupHandler object that should ..', function () {
			it('throw an error when exponentiating an array of ZpGroupElement objects, using invalid input data',
				function () {
					expect(function () {
						_mathGroupHandler.exponentiateElements(_group);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(undefined, _elements, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(null, _elements, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_nonObject, _elements, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_emptyObject, _elements, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, undefined, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, null, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _nonArray, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _nonObjectArray, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _emptyObjectArray, _exponent);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _elements, undefined);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _elements, _nonObject);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _elements, _emptyObject);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _elements, _exponentFromAnotherGroup);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _elements, _exponent, null);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.exponentiateElements(_group, _elements, _exponent, _nonBoolean);
					}).to.throw();
				});

			it('throw an error when dividing one array of ZpGroupElement objects with another, using invalid input data',
				function () {
					expect(function () {
						_mathGroupHandler.divideElements(_elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(undefined, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(null, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_nonArray, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_nonObjectArray, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_emptyObjectArray, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_elements, undefined);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_elements, null);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_elements, _nonArray);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_elements, _nonObjectArray);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_elements, _emptyObjectArray);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.divideElements(_elements, [_element]);
					}).to.throw();
				});

			it('throw an error when checking for group membership, using invalid input data',
				function () {
					expect(function () {
						_mathGroupHandler.checkGroupMembership(undefined, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(null, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(_nonObject, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(_emptyObject, _elements);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(_group, undefined);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(_group, null);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(_group, _nonArray);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(_group, _nonObjectArray);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.checkGroupMembership(_group, _emptyObjectArray);
					}).to.throw();
				});

			it('throw an error when extracting Zp group elements from an array of BigIntegers, using invalid input data',
				function () {
					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _multiGroupElementValues);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(undefined, _multiGroupElementValues, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(null, _multiGroupElementValues, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_nonObject, _multiGroupElementValues, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_emptyObject, _multiGroupElementValues, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, undefined, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, null, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _nonArray, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _nonObjectArray, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _emptyObjectArray, 2);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _multiGroupElementValues, undefined);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _multiGroupElementValues, null);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _multiGroupElementValues, _nonNumber);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _multiGroupElementValues, _nonPositiveNumber);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.extractGroupMembers(_group, _multiGroupElementValues, _tooLargeNumMembersRequired);
					}).to.throw();
				});

			it('throw an error when finding the smallest generator for a given Zp subgroup, using invalid input data',
				function () {
					expect(function () {
						_mathGroupHandler.findMinGenerator(_p);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(undefined, _q);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(null, _q);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(_nonBigInteger, _q);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(_tooSmallModulus, _q);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(_p, undefined);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(_p, null);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(_p, _nonBigInteger);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(_p, _tooSmallOrder);
					}).to.throw();

					expect(function () {
						_mathGroupHandler.findMinGenerator(_p, _tooLargeOrder);
					}).to.throw();
				});
		});

		it('throw an error when validator receives invalid values',
			function () {
				const label = 'Invalid value';
				expect(function () {
					validator.checkIsJsonString('', label);
				}).to.throw('Invalid value is empty.');

				expect(function () {
					validator.checkIsNonEmptyArray([], label);
				}).to.throw('Invalid value is empty.');

				expect(function () {
					validator.checkZpGroupElement(_element, label, _anotherGroup);
				}).to.throw('Expected modulus p of Invalid value to equal modulus of Zp subgroup provided');

				expect(function () {
					const invalidGroup = _mathService.newZpSubgroup(_p, _anotherQ, _g)
					validator.checkZpGroupElement(_element, label, invalidGroup);
				}).to.throw('Expected order q of Invalid value to equal order of Zp subgroup provided');
			});
	});
});
