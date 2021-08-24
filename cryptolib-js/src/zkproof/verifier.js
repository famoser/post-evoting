/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const HashGenerator = require('./hash-generator');
const codec = require('../codec');

module.exports = ZeroKnowledgeProofVerifier;

/**
 * @class ZeroKnowledgeProofVerifier
 * @classdesc Encapsulates a zero-knowledge proof of knowledge verifier that
 *            uses the PHI function defined in Maurer's unified framework.
 *            Is only used for testing purposes.
 * @private
 * @param {MessageDigestService}
 *            messageDigestService The message digest service.
 * @param {MathematicalService}
 *            mathService The mathematical service.
 */
function ZeroKnowledgeProofVerifier(messageDigestService, mathService) {
	/**
	 * Verifies a zero-knowledge proof of knowledge.
	 *
	 * @function verify
	 * @memberof ZeroKnowledgeProofVerifier
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which all exponents and Zp group
	 *            elements required for the proof verification are associated or
	 *            belong, respectively.
	 * @param {ZeroKnowledgeProof}
	 *            proof The zero-knowledge proof of knowledge to verify.
	 * @param {PhiFunction}
	 *            phiFunction The PHI function used for the verification.
	 * @param {ZpGroupElement[]}
	 *            publicValues The public values used to verify the proof.
	 * @param {Uint8Array|string}
	 *            data Auxiliary data.
	 * @returns {boolean} <code>true</code> if the zero-knowledge proof of
	 *          knowledge was verified, <code>false</code> otherwise.
	 */
	this.verify = function (
		group, proof, phiFunction, publicValues, data) {

		const proofHash = proof.hash;

		let phiOutputs;
		let generatedValues;
		phiOutputs = phiFunction.calculate(proof.values);
		generatedValues = this.generateValues(publicValues, phiOutputs, proofHash);

		const calculatedHash =
			generateHash(group, publicValues, generatedValues, data);

		return proofHash.equals(calculatedHash);
	};

	function generateHash(group, publicValues, generatedValues, data) {
		const hashGenerator = new HashGenerator(messageDigestService);

		const hashBytes = hashGenerator.generate(publicValues, generatedValues, data);
		const hashByteArray = Array.apply([], hashBytes);
		hashByteArray.unshift(0);

		const value = codec.bytesToBigInteger(hashBytes);

		return mathService.newExponent(group.q, value);
	}
}

ZeroKnowledgeProofVerifier.prototype = {
	generateValues: function (
		publicValues, phiOutputs, proofHash) {
		const generatedValues = [];
		for (let i = 0; i < phiOutputs.length; i++) {
			generatedValues.push(phiOutputs[i].multiply(
				publicValues[i].exponentiate(proofHash.negate())));
		}

		return generatedValues;
	}
};
