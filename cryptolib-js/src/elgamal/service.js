/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ElGamalKeyPair = require('./key-pair');
const ElGamalPublicKey = require('./public-key');
const ElGamalPrivateKey = require('./private-key');
const ElGamalEncrypter = require('./encrypter');
const ElGamalDecrypter = require('./decrypter');
const ElGamalEncryptedElements = require('./encrypted-elements');
const mathematical = require('../mathematical');
const validator = require('../input-validator');
const codec = require('../codec');
const cryptoPolicy = require('../cryptopolicy');

module.exports = ElGamalCryptographyService;

/**
 * @class ElGamalCryptographyService
 * @classdesc The ElGamal cryptography service API. To instantiate this object,
 *            use the method {@link newService}.
 * @hideconstructor
 * @param {Object}
 *            [options] An object containing optional arguments.
 * @param {Policy}
 *            [options.policy=Default policy] The cryptographic policy to use.
 * @param {SecureRandomService}
 *            [options.secureRandomService=Created internally] The secure random
 *            service to use.
 * @param {MathematicalService}
 *            [options.mathematicalService=Created internally] The mathematical
 *            service to use.
 */
function ElGamalCryptographyService(options) {
    options = options || {};

    let policy;
    if (options.policy) {
        policy = options.policy;
    } else {
        policy = cryptoPolicy.newInstance();
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
     * Creates a new ElGamalPublicKey object, which encapsulates an ElGamal
     * public key.
     *
     * @function newPublicKey
     * @memberof ElGamalCryptographyService
     * @param {ZpSubgroup|string}
     *            groupOrJson The Zp subgroup to which the elements of the
     *            public key belong <b>OR</b> a JSON string representation of
     *            an ElGamalPublicKey object, compatible with its
     *            <code>toJson</code> method. For the latter case, any
     *            additional input arguments will be ignored.
     * @param {ZpGroupElement[]}
     *            elements The Zp group elements that comprise the public key.
     * @returns {ElGamalPublicKey} The ElGamalPublicKey object.
     * @throws {Error}
     *             If the input data validation fails.
     */
    this.newPublicKey = function (groupOrJson, elements) {
        if (typeof groupOrJson !== 'string') {
            validator.checkIsObjectWithProperties(
                groupOrJson, 'Zp subgroup for new ElGamalPublicKey object');
            validator.checkZpGroupElements(
                elements, 'Zp group elements for new ElGamalPublicKey object',
                groupOrJson);
            _mathService.checkGroupMatchesPolicy(groupOrJson);
            _mathService.checkGroupArrayMatchesPolicy(elements);

            return new ElGamalPublicKey(groupOrJson, elements);
        } else {
            return jsonToPublicKey(groupOrJson);
        }
    };

    /**
     * Creates a new ElGamalPrivateKey object, which encapsulates an ElGamal
     * private key.
     *
     * @function newPrivateKey
     * @memberof ElGamalCryptographyService
     * @param {ZpSubgroup|string}
     *            groupOrJson The Zp subgroup to which the exponents of the
     *            private key are associated <b>OR</b> a JSON string
     *            representation of an ElGamalPrivateKey object, compatible with
     *            its <code>toJson</code> method. For the latter case, any
     *            additional input arguments will be ignored.
     * @param {Exponent[]}
     *            exponents The exponents that comprise the private key.
     * @returns {ElGamalPrivateKey} The new ElGamalPrivateKey object.
     * @throws {Error}
     *             If the input data validation fails.
     */
    this.newPrivateKey = function (groupOrJson, exponents) {
        if (typeof groupOrJson !== 'string') {
            validator.checkIsObjectWithProperties(
                groupOrJson, 'Zp subgroup for new ElGamalPrivateKey object');
            validator.checkExponents(
                exponents, 'Exponents for new ElGamalPrivateKey object',
                groupOrJson.q);
            _mathService.checkGroupMatchesPolicy(groupOrJson);

            return new ElGamalPrivateKey(groupOrJson, exponents);
        } else {
            return jsonToPrivateKey(groupOrJson);
        }
    };

    /**
     * Creates a new ElGamalKeyPair object, which encapsulates an ElGamal key
     * pair.
     *
     * @function newKeyPair
     * @memberof ElGamalCryptographyService
     * @param {ElGamalPublicKey}
     *            publicKey The ElGamal public key comprising the key pair.
     * @param {ElGamalPrivateKey}
     *            privateKey The ElGamal private key comprising the key pair.
     * @returns {ElGamalKeyPair} The new ElGamalKeyPair object.
     * @throws {Error}
     *             If the input data validation fails.
     */
    this.newKeyPair = function (publicKey, privateKey) {
        validator.checkElGamalPublicKey(
            publicKey, 'ElGamal public key for new ElGamalKeyPair object');
        validator.checkElGamalPrivateKey(
            privateKey, 'ElGamal private key for new ElGamalKeyPair object');
        _mathService.checkGroupMatchesPolicy(publicKey.group);
        _mathService.checkGroupMatchesPolicy(privateKey.group);
        _mathService.checkGroupArrayMatchesPolicy(publicKey.elements);

        return new ElGamalKeyPair(publicKey, privateKey);
    };

    /**
     * Creates a new ElGamalEncryptedElements object, which encapsulates the
     * encryption or encryption pre-computation of some Zp group elements.
     *
     * @function newEncryptedElements
     * @memberof ElGamalCryptographyService
     * @param {ZpGroupElement|string}
     *            gammaOrJson The gamma Zp group element comprising the
     *            encryption or pre-computation <b>OR</b> a JSON string
     *            representation of an ElGamalEncryptedElements object,
     *            compatible with its <code>toJson</code> method. For the
     *            latter case, any additional input arguments will be ignored.
     * @param {ZpGroupElement[]}
     *            phis The phi Zp group elements comprising the encryption or
     *            pre-computation.
     * @param {Exponent}
     *            [secret] The secret exponent comprising the encryption or
     *            pre-computation. Required when the secret is
     *            needed later for zero-knowledge proof generation.
     * @returns {ElGamalEncryptedElements} The new ElGamalEncryptedElements
     *          object.
     * @throws {Error}
     *             If the input data validation fails.
     */
    this.newEncryptedElements = function (gammaOrJson, phis, secret) {
        if (typeof gammaOrJson !== 'string') {
            _mathService.checkGroupMatchesPolicy(gammaOrJson);
            _mathService.checkGroupArrayMatchesPolicy(phis);
            validator.checkZpGroupElement(
                gammaOrJson,
                'Gamma Zp group element for new ElGamalEncryptedElements object');
            validator.checkZpGroupElements(
                phis,
                'Phi Zp group elements for new ElGamalEncryptedElements object');
            if (typeof secret !== 'undefined') {
                validator.checkExponent(secret, 'ElGamal encryption secret exponent');
            }

            return new ElGamalEncryptedElements(gammaOrJson, phis, secret);
        } else {
            return jsonToEncryptedElements(gammaOrJson);
        }
    };

    /**
     * Creates a new ElGamalEncrypter object for ElGamal encrypting data. It
     * must be initialized with an ElGamal public key.
     *
     * @function newEncrypter
     * @memberof ElGamalCryptographyService
     * @returns {ElGamalEncrypter} The new ElGamalEncrypter object.
     */
    this.newEncrypter = function () {
        return new ElGamalEncrypter(_mathService);
    };

    /**
     * Creates a new ElGamalDecrypter object for ElGamal decrypting data. It
     * must be initialized with an ElGamal private key.
     *
     * @function newDecrypter
     * @memberof ElGamalCryptographyService
     * @returns {ElGamalDecrypter} The new ElGamalDecrypter object.
     */
    this.newDecrypter = function () {
        return new ElGamalDecrypter(_mathService);
    };

    function jsonToPublicKey(json) {
        validator.checkIsJsonString(
            json, 'JSON string to deserialize to ElGamalPublicKey object');

        const parsed = JSON.parse(json).publicKey;

        const p = codec.bytesToBigInteger(
            codec.base64Decode(parsed.zpSubgroup.p.toString()));
        const q = codec.bytesToBigInteger(
            codec.base64Decode(parsed.zpSubgroup.q.toString()));
        const g = codec.bytesToBigInteger(
            codec.base64Decode(parsed.zpSubgroup.g.toString()));
        const group = _mathService.newZpSubgroup(p, q, g);

        const elements = [];
        for (let i = 0; i < parsed.elements.length; i++) {
            const value =
                codec.bytesToBigInteger(codec.base64Decode(parsed.elements[i]));
            const element = _mathService.newZpGroupElement(p, q, value);
            elements.push(element);
        }

        return new ElGamalPublicKey(group, elements);
    }

    function jsonToPrivateKey(json) {
        validator.checkIsJsonString(
            json, 'JSON string representation of ElGamalPrivateKey object');

        const parsed = JSON.parse(json).privateKey;

        const g = codec.bytesToBigInteger(
            codec.base64Decode(parsed.zpSubgroup.g.toString()));
        const p = codec.bytesToBigInteger(
            codec.base64Decode(parsed.zpSubgroup.p.toString()));
        const q = codec.bytesToBigInteger(
            codec.base64Decode(parsed.zpSubgroup.q.toString()));
        const group = _mathService.newZpSubgroup(p, q, g);

        const exponents = [];
        for (let i = 0; i < parsed.exponents.length; i++) {
            const value =
                codec.bytesToBigInteger(codec.base64Decode(parsed.exponents[i]));
            const exponent = _mathService.newExponent(q, value);
            exponents.push(exponent);
        }

        return new ElGamalPrivateKey(group, exponents);
    }

    function jsonToEncryptedElements(json) {
        validator.checkIsJsonString(
            json, 'JSON string representation of ElGamalEncryptedElements object');

        const parsed = JSON.parse(json).ciphertext;

        const p = codec.bytesToBigInteger(codec.base64Decode(parsed.p));
        const q = codec.bytesToBigInteger(codec.base64Decode(parsed.q));

        const gammaFromJson = _mathService.newZpGroupElement(
            p, q, codec.bytesToBigInteger(codec.base64Decode(parsed.gamma)));

        const phisFromJson = [];
        for (let i = 0; i < parsed.phis.length; i++) {
            phisFromJson.push(_mathService.newZpGroupElement(
                p, q, codec.bytesToBigInteger(codec.base64Decode(parsed.phis[i]))));
        }

        return new ElGamalEncryptedElements(gammaFromJson, phisFromJson);
    }
}
