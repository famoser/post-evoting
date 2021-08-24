/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ZpSubgroup = require('./zp-subgroup');
const Exponent = require('./exponent');
const MathematicalGroupHandler = require('./group-handler');
const constants = require('./constants');
const validator = require('../input-validator');
const forge = require('node-forge');

const BigInteger = forge.jsbn.BigInteger;

module.exports = MathematicalRandomGenerator;

/**
 * @class MathematicalRandomGenerator
 * @classdesc The mathematical random generator API. To instantiate this object,
 *            use the method {@link MathematicalService.newRandomGenerator}.
 * @hideconstructor
 * @param {SeureRandomService}
 *            secureRandomService The secure random service to use.
 */
function MathematicalRandomGenerator(secureRandomService) {
	const _randomGenerator = secureRandomService.newRandomGenerator();
	const _mathGroupHandler = new MathematicalGroupHandler();

	/**
	 * Generates a random Zp group element belonging to the Zp subgroup provided
	 * as input.
	 *
	 * @function nextZpGroupElement
	 * @memberof MathematicalRandomGenerator
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which the Zp group element belongs.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {SecureRandomGenerator}
	 *            [options.secureRandomGenerator=Internal generator used] The
	 *            secure random generator to use.
	 * @param {boolean}
	 *            [options.useShortExponent=false] If <code>true</code>, then
	 *            a short exponent is to be generated for the exponentiation
	 *            used to generate the Zp group element.
	 * @returns {ZpGroupElement} The generated Zp group element.
	 * @throws {Error}
	 *             If the input validation fails.
	 */
	this.nextZpGroupElement = function (group, options) {
		validator.checkIsObjectWithProperties(
			group, 'Zp subgroup to which randomly generated element is to belong');

		const exponent = this.nextExponent(group, options);

		return group.generator.exponentiate(exponent);
	};

	/**
	 * Generates a random exponent associated with the Zp subgroup provided as
	 * input. The value of the exponent will be within the range <code>[0,
	 * q-1]</code>.
	 *
	 * @function nextExponent
	 * @memberof MathematicalRandomGenerator
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which the exponent is to be
	 *            associated.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {SecureRandomGenerator}
	 *            [options.secureRandomGenerator=Internal generator is used] The
	 *            secure random generator to use.
	 * @param {boolean}
	 *            [options.useShortExponent=false] If <code>true</code>, then
	 *            a short exponent is to be generated.
	 * @returns {Exponent} The generated exponent.
	 * @throws {Error}
	 *             If the input validation fails.
	 */
	this.nextExponent = function (group, options) {
		validator.checkIsObjectWithProperties(
			group,
			'Zp subgroup to which randomly generated exponent is to be associated');

		options = options || {};

		const randomGenerator = options.secureRandomGenerator || _randomGenerator;
		validator.checkIsObjectWithProperties(
			randomGenerator,
			'Random BigInteger generator for random exponent generation');

		const useShortExponent = options.useShortExponent || false;

		const q = group.q;
		const qBitLength = q.bitLength();
		let randomExponentBitLength;
		if (useShortExponent) {
			if (qBitLength < constants.SHORT_EXPONENT_BIT_LENGTH) {
				throw new Error(
					'Zp subgroup order bit length must be greater than or equal to short exponent bit length : ' +
					constants.SHORT_EXPONENT_BIT_LENGTH + '; Found ' + qBitLength);
			}
			randomExponentBitLength = constants.SHORT_EXPONENT_BIT_LENGTH;
		} else {
			randomExponentBitLength = qBitLength;
		}

		let randomExponentValue;
		let randomExponentFound = false;
		while (!randomExponentFound) {
			randomExponentValue =
				randomGenerator.nextBigInteger(randomExponentBitLength);
			if (randomExponentValue.compareTo(q) < 0) {
				randomExponentFound = true;
			}
		}

		return new Exponent(q, randomExponentValue);
	};

	/**
	 * Generates a random quadratic residue Zp subgroup, that has the specified
	 * bit length for its modulus <code>p</code> and the specified certainty
	 * of having its modulus and the order <code>q</code> being prime.
	 * <p>
	 * This method generates a Zp subgroup that satisfies the condition:
	 * <p>
	 * <code>p =
	 * (q * 2) + 1</code>
	 * <p>
	 * <b>Note:</b> The minimum bit length of the modulus <code>p</code> that
	 * is permitted by this method is 2. If a bit length less than 2 is
	 * requested, then an Error will be thrown.
	 *
	 * @function nextQuadraticResidueGroup
	 * @memberof MathematicalRandomGenerator
	 * @param {number}
	 *            pBitLength The modulus bit length of the Zp subgroup to
	 *            generate.
	 * @param {number}
	 *            certainty The level of certainty that the order <code>q</code>
	 *            of the generated group is prime.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {SecureRandomGenerator}
	 *            [options.secureRandomGenerator=Internal generator used] The
	 *            secure random generator to use.
	 * @returns {ZpSubgroup} The generated quadratic residue group.
	 * @throws {Error}
	 *             If the input validation fails.
	 */
	this.nextQuadraticResidueGroup = function (pBitLength, certainty, options) {
		checkQrGroupGenerationData(pBitLength, certainty);

		options = options || {};

		const randomGenerator = options.secureRandomGenerator || _randomGenerator;
		validator.checkIsObjectWithProperties(
			randomGenerator,
			'Random bytes generator for random quadratic residue Zp subgroup generation');

		let p, q;
		let randomGroupFound = false;

		const primeOptions = {prng: randomGenerator};

		const callback = function (err, num) {
			if (err) {
				throw new Error('Error while generating prime number; ' + err);
			}
			p = num;
			q = num.subtract(BigInteger.ONE).divide(new BigInteger('2'));
		};

		while (!randomGroupFound) {
			forge.prime.generateProbablePrime(pBitLength, primeOptions, callback);

			if (q.isProbablePrime(certainty)) {
				randomGroupFound = true;
			}
		}

		const g = _mathGroupHandler.findMinGenerator(p, q);

		return new ZpSubgroup(p, q, g);
	};

	function checkQrGroupGenerationData(pBitLength, certainty) {
		validator.checkIsPositiveNumber(pBitLength);
		validator.checkIsPositiveNumber(certainty);

		if (pBitLength < 2) {
			throw new Error(
				'Bit length: ' + pBitLength +
				' of modulus p provided as input for generating quadratic residue group is less than minimum required value of 2.');
		}

		let _minimumPrimeCertaintyLevel;

		if (pBitLength <= 1024) {
			_minimumPrimeCertaintyLevel = 80;
		} else if (pBitLength <= 2048) {
			_minimumPrimeCertaintyLevel = 112;
		} else {
			_minimumPrimeCertaintyLevel = 128;
		}

		if (certainty < _minimumPrimeCertaintyLevel) {
			throw new Error(
				'Certainty level: ' + certainty +
				' provided as input for generating quadratic residue group is less than minimum required value of ' +
				_minimumPrimeCertaintyLevel);
		}
	}
}
