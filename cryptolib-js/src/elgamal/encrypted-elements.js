/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const codec = require('../codec');

module.exports = ElGamalEncryptedElements;

/**
 * @class ElGamalEncryptedElements
 * @classdesc Encapsulates the components of the ElGamal encryption of some Zp
 *            group elements or the pre-computation of such an encryption. To
 *            instantiate this object, use the method {@link
	*            ElGamalCryptographyService.newEncryptedElements}.
 * @property {ZpGroupElement} gamma The gamma Zp group element that comprises
 *           the ElGamal encryption or pre-computation.
 * @property {ZpGroupElement[]} phis The phi Zp group elements that comprise the
 *           ElGamal encryption or pre-computation.
 * @property {Exponent} [secret=undefined] The secret exponent that comprises
 *           the encryption or pre-computation. Required when the secret exponent
 *           is needed later for zero-knowledge proof generation.
 */
function ElGamalEncryptedElements(gamma, phis, secret) {
	this.gamma = gamma;
	this.phis = phis;
	Object.freeze(this.phis);
	this.secret = secret;

	Object.freeze(this);
}

ElGamalEncryptedElements.prototype = {
	/**
	 * Serializes this object into a JSON string representation.
	 * <p>
	 * <b>IMPORTANT:</b> This serialization must be exactly the same as the
	 * corresponding serialization in the library <code>cryptoLib</code>,
	 * implemented in Java, since the two libraries are expected to communicate
	 * with each other via these serializations.
	 * <p>
	 * <b>NOTE:</b> For security reasons, the secret exponent of the ElGamal
	 * encryption or pre-computation is not included in this serialization.
	 *
	 * @function toJson
	 * @memberof ElGamalEncryptedElements
	 * @returns {string} The JSON string representation of this object.
	 */
	toJson: function () {
		const pB64 = codec.base64Encode(this.gamma.p);
		const qB64 = codec.base64Encode(this.gamma.q);

		const gammaB64 = codec.base64Encode(this.gamma.value);

		const phisB64 = [];
		for (let i = 0; i < this.phis.length; i++) {
			phisB64[i] = codec.base64Encode(this.phis[i].value);
		}

		return JSON.stringify({
			ciphertext: {
				p: pB64,
				q: qB64,
				gamma: gammaB64,
				phis: phisB64,
			}
		});
	}
};
