/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ElGamalEncryptedElements = require('./encrypted-elements');
const validator = require('../input-validator');
const forge = require('node-forge');

const BigInteger = forge.jsbn.BigInteger;

module.exports = ElGamalEncrypter;

/**
 * @class ElGamalEncrypter
 * @classdesc The ElGamal encrypter API. To instantiate this object, use the
 *            method {@link ElGamalCryptographyService.newEncrypter}.
 * @hideconstructor
 * @param {MathematicalService}
 *            mathService The mathematical service to use.
 */
function ElGamalEncrypter(mathService) {
	const _mathService = mathService;
	const _mathArrayCompressor = _mathService.newArrayCompressor();
	const _mathRandomGenerator = _mathService.newRandomGenerator();

	let _publicKey;
	let _secureRandomGenerator;

	/**
	 * Initializes the ElGamal encrypter with the provided ElGamal public key.
	 *
	 * @function init
	 * @memberof ElGamalEncrypter
	 * @param {ElGamalPublicKey}
	 *            publicKey The ElGamal public key with which to initialize the
	 *            ElGamal encrypter.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {SecureRandomGenerator}
	 *            [options.secureRandomGenerator=Created internally] The secure
	 *            random generator to use.
	 * @returns {ElGamalEncrypter} A reference to this object, to facilitate
	 *          method chaining.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.init = function (publicKey, options) {
		validator.checkElGamalPublicKey(
			publicKey,
			'ElGamal public key with which to initialize ElGamal encrypter');
		_mathService.checkGroupMatchesPolicy(publicKey.group);
		_mathService.checkGroupArrayMatchesPolicy(publicKey.elements);

		_publicKey = publicKey;

		options = options || {};
		_secureRandomGenerator = options.secureRandomGenerator;

		return this;
	};

	/**
	 * ElGamal encrypts some Zp group elements. Before using this method, the
	 * encrypter must have been initialized with an ElGamal public key, via the
	 * method {@link ElGamalEncrypter.init}.
	 * <p>
	 * The number of elements to encrypt must be less than or equal to the
	 * number of elements in the ElGamal public key that was used to initialize
	 * the ElGamal encrypter.
	 *
	 * @function encrypt
	 * @memberof ElGamalEncrypter
	 * @param {ZpGroupElement[]|string[]}
	 *            elements The Zp group elements to encrypt. The Zp group
	 *            elements may also be provided in the form of BigInteger string
	 *            values.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {ElGamalEncryptedElements}
	 *            [options.preComputation=Generated internally] A
	 *            pre-computation of the ElGamal encryption.
	 * @param {boolean}
	 *            [options.useShortExponent=false] If <code>true</code>, then
	 *            a <code>short</code> secret exponent will be generated for
	 *            the pre-computation process.
	 * @param {boolean}
	 *            [options.saveSecret=false] If <code>true</code>, the secret
	 *            exponent will be saved and returned, along with the encrypted
	 *            elements. Required when the secret exponent
     *            is needed later for zero-knowledge proof generation.
	 * @returns {ElGamalEncryptedElements} The encrypted Zp group elements.
	 * @throws {Error}
	 *             If the input data validation fails, the encrypter was not
	 *             initialized or an attempt is made to ElGamal encrypt a
	 *             non-quadratic residue Zp subgroup, using a short exponent.
	 */
	this.encrypt = function (elements, options) {
		if (typeof _publicKey === 'undefined') {
			throw new Error(
				'Could not ElGamal encrypt; Encrypter has not been initialized with any ElGamal public key');
		}

		options = options || {};

		checkEncryptionData(_publicKey, elements, options);

		if (typeof elements[0] === 'string') {
			elements = getGroupElementsFromStrings(_publicKey.group, elements);
		}

		let preComputation;
		if (typeof options.preComputation !== 'undefined') {
			preComputation = options.preComputation;
		} else {
			preComputation = this.preCompute({
				useShortExponent: options.useShortExponent,
				saveSecret: options.saveSecret
			});
		}

		const compressedPreComputation =
			compressPreComputation(preComputation, elements.length);

		const gamma = compressedPreComputation.gamma;
		const preComputedPhis = compressedPreComputation.phis;
		const phis = [];
		for (let i = 0; i < preComputedPhis.length; i++) {
			phis.push(elements[i].multiply(preComputedPhis[i]));
		}

		const saveSecret = options.saveSecret || false;
		if (!saveSecret) {
			return new ElGamalEncryptedElements(gamma, phis);
		} else {
			return new ElGamalEncryptedElements(gamma, phis, preComputation.secret);
		}
	};

	/**
	 * Computes the part of the ElGamal encryption that can be performed before
	 * knowing the Zp group elements that are to be encrypted.
	 *
	 * @function preCompute
	 * @memberof ElGamalEncrypter
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {boolean}
	 *            [options.useShortExponent=false] If <code>true</code>, then
	 *            a <code>short</code> secret exponent will be generated for
	 *            the pre-computation process.
	 * @param {boolean}
	 *            [options.saveSecret=false] If <code>true</code>, the secret
	 *            exponent will be saved and returned, along with the encrypted
	 *            elements. Required when the secret exponent
     *            is needed later for zero-knowledge proof generation.
	 * @returns {ElGamalEncryptedElements} The encryption pre-computation.
	 * @throws {Error}
	 *             If the input data validation fails or an attempt is made to
	 *             ElGamal pre-compute a non-quadratic residue Zp subgroup,
	 *             using a short exponent.
	 */
	this.preCompute = function (options) {
		if (typeof _publicKey === 'undefined') {
			throw new Error(
				'Could not ElGamal pre-compute; Encrypter has not been initialized with any ElGamal public key');
		}

		options = options || {};

		checkPreComputationData(_publicKey, options);

		const group = _publicKey.group;
		const publicKeyElements = _publicKey.elements;

		const useShortExponent = options.useShortExponent || false;
		const secret = _mathRandomGenerator.nextExponent(group, {
			secureRandomGenerator: _secureRandomGenerator,
			useShortExponent: useShortExponent
		});

		const gamma = group.generator.exponentiate(secret);
		const phis = [];
		for (let i = 0; i < publicKeyElements.length; i++) {
			phis.push(publicKeyElements[i].exponentiate(secret));
		}

		const saveSecret = options.saveSecret || false;
		if (!saveSecret) {
			return new ElGamalEncryptedElements(gamma, phis);
		} else {
			return new ElGamalEncryptedElements(gamma, phis, secret);
		}
	};

	function checkEncryptionData(publicKey, elements, options) {
		validator.checkIsArray(elements, 'Zp group elements to encrypt');
		if (typeof elements[0] !== 'string') {
			validator.checkZpGroupElements(
				elements, 'Zp group elements to encrypt', publicKey.group);
			_mathService.checkGroupArrayMatchesPolicy(elements);
		} else {
			validator.checkIsStringArray(
				elements, 'Zp group element value strings to encrypt');
		}

		const numElements = elements.length;
		const numPublicKeyElements = publicKey.elements.length;
		if (numElements > numPublicKeyElements) {
			throw new Error(
				'Expected number of Zp group elements to encrypt to be less than or equal to number of public key elements: ' +
				numPublicKeyElements + ' ; Found: ' + numElements);
		}

		const preComputation = options.preComputation;
		if (typeof preComputation !== 'undefined') {
			validator.checkElGamalEncryptedElements(
				preComputation, 'ElGamal encryption pre-computation');

			const numPreComputedPhis = preComputation.phis.length;
			if (numElements > numPreComputedPhis) {
				throw new Error(
					'Expected number of elements to encrypt to be less than or equal to number of pre-computed phi elements: ' +
					numPreComputedPhis + ' ; Found: ' + numElements);
			}
		}
	}

	function checkPreComputationData(publicKey, options) {
		const useShortExponent = options.useShortExponent;
		if (typeof useShortExponent !== 'undefined') {
			validator.checkIsType(
				useShortExponent, 'boolean',
				'"Use short exponent" flag for pre-computation');

			if (useShortExponent && !publicKey.group.isQuadraticResidueGroup()) {
				throw new Error(
					'Attempt to ElGamal pre-compute using short exponent for Zp subgroup that is not of type quadratic residue.');
			}
		}

		const saveSecret = options.saveSecret;
		if (typeof saveSecret !== 'undefined') {
			validator.checkIsType(
				saveSecret, 'boolean', '"Save secret" flag for pre-computation');
		}
	}

	function getGroupElementsFromStrings(group, elementValueStrings) {
		const p = group.p;
		const q = group.q;

		const groupElements = [];
		let groupElement;
		for (let i = 0; i < elementValueStrings.length; i++) {
			groupElement = _mathService.newZpGroupElement(
				p, q, new BigInteger(elementValueStrings[i]));
			groupElements.push(groupElement);
		}

		return groupElements;
	}

	function compressPreComputation(preComputation, numElements) {
		const preComputedPhis = preComputation.phis;

		if (preComputedPhis.length === numElements) {
			return preComputation;
		} else {
			const compressedPreComputedPhis =
				_mathArrayCompressor.compressTrailingZpGroupElements(
					preComputedPhis, numElements);

			return new ElGamalEncryptedElements(
				preComputation.gamma, compressedPreComputedPhis);
		}
	}
}
