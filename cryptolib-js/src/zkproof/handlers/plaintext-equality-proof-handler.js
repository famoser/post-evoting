/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ZeroKnowledgeProofProver = require('../prover');
const ZeroKnowledgeProofVerifier = require('../verifier');
const PhiFunction = require('../phi-function');
const validator = require('../../input-validator');

module.exports = PlaintextEqualityProofHandler;

const SECONDARY_ELGAMAL_PK = 'secondary ElGamal public key Zp group elements';

/**
 * @class PlaintextEqualityProofHandler
 * @classdesc Encapsulates a handler for the plaintext equality zero-knowledge
 *            proof of knowledge generation, pre-computation and verification
 *            processes. To instantiate this object, use the method {@link
	*            ZeroKnowledgeProofService.newPlaintextEqualityProofHandler}.
 * @param {ZpSubgroup}
 *            group The Zp subgroup to which all exponents and Zp group elements
 *            required for the proof generation are associated or belong,
 *            respectively.
 * @param {MessageDigestService}
 *            messageDigestService The message digest service.
 * @param {MathematicalService}
 *            mathematicalService The mathematical service.
 */
function PlaintextEqualityProofHandler(
	group, messageDigestService, mathematicalService) {
	const NUM_PHI_INPUTS = 2;
	const DEFAULT_AUXILIARY_DATA = 'PlaintextEqualityProof';

	const _mathGroupHandler = mathematicalService.newGroupHandler();
	const _prover =	new ZeroKnowledgeProofProver(messageDigestService, mathematicalService);
	const _verifier = new ZeroKnowledgeProofVerifier(messageDigestService, mathematicalService);

	let _primaryPublicKey;
	let _secondaryPublicKey;

	/**
	 * Initializes the handler with the provided primary and secondary ElGamal
	 * public keys.
	 *
	 * @function init
	 * @memberof PlaintextEqualityProofHandler
	 * @param {ElGamalPublicKey}
	 *            primaryPublicKey The primary ElGamal public key.
	 * @param {ElGamalPublicKey}
	 *            secondaryPublicKey The secondary ElGamal public key.
	 * @returns {PlaintextEqualityProofHandler} A reference to this object, to
	 *          facilitate method chaining.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.init = function (primaryPublicKey, secondaryPublicKey) {
		checkInitData(primaryPublicKey, secondaryPublicKey);

		_primaryPublicKey = primaryPublicKey;
		_secondaryPublicKey = secondaryPublicKey;

		return this;
	};

	/**
	 * Generates a plaintext equality zero-knowledge proof of knowledge. Before
	 * using this method, the handler must have been initialized with primary
	 * secondary public keys, via the method
	 * {@link PlaintextEqualityProofHandler.init}.
	 *
	 * @function generate
	 * @memberof PlaintextEqualityProofHandler
	 * @param {Exponent}
	 *            primarySecret The secret exponent used to generate the primary
	 *            ciphertext, the knowledge of which must be proven.
	 * @param {Exponent}
	 *            secondarySecret The secret exponent used to generate the
	 *            secondary ciphertext, the knowledge of which must be proven.
	 * @param {ElGamalEncryptedElements}
	 *            primaryCiphertext The primary ciphertext.
	 * @param {ElGamalEncryptedElements}
	 *            secondaryCiphertext The secondary ciphertext.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {Uint8Array|string}
	 *            [options.data='PlaintextEqualityProof'] Auxiliary data.
	 * @param {ZeroKnowledgeProofPreComputation}
	 *            [options.preComputation=Generated internally] A
	 *            pre-computation of the plaintext equality zero-knowledge proof
	 *            of knowledge.
	 * @returns {ZeroKnowledgeProof} The plaintext equality zero-knowledge proof
	 *          of knowledge.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.generate = function (
		primarySecret, secondarySecret, primaryCiphertext, secondaryCiphertext,
		options) {
		if (typeof _primaryPublicKey === 'undefined') {
			throw new Error(
				'Cannot generate plaintext equality proof; Associated handler has not been initialized with any public keys');
		}

		options = options || {};

		checkGenerationData(
			primarySecret, secondarySecret, primaryCiphertext, secondaryCiphertext,
			options);

		let data = options.data;
		if (typeof data === 'undefined') {
			data = DEFAULT_AUXILIARY_DATA;
		}

		let preComputation = options.preComputation;

		const privateValues = [primarySecret, secondarySecret];
		const publicValues =
			generatePublicValues(primaryCiphertext, secondaryCiphertext);
		if (typeof preComputation === 'undefined') {
			preComputation = this.preCompute();
		}

		return _prover.prove(
			group, privateValues, publicValues, data, preComputation);
	};

	/**
	 * Pre-computes a plaintext equality zero-knowledge proof of knowledge.
	 * IMPORTANT: The same pre-computed values must not be used twice. Before
	 * using this method, the handler must have been initialized with primary
	 * secondary public keys, via the method {@link
		* PlaintextEqualityProofHandler.init}.
	 *
	 * @function preCompute
	 * @memberof PlaintextEqualityProofHandler
	 * @returns {ZeroKnowledgeProofPreComputation} The plaintext equality
	 *          zero-knowledge proof of knowledge pre-computation.
	 */
	this.preCompute = function () {
		if (typeof _primaryPublicKey === 'undefined') {
			throw new Error(
				'Cannot pre-compute plaintext equality proof; Associated handler has not been initialized with any public keys');
		}

		const phiFunction =
			newPhiFunction(group, _primaryPublicKey, _secondaryPublicKey);

		return _prover.preCompute(group, phiFunction);
	};

	/**
	 * Verifies a plaintext equality zero-knowledge proof of knowledge. Before
	 * using this method, the handler must have been initialized with primary
	 * secondary public keys, via the method {@link
		* PlaintextEqualityProofHandler.init}.
	 *
	 * @function verify
	 * @memberof PlaintextEqualityProofHandler
	 * @param {ZeroKnowledgeProof}
	 *            proof The plaintext equality zero-knowledge proof of knowledge
	 *            to verify.
	 * @param {ElGamalEncryptedElements}
	 *            primaryCiphertext The primary ciphertext.
	 * @param {ElGamalEncryptedElements}
	 *            secondaryCiphertext The secondary ciphertext.
	 * @param {Object}
	 *            [options] An object containing optional arguments.
	 * @param {Uint8Array|string}
	 *            [options.data='PlaintextEqualityProof'] Auxiliary data. It
	 *            must be the same as that used to generate the proof.
	 * @returns {boolean} <code>true</code> if the plaintext equality
	 *          zero-knowledge proof of knowledge was verified,
	 *          <code>false</code> otherwise.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.verify = function (
		proof, primaryCiphertext, secondaryCiphertext, options) {
		if (typeof _primaryPublicKey === 'undefined') {
			throw new Error(
				'Cannot verify plaintext equality proof; Associated handler has not been initialized with any public keys');
		}

		options = options || {};

		checkVerificationData(
			proof, primaryCiphertext, secondaryCiphertext, options);

		let data = options.data;
		if (typeof data === 'undefined') {
			data = DEFAULT_AUXILIARY_DATA;
		}

		const phiFunction =
			newPhiFunction(group, _primaryPublicKey, _secondaryPublicKey);
		const publicValues =
			generatePublicValues(primaryCiphertext, secondaryCiphertext);

		return _verifier.verify(group, proof, phiFunction, publicValues, data);
	};

	function generatePublicValues(primaryCiphertext, secondaryCiphertext) {
		const quotientElements = _mathGroupHandler.divideElements(
			primaryCiphertext.phis, secondaryCiphertext.phis);

		return [primaryCiphertext.gamma, secondaryCiphertext.gamma].concat(
			quotientElements,
			primaryCiphertext.phis,
			secondaryCiphertext.phis,
			_primaryPublicKey.elements,
			_secondaryPublicKey.elements,
			group.generator);
	}

	function newPhiFunction(group, primaryPublicKey, secondaryPublicKey) {
		const numOutputs = primaryPublicKey.elements.length + 2;
		const computationRules = generateComputationRules(numOutputs);
		const baseElements =
			generateBaseElements(group, primaryPublicKey, secondaryPublicKey);

		return new PhiFunction(
			NUM_PHI_INPUTS, numOutputs, computationRules, baseElements);
	}

	function generateComputationRules(numOutputs) {
		const rules = [];

		rules[0] = [];
		rules[0][0] = [];
		rules[0][0].push(1);
		rules[0][0].push(1);

		rules[1] = [];
		rules[1][0] = [];
		rules[1][0].push(1);
		rules[1][0].push(2);

		for (let i = 2; i < numOutputs; i++) {
			rules[i] = [];

			rules[i][0] = [];
			rules[i][0].push(i);
			rules[i][0].push(1);

			rules[i][1] = [];
			rules[i][1].push(i + (numOutputs - 2));
			rules[i][1].push(2);
		}

		return rules;
	}

	function generateBaseElements(group, primaryPublicKey, secondaryPublicKey) {
		return [group.generator].concat(
			primaryPublicKey.elements, invertPublicKeyElements(secondaryPublicKey));
	}

	function invertPublicKeyElements(publicKey) {
		const elements = publicKey.elements;

		const invertedElements = [];
		for (let i = 0; i < elements.length; i++) {
			invertedElements.push(elements[i].invert());
		}

		return invertedElements;
	}

	function checkInitData(primaryPublicKey, secondaryPublicKey) {
		mathematicalService.checkGroupMatchesPolicy(primaryPublicKey.group);
		mathematicalService.checkGroupMatchesPolicy(secondaryPublicKey.group);
		mathematicalService.checkGroupArrayMatchesPolicy(primaryPublicKey.elements);
		mathematicalService.checkGroupArrayMatchesPolicy(secondaryPublicKey.elements);

		validator.checkElGamalPublicKey(
			primaryPublicKey,
			'Primary ElGamal public key for plaintext equality proof handler initialization',
			group);
		validator.checkElGamalPublicKey(
			secondaryPublicKey,
			'Secondary ElGamal public key for plaintext equality proof handler initialization',
			group);
		validator.checkArrayLengthsEqual(
			primaryPublicKey.elements,
			'primary ElGamal public key Zp group elements for plaintext equality proof handler initialization',
			secondaryPublicKey.elements,
			SECONDARY_ELGAMAL_PK);
	}

	function checkGenerationData(
		primarySecret, secondarySecret, primaryCiphertext, secondaryCiphertext,
		options) {
		validator.checkExponent(
			primarySecret,
			'Primary secret exponent for plaintext equality proof generation',
			group.q);
		mathematicalService.checkGroupMatchesPolicy(primaryCiphertext.gamma);
		mathematicalService.checkGroupArrayMatchesPolicy(primaryCiphertext.phis);
		validator.checkElGamalEncryptedElements(
			primaryCiphertext,
			'Primary ciphertext ElGamal encrypted elements for plaintext equality proof generation',
			group);
		validator.checkExponent(
			secondarySecret,
			'Secondary secret exponent for plaintext equality proof generation',
			group.q);
		mathematicalService.checkGroupMatchesPolicy(secondaryCiphertext.gamma);
		mathematicalService.checkGroupArrayMatchesPolicy(secondaryCiphertext.phis);
		validator.checkElGamalEncryptedElements(
			secondaryCiphertext,
			'Secondary ciphertext ElGamal encrypted elements for plaintext equality proof generation',
			group);
		validator.checkArrayLengthsEqual(
			primaryCiphertext.phis,
			'primary ciphertext phi Zp group elements for plaintext equality proof generation',
			_primaryPublicKey.elements,
			'primary ElGamal public key Zp group elements');
		validator.checkArrayLengthsEqual(
			secondaryCiphertext.phis,
			'secondary ciphertext ElGamal phi Zp group elements for plaintext equality proof generation',
			_secondaryPublicKey.elements,
			SECONDARY_ELGAMAL_PK);
		if (typeof options.data !== 'undefined' && typeof options.data !== 'string') {
			validator.checkIsInstanceOf(
				options.data, Uint8Array, 'Uint8Array',
				'Non-string auxiliary data for plaintext equality proof generation');
		}
		if (options.preComputation) {
			validator.checkIsObjectWithProperties(
				options.preComputation,
				'Pre-computation for plaintext equality proof generation');
		}
	}

	function checkVerificationData(
		proof, primaryCiphertext, secondaryCiphertext, options) {
		validator.checkIsObjectWithProperties(
			proof, 'Plaintext equality zero-knowledge proof');
		mathematicalService.checkGroupMatchesPolicy(primaryCiphertext.gamma);
		mathematicalService.checkGroupArrayMatchesPolicy(primaryCiphertext.phis);
		validator.checkElGamalEncryptedElements(
			primaryCiphertext,
			'Primary ciphertext ElGamal encrypted elements for plaintext equality proof verification',
			group);
		mathematicalService.checkGroupMatchesPolicy(secondaryCiphertext.gamma);
		mathematicalService.checkGroupArrayMatchesPolicy(secondaryCiphertext.phis);
		validator.checkElGamalEncryptedElements(
			secondaryCiphertext,
			'Secondary ciphertext ElGamal encrypted elements for plaintext equality proof verification',
			group);
		validator.checkArrayLengthsEqual(
			primaryCiphertext.phis,
			'primary ciphertext phi Zp group elements for plaintext equality proof verification',
			_primaryPublicKey.elements,
			'primary ElGamal public key Zp group elements');
		validator.checkArrayLengthsEqual(
			secondaryCiphertext.phis,
			'secondary ciphertext ElGamal phi Zp group elements for plaintext equality proof verification',
			_secondaryPublicKey.elements,
			SECONDARY_ELGAMAL_PK);
		if (typeof options.data !== 'undefined' && typeof options.data !== 'string') {
			validator.checkIsInstanceOf(
				options.data, Uint8Array, 'Uint8Array',
				'Non-string auxiliary data for plaintext equality proof verification');
		}
	}
}
