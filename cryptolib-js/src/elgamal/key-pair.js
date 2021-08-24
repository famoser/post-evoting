/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

module.exports = ElGamalKeyPair;

/**
 * @class ElGamalKeyPair
 * @classdesc Encapsulates an ElGamal key pair. To instantiate this object, use
 *            the method {@link ElGamalCryptographyService.newKeyPair}.
 * @property {ElGamalPublicKey} publicKey The ElGamal public key comprising the
 *           key pair.
 * @property {ElGamalPrivateKey} privateKey The ElGamal private key comprising
 *           the key pair.
 */
function ElGamalKeyPair(publicKey, privateKey) {
	return Object.freeze({publicKey: publicKey, privateKey: privateKey});
}
