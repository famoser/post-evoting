/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ElGamalPrivateKey = require('./private-key');
const validator = require('../input-validator');

module.exports = ElGamalDecrypter;

/**
 * @class ElGamalDecrypter
 * @classdesc The ElGamal decrypter API. To instantiate this object, use the
 *            method {@link ElGamalCryptographyService.newDecrypter}.
 * @hideconstructor
 * @param {MathematicalService}
 *            mathService The mathematical service to use.
 */
function ElGamalDecrypter(mathService) {
	const _mathArrayCompressor = mathService.newArrayCompressor();

	let _privateKey;

	/**
	 * Initializes the ElGamal decrypter with the provided ElGamal private key.
	 *
	 * @function init
	 * @memberof ElGamalDecrypter
	 * @param {ElGamalPrivateKey}
	 *            privateKey The ElGamal private key with which to initialize
	 *            the ElGamal decrypter.
	 * @returns {ElGamalDecrypter} A reference to this object, to facilitate
	 *          method chaining.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.init = function (privateKey) {
		validator.checkElGamalPrivateKey(
			privateKey,
			'ElGamal private key with which to initialize ElGamal decrypter');
		mathService.checkGroupMatchesPolicy(privateKey.group);

		_privateKey = privateKey;

		return this;
	};

	/**
	 * ElGamal decrypts some ElGamal encrypted Zp group elements. Before using
	 * this method, the decrypter must have been initialized with an ElGamal
	 * private key, via the method {@link ElGamalDecrypter.init}.
	 * <p>
	 * The number of phi elements that comprise the encryption must be less than
	 * or equal to the number of exponents in the ElGamal private key that was
	 * used to initialize the ElGamal decrypter.
	 *
	 * @function decrypt
	 * @memberof ElGamalDecrypter
	 * @param {ElGamalEncryptedElements}
	 *            encryptedElements The ElGamal encrypted Zp group elements.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {boolean}
	 *            [options.confirmMembership=false] If <true>true</false>, then
	 *            each of the encryption elements will be checked for membership
	 *            in the Zp subgroup associated with the private key.
	 *            <b>WARNING:</b> This operation is computationally costly and
	 *            increases linearly with the number of encrypted elements.
	 * @returns {ZpGroupElement[]} The decrypted Zp group elements.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.decrypt = function (encryptedElements, options) {
		if (typeof _privateKey === 'undefined') {
			throw new Error(
				'Could not ElGamal decrypt; Decrypter has not been initialized with any ElGamal private key');
		}

		options = options || {};

		checkDecryptionData(_privateKey, encryptedElements, options);

		const gamma = encryptedElements.gamma;
		const phis = encryptedElements.phis;

		const checkMembership = options.checkMembership || false;
		if (checkMembership) {
			const group = _privateKey.group;
			for (let i = 0; i < phis.length; i++) {
				if (!(group.isGroupMember(phis[i]))) {
					throw new Error(
						'Found phi element with value: ' + phis[i].value +
						' that is not member of Zp subgroup associated with private key.');
				}
			}
		}

		const compressedPrivateKey = compressPrivateKey(_privateKey, phis.length);
		const privateKeyExponents = compressedPrivateKey.exponents;

		const decryptedElements = [];
		for (let j = 0; j < phis.length; j++) {
			decryptedElements.push(gamma.exponentiate(privateKeyExponents[j].negate())
				.multiply(phis[j]));
		}

		return decryptedElements;
	};

	function checkDecryptionData(privateKey, encryptedElements, options) {
		validator.checkElGamalEncryptedElements(
			encryptedElements, 'ElGamal encrypted elements to decrypt');
		mathService.checkGroupMatchesPolicy(encryptedElements.gamma);
		mathService.checkGroupArrayMatchesPolicy(encryptedElements.phis);

		const numPrivateKeyExponents = privateKey.exponents.length;
		const numPhis = encryptedElements.phis.length;
		if (numPrivateKeyExponents < numPhis) {
			throw new Error(
				'Expected number of phi elements to decrypt to be less than or equal to number of private key exponents: ' +
				numPrivateKeyExponents + ' ; Found: ' + numPhis);
		}

		const confirmMembership = options.confirmMembership;
		if (typeof confirmMembership !== 'undefined') {
			validator.checkIsType(
				confirmMembership, 'boolean',
				'"Confirm group membership" flag for decryption');
		}
	}

	function compressPrivateKey(privateKey, numPhis) {
		const group = privateKey.group;
		const exponents = privateKey.exponents;

		if (exponents.length === numPhis) {
			return privateKey;
		} else {
			const compressedExponents =
				_mathArrayCompressor.compressTrailingExponents(exponents, numPhis);

			return new ElGamalPrivateKey(group, compressedExponents);
		}
	}
}
