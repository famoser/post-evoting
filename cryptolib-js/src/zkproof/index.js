/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ZeroKnowledgeProofService = require('./service');
const validator = require('../input-validator');

module.exports = {
	/**
	 * Creates a new ZeroKnowledgeProofService object, which encapsulates a
	 * zero-knowledge proof service.
	 *
	 * @function newService
	 * @global
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {Policy}
	 *            [options.policy=Default policy] The cryptographic policy to
	 *            use.
	 * @param {MessageDigestService}
	 *            [options.messageDigestService=Created internally] The message
	 *            digest service to use.
	 * @param {SecureRandomService}
	 *            [options.secureRandomService=Created internally] The secure
	 *            random service to use.
	 * @param {MathematicalService}
	 *            [options.mathematicalService=Created internally] The
	 *            mathematical service to use.
	 * @returns {ZeroKnowledgeProofService} The new ZeroKnowledgeProofService
	 *          object.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	newService: function (options) {
		checkData(options);

		return new ZeroKnowledgeProofService(options);
	}
};

function checkData(options) {
	options = options || {};

	if (typeof options.policy !== 'undefined') {
		validator.checkIsObjectWithProperties(
			options.policy,
			'Cryptographic policy provided to zero-knowledge proof service');
	}

	if (typeof options.messageDigestService !== 'undefined') {
		validator.checkIsObjectWithProperties(
			options.messageDigestService,
			'Message digest service object provided to zero-knowledge proof service');
	}

	if (typeof options.secureRandomService !== 'undefined') {
		validator.checkIsObjectWithProperties(
			options.secureRandomService,
			'Secure random service object provided to zero-knowledge proof service');
	}

	if (typeof options.mathematicalService !== 'undefined') {
		validator.checkIsObjectWithProperties(
			options.mathematicalService,
			'Mathematical service object provided to zero-knowledge proof service');
	}
}
