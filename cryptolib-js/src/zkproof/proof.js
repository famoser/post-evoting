/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const codec = require('../codec');

module.exports = ZeroKnowledgeProof;

/**
 * @class ZeroKnowledgeProof
 * @classdesc Encapsulates a zero-knowledge proof of knowledge. This object is
 *            instantiated by the method
 *            {@link ZeroKnowledgeProofService.newProof} or internally by the
 *            <code>prove</code> method of any zero-knowledge proof of
 *            knowledge prover object.
 * @property {Exponent} hash The hash of the zero-knowledge proof of knowledge.
 * @property {Exponent[]} values The values of the zero-knowledge proof of
 *           knowledge.
 */
function ZeroKnowledgeProof(hash, values) {
	this.hash = hash;
	this.values = values;
	Object.freeze(this.values);

	Object.freeze(this);
}

ZeroKnowledgeProof.prototype = {
	/**
	 * Checks if this zero-knowledge proof of knowledge is equal to the
	 * zero-knowledge proof of knowledge provided as input.
	 *
	 * @function equals
	 * @memberof ZeroKnowledgeProof
	 * @param {ZeroKnowledgeProof}
	 *            proof The zero-knowledge proof of knowledge that should be
	 *            checked against this zero-knowledge proof of knowledge for
	 *            equality.
	 * @returns {boolean} <code>true</code> if the equality holds,
	 *          <code>false</code> otherwise.
	 */
	equals: function (proof) {
		return proof.hash.equals(this.hash && proof.values.equals(this.values));
	},

	/**
	 * Serializes this object into a JSON string representation.
	 * <p>
	 * <b>IMPORTANT:</b> This serialization must be exactly the same as the
	 * corresponding serialization in the library <code>cryptoLib</code>,
	 * implemented in Java, since the two libraries are expected to communicates
	 * with each other via these serializations.
	 *
	 * @function toJson
	 * @memberof ZeroKnowledgeProof
	 * @returns {string} The JSON string representation of this object.
	 */
	toJson: function () {
		const qB64 = codec.base64Encode(this.hash.q);
		const hashB64 = codec.base64Encode(this.hash.value);
		const valuesB64 = [];
		for (let i = 0; i < this.values.length; i++) {
			valuesB64[i] = codec.base64Encode(this.values[i].value);
		}

		return JSON.stringify(
			{zkProof: {q: qB64, hash: hashB64, values: valuesB64}});
	}
};
