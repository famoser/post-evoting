/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const codec = require('../codec');

module.exports = ElGamalPublicKey;

/**
 * @class ElGamalPublicKey
 * @classdesc Encapsulates an ElGamal public key. To instantiate this object,
 *            use the method {@link ElGamalCryptographyService.newPublicKey}.
 * @property {ZpSubgroup} group The Zp subgroup to which the Zp group elements
 *           of this public key belong.
 * @property {ZpGroupElement[]} elements The Zp group elements that comprise
 *           this public key.
 */
function ElGamalPublicKey(group, elements) {
	this.group = group;
	this.elements = elements;
	Object.freeze(this.elements);

	Object.freeze(this);
}

ElGamalPublicKey.prototype = {
	/**
	 * Serializes this object into a JSON string representation.
	 * <p>
	 * <b>IMPORTANT:</b> This serialization must be exactly the same as the
	 * corresponding serialization in the library <code>cryptoLib</code>,
	 * implemented in Java, since the two libraries are expected to communicates
	 * with each other via these serializations.
	 *
	 * @function toJson
	 * @memberof ElGamalPublicKey
	 * @returns {string} The JSON string representation of this object.
	 */
	toJson: function () {
		const gB64 = codec.base64Encode(this.group.generator.value);
		const pB64 = codec.base64Encode(this.group.p);
		const qB64 = codec.base64Encode(this.group.q);

		const elementsB64 = [];
		for (let i = 0; i < this.elements.length; i++) {
			elementsB64.push(codec.base64Encode(this.elements[i].value));
		}

		return JSON.stringify({
			publicKey:
				{zpSubgroup: {g: gB64, p: pB64, q: qB64}, elements: elementsB64}
		});
	}
};
