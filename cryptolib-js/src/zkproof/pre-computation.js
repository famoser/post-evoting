/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const codec = require('../codec');

module.exports = ZeroKnowledgeProofPreComputation;

/**
 * @class ZeroKnowledgeProofPreComputation
 * @classdesc Encapsulates a zero-knowledge proof of knowledge pre-computation.
 *            This object is instantiated by the method {@link
	*            ZeroKnowledgeProofService.newPreComputation} or internally by the
 *            <code>preCompute</code> method of any zero-knowledge proof of
 *            knowledge prover object.
 * @property {Exponent[]} exponents The array of randomly generated exponents
 *           that comprise the pre-computation.
 * @property {ZpGroupElement[]} phiOutputs The array of PHI function output
 *           elements that comprise the pre-computation.
 */
function ZeroKnowledgeProofPreComputation(exponents, phiOutputs) {
	this.exponents = exponents;
	Object.freeze(this.exponents);

	this.phiOutputs = phiOutputs;
	Object.freeze(this.phiOutputs);

	Object.freeze(this);
}

ZeroKnowledgeProofPreComputation.prototype = {
	/**
	 * Serializes this object into a JSON string representation.
	 * <p>
	 * <b>IMPORTANT:</b> This serialization must be exactly the same as the
	 * corresponding serialization in the library <code>cryptoLib</code>,
	 * implemented in Java, since the two libraries are expected to communicates
	 * with each other via these serializations.
	 *
	 * @function toJson
	 * @memberof ZeroKnowledgeProofPreComputation
	 * @returns {string} The JSON string representation of this object.
	 */
	toJson: function () {
		const pB64 = codec.base64Encode(this.phiOutputs[0].p);
		const qB64 = codec.base64Encode(this.phiOutputs[0].q);
		const exponentValuesB64 = [];
		for (let i = 0; i < this.exponents.length; i++) {
			exponentValuesB64[i] = codec.base64Encode(this.exponents[i].value);
		}
		const phiOutputValuesB64 = [];
		for (let j = 0; j < this.phiOutputs.length; j++) {
			phiOutputValuesB64[j] = codec.base64Encode(this.phiOutputs[j].value);
		}

		return JSON.stringify({
			preComputed: {
				p: pB64,
				q: qB64,
				exponents: exponentValuesB64,
				phiOutputs: phiOutputValuesB64
			}
		});
	}
};
