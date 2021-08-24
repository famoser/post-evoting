/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ZeroKnowledgeProofProver = require('../prover');
const ZeroKnowledgeProofVerifier = require('../verifier');
const PhiFunction = require('../phi-function');
const validator = require('../../input-validator');

module.exports = ExponentiationProofHandler;

/**
 * @class ExponentiationProofHandler
 * @classdesc Encapsulates a handler for the exponentiation zero-knowledge proof
 *            of knowledge generation, pre-computation and verification
 *            processes. To instantiate this object, use the method {@link
	*            ZeroKnowledgeProofService.newExponentiationProofHandler}.
 * @param {ZpSubgroup}
 *            group The Zp subgroup to which all exponents and Zp group elements
 *            required for the proof generation are associated or belong,
 *            respectively.
 * @param {MessageDigestService}
 *            messageDigestService The message digest service.
 * @param {MathematicalService}
 *            mathematicalService The mathematical service.
 */
function ExponentiationProofHandler(
	group, messageDigestService, mathematicalService) {
	const NUM_PHI_INPUTS = 1;
	const DEFAULT_AUXILIARY_DATA = 'ExponentiationProof';

	const _prover =	new ZeroKnowledgeProofProver(messageDigestService, mathematicalService);
	const _verifier = new ZeroKnowledgeProofVerifier(messageDigestService, mathematicalService);

	let _baseElements;

	/**
	 * Initializes the handler with the provided base elements.
	 *
	 * @function init
	 * @memberof ExponentiationProofHandler
	 * @param {ZpGroupElement[]}
	 *            baseElements The base elements.
	 * @returns {ExponentiationProofHandler} A reference to this object, to
	 *          facilitate method chaining.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.init = function (baseElements) {
		mathematicalService.checkGroupArrayMatchesPolicy(baseElements);

		validator.checkZpGroupElements(
			baseElements,
			'Base elements for exponentiation proof handler initialization', group);

		_baseElements = baseElements;

		return this;
	};

	/**
	 * Generates an exponentiation zero-knowledge proof of knowledge. Before
	 * using this method, the handler must have been initialized with base
	 * elements, via the method {@link ExponentiationProofHandler.init}.
	 *
	 * @function generate
	 * @memberof ExponentiationProofHandler
	 * @param {Exponent}
	 *            secret The secret exponent, the knowledge of which must be
	 *            proven.
	 * @param {ZpGroupElement[]}
	 *            exponentiatedElements The base elements after exponentiation
	 *            with the secret exponent.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {Uint8Array|string}
	 *            [options.data='ExponentiationProof'] Auxiliary data.
	 * @param {ZeroKnowledgeProofPreComputation}
	 *            [options.preComputation=Generated internally] A
	 *            pre-computation of the exponentiation zero-knowledge proof of
	 *            knowledge.
	 * @returns {ZeroKnowledgeProof} The exponentiation zero-knowledge proof of
	 *          knowledge.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.generate = function (secret, exponentiatedElements, options) {
		if (typeof _baseElements === 'undefined') {
			throw new Error(
				'Cannot generate exponentiation proof; Associated handler has not been initialized with any base elements');
		}

		options = options || {};

		checkGenerationData(secret, exponentiatedElements, options);

		let data = options.data;
		if (typeof data === 'undefined') {
			data = DEFAULT_AUXILIARY_DATA;
		}

		let preComputation = options.preComputation;

		const privateValues = [secret];
		const publicValues = getPublicValues(exponentiatedElements);
		if (typeof preComputation === 'undefined') {
			preComputation = this.preCompute();
		}

		return _prover.prove(
			group, privateValues, publicValues, data, preComputation);
	};

	/**
	 * Pre-computes an exponentiation zero-knowledge proof of knowledge.
	 * IMPORTANT: The same pre-computed values must not be used twice. Before
	 * using this method, the handler must have been initialized with base
	 * elements, via the method {@link ExponentiationProofHandler.init}.
	 *
	 * @function preCompute
	 * @memberof ExponentiationProofHandler
	 * @returns {ZeroKnowledgeProofPreComputation} The exponentiation
	 *          zero-knowledge proof of knowledge pre-computation.
	 */
	this.preCompute = function () {
		if (typeof _baseElements === 'undefined') {
			throw new Error(
				'Cannot pre-compute exponentiation proof; Associated handler has not been initialized with any base elements');
		}

		const phiFunction = newPhiFunction(_baseElements);

		return _prover.preCompute(group, phiFunction);
	};

	/**
	 * Verifies an exponentiation zero-knowledge proof of knowledge. Before
	 * using this method, the handler must have been initialized with base
	 * elements, via the method {@link ExponentiationProofHandler.init}.
	 *
	 * @function verify
	 * @memberof ExponentiationProofHandler
	 * @param {ZeroKnowledgeProof}
	 *            proof The exponentiation zero-knowledge proof of knowledge to
	 *            verify.
	 * @param {ZpGroupElement[]}
	 *            exponentiatedElements The base elements after exponentiation
	 *            with the secret exponent.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {Uint8Array|string}
	 *            [options.data='ExponentiationProof'] Auxiliary data. It must
	 *            be the same as that used to generate the proof.
	 * @returns {boolean} <code>true</code> if the exponentiation
	 *          zero-knowledge proof of knowledge was verified,
	 *          <code>false</code> otherwise.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.verify = function (proof, exponentiatedElements, options) {
		if (typeof _baseElements === 'undefined') {
			throw new Error(
				'Cannot verify exponentiation proof; Associated handler has not been initialized with any base elements');
		}

		options = options || {};

		checkVerificationData(proof, exponentiatedElements, options);

		let data = options.data;
		if (typeof data === 'undefined') {
			data = DEFAULT_AUXILIARY_DATA;
		}

		const phiFunction = newPhiFunction(_baseElements);
		const publicValues = getPublicValues(exponentiatedElements);

		return _verifier.verify(group, proof, phiFunction, publicValues, data);
	};

	function newPhiFunction(baseElements) {
		const numOutputs = baseElements.length;
		const computationRules = generateComputationRules(numOutputs);

		return new PhiFunction(
			NUM_PHI_INPUTS, numOutputs, computationRules, baseElements);
	}

	function generateComputationRules(numOutputs) {
		const rules = [];

		for (let i = 0; i < numOutputs; i++) {
			rules[i] = [];
			rules[i][0] = [];
			rules[i][0].push(i + 1);
			rules[i][0].push(1);
		}

		return rules;
	}

	function checkGenerationData(secret, exponentiatedElements, options) {
		validator.checkExponent(
			secret, 'Secret exponent for exponentiation proof generation', group.q);
		mathematicalService.checkGroupArrayMatchesPolicy(exponentiatedElements);
		validator.checkZpGroupElements(
			exponentiatedElements,
			'Exponentiated base elements for exponentiation proof generation',
			group);
		validator.checkArrayLengthsEqual(
			_baseElements, 'base elements for exponentiation proof generation',
			exponentiatedElements, 'exponentiated base elements');
		if (typeof options.data !== 'undefined' &&
			typeof options.data !== 'string') {
			validator.checkIsInstanceOf(
				options.data, Uint8Array, 'Uint8Array',
				'Non-string auxiliary data for exponentiation proof generation');
		}
		if (options.preComputation) {
			validator.checkIsObjectWithProperties(
				options.preComputation,
				'Pre-computation for exponentiation proof generation');
		}
	}

	function checkVerificationData(proof, exponentiatedElements, options) {
		validator.checkIsObjectWithProperties(
			proof, 'Exponentiation zero-knowledge proof');
		mathematicalService.checkGroupArrayMatchesPolicy(exponentiatedElements);
		validator.checkZpGroupElements(
			exponentiatedElements,
			'Exponentiated base elements for exponentiation proof verification',
			group);
		validator.checkArrayLengthsEqual(
			_baseElements, 'base elements for exponentiation proof verification',
			exponentiatedElements, 'exponentiated base elements');
		if (typeof options.data !== 'undefined' &&
			typeof options.data !== 'string') {
			validator.checkIsInstanceOf(
				options.data, Uint8Array, 'Uint8Array',
				'Non-string auxiliary data for exponentiation proof verification');
		}
	}

	/**
	 * Gets the public values that will be included in the hash.
	 * @param {ZpGroupElement[]} exponentiatedElements the ciphertext
	 */
	function getPublicValues(exponentiatedElements) {
		validator.checkIsNonEmptyArray(exponentiatedElements, "exponentiated elements");

		return exponentiatedElements.concat(_baseElements);
	}
}
