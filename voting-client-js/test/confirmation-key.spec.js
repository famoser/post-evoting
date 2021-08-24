/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const forge = require('node-forge');
/* global OV */
/* jshint maxlen: 6666 */

describe('Vote confirmation message api', function () {
	'use strict';

	const TD = require('./mocks/testdata.json');

	it('should generate a confirmation key', function () {
		const encParms = OV.parseEncryptionParams(TD.authResponse);
		const exponent = new forge.jsbn.BigInteger('34987025184313239');

		const confirmationKey = OV.generateConfirmationKey(
			'123456789',
			encParms,
			exponent,
		);

		expect(confirmationKey).toBe('NzMzMDg4NTE5NTI3ODExMzYzODEwMTUzNDgyNDE0MTE3MDk2MjYxOTY0MTM0NDE4ODY0NDczNzY0ODA5Nzc5NTAyNDA1ODY5MzIyODA5MTg2N' +
			'jAwNDcwMTQ4Mjc4NDU1NTU1MTA2Njk3NTc4ODk1MjU1NjM2MDYzNTk5NDkwMTg3MzU0NjY5MzkxMDUzMTQ4MjIzNDgwODU4MDUxMTc2ODA0OTMxMzc0MTgwMjgzMjQyMjg1MDI' +
			'0OTExMjYwNjMxNTI4MzgwNzY4ODgyMDE1MDU3ODQyNzEzNzkwMDA3Njg3NzI1NTU5MDU1OTM4MTkzMjAzNDQ0MDU2OTQzODMxODkxNTQxMjg1MzMzMjM0OTg1OTYwNDc2MzQ4N' +
			'jk3MjY0MzMyODYwODc0ODg5MzQzNjc0NTk4NjMyMDIyOTk1NjU1NzA2MTQ0Nzg5NDc2ODc4MDQwMDgzNzIyNjM3MTc4MjQyNDQzNjU3ODc3ODI3Njk4MzM0NjkyMzM1NDU0ODc' +
			'5MzY0MDUzODExNDg3Njg4MDE5ODk5ODIzOTQ3MzgwMzI4ODI1MDA0NTMzODI4MTMxMzE5NjY4MjcwMzM2MDM3NjM0ODIxMTY2NTA0NzcxODIyODQzODg5OTMyNDQwNzEzMzkyM' +
			'DgzMjI5NTM1NzExOTM4MDk2ODMyNDIwNjU2OTc2MjkzMjgzODA1NDI3NzQ4NjA0OTU1OTEyMDkyOTk1Njc1MzIyMDk4Mzk3MDgwNTQ4MDA5NjczNjAwNjA3MTQxODEwOTY3Njg' +
			'0NDg0OTE3MjI1NDk1MTQ4NzAwMDczMzg5Nzg4NzQwNA==');
	});
});
