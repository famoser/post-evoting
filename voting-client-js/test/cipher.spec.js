/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const forge = require('node-forge');
/* global OV */

const mathematical = require('cryptolib-js/src/mathematical/index');
const TD = require('./mocks/testdata.json');

describe('Cipher api', function () {
	'use strict';

	const mathematicalService = mathematical.newService();

	it('should cipher an array of elements', function () {
		const encParms = OV.parseEncryptionParams(TD.authResponse);
		const serializedEncrypterValues = OV.precomputeEncrypterValues(encParms);
		const encrypterValues = OV.deserializeEncrypterValues(
			serializedEncrypterValues,
		);

		const keyValue = forge.jsbn.BigInteger.ONE;
		const rho = mathematicalService.newZpGroupElement(
			encParms.p,
			encParms.q,
			keyValue,
		);
		const ciphered = OV.encryptRho(
			rho,
			encParms,
			encrypterValues,
		);

		expect(ciphered).toBeDefined();
	});
});
