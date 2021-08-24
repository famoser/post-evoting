/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ZeroKnowledgeProof = require('./proof');
const ZeroKnowledgeProofPreComputation = require('./pre-computation');
const ExponentiationProofHandler = require('./handlers/exponentiation-proof-handler');
const PlaintextEqualityProofHandler = require('./handlers/plaintext-equality-proof-handler');
const cryptoPolicy = require('../cryptopolicy');
const messageDigest = require('../messagedigest');
const mathematical = require('../mathematical');
const validator = require('../input-validator');
const codec = require('../codec');
require('./array');

module.exports = ZeroKnowledgeProofService;

/**
 * @class ZeroKnowledgeProofService
 * @classdesc The zero-knowledge proof service API. To instantiate this object,
 *            use the method {@link newService}.
 * @hideconstructor
 * @param {Object}
 *            [options] An object containing optional arguments.
 * @param {Policy}
 *            [options.policy=Default policy] The cryptographic policy to use.
 * @param {MessageDigestService}
 *            [options.messageDigestService=Created internally] The message
 *            digest service to use.
 * @param {SecureRandomService}
 *            [options.secureRandomService=Created internally] The secure random
 *            service to use.
 * @param {MathematicalService}
 *            [options.mathematicalService=Created internally] The mathematical
 *            service to use.
 */
function ZeroKnowledgeProofService(options) {
	options = options || {};

	let policy;
	if (options.policy) {
		policy = options.policy;
	} else {
		policy = cryptoPolicy.newInstance();
	}

	let _messageDigestService;
	if (options.messageDigestService) {
		_messageDigestService = options.messageDigestService;
	} else {
		_messageDigestService = messageDigest.newService({policy: policy});
	}

	let secureRandomService;
	if (options.secureRandomService) {
		secureRandomService = options.secureRandomService;
	}

	let _mathService;
	if (options.mathematicalService) {
		_mathService = options.mathematicalService;
	} else if (secureRandomService && policy) {
		_mathService =
			mathematical.newService({policy: policy, secureRandomService: secureRandomService});
	} else if (policy) {
		_mathService = mathematical.newService({policy: policy});
	} else {
		_mathService = mathematical.newService();
	}

	/**
	 * Creates a new ZeroKnowledgeProof object from a provided zero-knowledge
	 * proof of knowledge or its components.
	 *
	 * @function newProof
	 * @memberof ZeroKnowledgeProofService
	 * @param {Exponent}
	 *            hashOrJson The hash of the zero-knowledge proof of knowledge
	 *            <b>OR</b> a JSON string representation of a
	 *            ZeroKnowledgeProof object, compatible with its
	 *            <code>toJson</code> method. For the latter case, any
	 *            additional input arguments will be ignored.
	 * @param {Exponent[]}
	 *            values The values of the zero-knowledge proof of knowledge.
	 * @returns {ZeroKnowledgeProof} The new ZeroKnowledgeProof object.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.newProof = function (hashOrJson, exponents) {
		if (typeof hashOrJson !== 'string') {
			validator.checkExponent(
				hashOrJson, 'Hash to create new ZeroKnowledgeProof object');
			validator.checkExponents(
				exponents, 'Exponents to create new ZeroKnowledgeProof object');

			return new ZeroKnowledgeProof(hashOrJson, exponents);
		} else {
			return jsonToProof(hashOrJson);
		}
	};

	/**
	 * Creates a new ZeroKnowledgeProofPreComputation object from a provided
	 * zero-knowledge proof of knowledge pre-computation or its components.
	 *
	 * @function newPreComputation
	 * @memberof ZeroKnowledgeProofService
	 * @param {Exponent[]}
	 *            exponentsOrJson The array of randomly generated exponents that
	 *            comprise the pre-computation <b>OR</b> a JSON string
	 *            representation of a ZeroKnowledgeProofPreComputation object,
	 *            compatible with its <code>toJson</code> method. For the
	 *            latter case, any additional input arguments will be ignored.
	 * @param {ZpGroupElements[]}
	 *            phiOutputs The array of PHI function output elements that
	 *            comprise the pre-computation.
	 * @returns {ZeroKnowledgeProofPreComputation} The new
	 *          ZeroKnowledgeProofPreComputation object.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.newPreComputation = function (exponentsOrJson, phiOutputs) {
		if (typeof exponentsOrJson !== 'string') {
			_mathService.checkGroupArrayMatchesPolicy(phiOutputs);
			validator.checkExponents(
				exponentsOrJson,
				'Exponents to create new ZeroKnowledgeProofPreComputation object');
			validator.checkZpGroupElements(
				phiOutputs,
				'PHI function output elements to create new ZeroKnowledgeProofPreComputation object');

			return new ZeroKnowledgeProofPreComputation(exponentsOrJson, phiOutputs);
		} else {
			return jsonToPreComputation(exponentsOrJson);
		}
	};

	/**
	 * Creates new ExponentiationProofHandler object, for generating,
	 * pre-computing and verifying exponentation zero-knowledge proofs of
	 * knowledge. It must be initialized with base elements before it is used.
	 *
	 * @function newExponentiationProofHandler
	 * @memberof ZeroKnowledgeProofService
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which all exponents and Zp group
	 *            elements required for the proof generation are associated or
	 *            belong, respectively.
	 * @returns {ExponentiationProofHandler} The ExponentiationProofHandler
	 *          object.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.newExponentiationProofHandler = function (group) {
		validator.checkIsObjectWithProperties(
			group, 'Zp subgroup for exponentiation proof handler');
		_mathService.checkGroupMatchesPolicy(group);

		return new ExponentiationProofHandler(
			group, _messageDigestService, _mathService);
	};

	/**
	 * Creates new PlaintextEqualityProofHandler object, for generating,
	 * pre-computing and verifying plaintext equality zero-knowledge proofs of
	 * knowledge. It must be initialized with primary and secondary ElGamal
	 * public keys before it is used.
	 *
	 * @function newPlaintextEqualityProofHandler
	 * @memberof ZeroKnowledgeProofService
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which all exponents and Zp group
	 *            elements required for the proof generation are associated or
	 *            belong, respectively.
	 * @returns {PlaintextEqualityProofHandler} The
	 *          PlaintextEqualityProofHandler object.
	 * @throws {Error}
	 *             If the input data validation fails.
	 */
	this.newPlaintextEqualityProofHandler = function (group) {
		validator.checkIsObjectWithProperties(
			group, 'Zp subgroup for plaintext equality proof handler');
		_mathService.checkGroupMatchesPolicy(group);

		return new PlaintextEqualityProofHandler(
			group, _messageDigestService, _mathService);
	};

	function jsonToProof(json) {
		validator.checkIsJsonString(
			json, 'JSON to deserialize to ZeroKnowledgeProof object');

		const parsed = JSON.parse(json).zkProof;

		const q = codec.bytesToBigInteger(codec.base64Decode(parsed.q));
		const hash = _mathService.newExponent(
			q, codec.bytesToBigInteger(codec.base64Decode(parsed.hash)));

		const values = [];
		const valuesB64 = parsed.values;
		for (let i = 0; i < valuesB64.length; i++) {
			values.push(_mathService.newExponent(
				q, codec.bytesToBigInteger(codec.base64Decode(valuesB64[i]))));
		}

		return new ZeroKnowledgeProof(hash, values);
	}

	function jsonToPreComputation(json) {
		validator.checkIsJsonString(
			json, 'JSON to deserialize to ZeroKnowledgeProofPreComputation object');

		const parsed = JSON.parse(json).preComputed;

		const p = codec.bytesToBigInteger(codec.base64Decode(parsed.p));
		const q = codec.bytesToBigInteger(codec.base64Decode(parsed.q));

		const exponentValues = parsed.exponents;
		const exponents = [];
		for (let i = 0; i < exponentValues.length; i++) {
			exponents.push(_mathService.newExponent(
				q, codec.bytesToBigInteger(codec.base64Decode(exponentValues[i]))));
		}

		const phiOutputValues = parsed.phiOutputs;
		const phiOutputs = [];
		for (let j = 0; j < phiOutputValues.length; j++) {
			phiOutputs.push(_mathService.newZpGroupElement(
				p, q,
				codec.bytesToBigInteger(codec.base64Decode(phiOutputValues[j]))));
		}

		return new ZeroKnowledgeProofPreComputation(exponents, phiOutputs);
	}
}
