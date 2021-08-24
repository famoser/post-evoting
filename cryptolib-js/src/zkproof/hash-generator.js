/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const codec = require('../codec');
require('./array');

module.exports = HashGenerator;

/**
 * @class HashGenerator
 * @classdesc Encapsulates a hash generator needed for zero-knowledge proof
 *            generation and verification.
 * @private
 * @param {MessageDigestService}
 *            messageDigestService The message digest service to use.
 */
function HashGenerator(messageDigestService) {
	const _digester = messageDigestService.newDigester();

	/**
	 * Generates the hash.
	 *
	 * @function generate
	 * @memberof HashGenerator
	 * @param {ZpGroupElement[]}
	 *            publicValues The public values.
	 * @param {ZpGroupElement[]}
	 *            generatedValues The generated values.
	 * @param {Uint8Array|string}
	 *            data Auxiliary data.
	 * @returns {Uint8Array} The generated hash.
	 */
	this.generate = function (publicValues, generatedValues, data) {
		const listOfValues = [];
		for (let i = 0; i < publicValues.length; i++) {
			listOfValues.push(publicValues[i].value);
		}
		for (let j = 0; j < generatedValues.length; j++) {
			listOfValues.push(generatedValues[j].value);
		}
		listOfValues.push(data);
		_digester.update(valuesToString(listOfValues));
		return _digester.digest();
	};

	/**
	 * Generates the hash.
	 *
	 * @function valuesToString
	 * @memberof HashGenerator
	 * @param {forge.jsbn.BigInteger[]|String[]} values
	 *            Array of BigIntegers and/or strings (mixed).
	 * @returns {Uint8Array} The concatenated string to be hashed, that is, each
	 *     element encoded in base64 format separated by ':'.
	 */
	function valuesToString(values) {
		let str = '';
		for (let i = 0; i < values.length; i++) {
			str += codec.base64Encode(values[i].toString());
			if (i < values.length - 1) {
				str += ':';
			}
		}
		return str;
	}
}
