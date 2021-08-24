/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ZeroKnowledgeProof = require('./proof.js');
const ZeroKnowledgeProofPreComputation = require('./pre-computation.js');
const HashGenerator = require('./hash-generator');
const codec = require('../codec');

module.exports = ZeroKnowledgeProofProver;

/**
 * @class ZeroKnowledgeProofProver
 * @classdesc Encapsulates a zero-knowledge proof of knowledge prover that uses
 *            the PHI function defined in Maurer's unified framework.
 * @private
 * @param {MessageDigestService}
 *            messageDigestService The message digest service to use.
 * @param {MathematicalService}
 *            mathService The mathematical service to use.
 */
function ZeroKnowledgeProofProver(messageDigestService, mathService) {
	const _mathRandomGenerator = mathService.newRandomGenerator();

	/**
	 * Generates a zero-knowledge proof of knowledge.
	 *
	 * @function prove
	 * @memberof ZeroKnowledgeProofProver
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which all exponents and Zp group
	 *            elements required for the proof generation are associated or
	 *            belong, respectively.
	 * @param {Exponent[]}
	 *            privateValues The private values used to generate the proof.
	 * @param {ZpGroupElement[]}
	 *            publicValues The public values used to generate the proof.
	 * @param {Uint8Array|string}
	 *            data Auxiliary data.
	 * @param {ZeroKnowledgeProofPreComputation}
	 *            preComputation The zero-knowledge proof of knowledge
	 *            pre-computation.
	 * @returns {ZeroKnowledgeProof} The generated zero-knowledge proof of
	 *          knowledge.
	 */
	this.prove = function (
		group, privateValues, publicValues, data, preComputation) {
		const hash =
			generateHash(group, publicValues, preComputation.phiOutputs, data);
		const proofValues = this.generateProofValues(
			mathService, group.q, privateValues, hash, preComputation.exponents);

		return new ZeroKnowledgeProof(hash, proofValues);
	};

	/**
	 * Pre-computes a Zero-knowledge proof of knowledge. IMPORTANT: The
	 * same pre-computed values must not be used twice.
	 *
	 * @function preComputeProof
	 * @memberof ZeroKnowledgeProofProver
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which all exponents and Zp group
	 *            elements required for the pre-computation are associated or
	 *            belong, respectively.
	 * @param {PhiFunction}
	 *            phiFunction The PHI function used for the pre-computation.
	 * @returns {ZeroKnowledgeProofPreComputation} The Zero-knowledge proof of
	 * 			  knowledge pre-computation.
	 */
	this.preCompute = function (group, phiFunction) {
		const exponents = generateExponents(group, phiFunction.numInputs);

		const phiOutputs = phiFunction.calculate(exponents);

		return new ZeroKnowledgeProofPreComputation(exponents, phiOutputs);
	};

	function generateHash(group, publicValues, phiOutputs, data) {
		const hashGenerator = new HashGenerator(messageDigestService);

		const hashBytes = hashGenerator.generate(publicValues, phiOutputs, data);
		const hashByteArray = Array.apply([], hashBytes);
		hashByteArray.unshift(0);

		const value = codec.bytesToBigInteger(hashBytes);

		return mathService.newExponent(group.q, value);
	}

	function generateExponents(group, numExponents) {
		const exponents = [];
		for (let i = 0; i < numExponents; i++) {
			exponents.push(_mathRandomGenerator.nextExponent(group));
		}

		return exponents;
	}
}

ZeroKnowledgeProofProver.prototype = {

	generateProofValues: function (
		mathService, q, privateValues, hash, exponents) {
		const proofValues = [];
		let proofValue;
		for (let i = 0; i < privateValues.length; i++) {
			proofValue =
				exponents[i].value.add(privateValues[i].value.multiply(hash.value));

			proofValues.push(mathService.newExponent(q, proofValue));
		}

		return proofValues;
	}
};
