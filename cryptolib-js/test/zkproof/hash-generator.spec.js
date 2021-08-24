/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true, mocha: true, expr:true */
'use strict';

const { expect } = require('chai');

const cryptoPolicy = require('../../src/cryptopolicy');
const messageDigest = require('../../src/messagedigest');
const mathematical = require('../../src/mathematical');
const codec = require('../../src/codec');
const forge = require('node-forge');
const HashGenerator = require('../../src/zkproof/hash-generator');

describe('The hash generator should be able to ', function () {
	it('should concatenate and hash bigintegers and strings', function () {
		const policy = cryptoPolicy.newInstance();
		policy.proofs.messageDigest.algorithm =
			cryptoPolicy.options.proofs.messageDigest.algorithm.SHA256;
		const messageDigestService = messageDigest.newService({policy: policy});
		const mathematicalService = mathematical.newService({policy: policy});
		const p = new forge.jsbn.BigInteger('23');
		const q = new forge.jsbn.BigInteger('11');

		const element2 = mathematicalService.newZpGroupElement(
			p, q, new forge.jsbn.BigInteger('2'));
		const element3 = mathematicalService.newZpGroupElement(
			p, q, new forge.jsbn.BigInteger('3'));
		const element4 = mathematicalService.newZpGroupElement(
			p, q, new forge.jsbn.BigInteger('4'));
		const publicValues = [].concat(element2, element3, element4);
		const generatedValues = [].concat(element2, element3, element4);
		const data = 'TestData';

		const hashGenerator = new HashGenerator(messageDigestService);

		const hashBytes = hashGenerator.generate(publicValues, generatedValues, data);

		const hashByteArray = Array.apply([], hashBytes);
		hashByteArray.unshift(0);
		const result = codec.base64Encode(hashBytes);
		expect(result).to.equal('/aAK9O/vJtKXFMAqiUajsR0ox5VfS0j2WhsXi0iT0FI=');
	});
});
