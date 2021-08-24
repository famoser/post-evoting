/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const validator = require('../input-validator');

module.exports = MathematicalArrayCompressor;

/**
 * @class MathematicalArrayCompressor
 * @classdesc The mathematical array compressor API. To instantiate this object,
 *            use the method {@link MathematicalService.newArrayCompressor}.
 * @hideconstructor
 */
function MathematicalArrayCompressor() {
	// Empty constructor
}

MathematicalArrayCompressor.prototype = {
    /**
     * Compresses an array of Zp group elements into a single element.
     *
     * @function compressZpGroupElements
     * @memberof MathematicalArrayCompressor
     * @param {ZpGroupElement[]}
     *            elements The array of Zp group elements to compress.
     * @returns {ZpGroupElement} The Zp group element into which all of the Zp
     *          group elements have been compressed.
     * @throws {Error}
     *             If the input validation fails.
     */
    compressZpGroupElements: function (elements) {
        validator.checkZpGroupElements(elements, 'Zp group elements to compress');

		let element = elements[0];
		for (let i = 1; i < elements.length; i++) {
            element = element.multiply(elements[i]);
        }

        return element;
    },

    /**
     * Compresses a specified number of trailing elements of an array of Zp
     * group elements into a single Zp group element.
     *
     * @function compressTrailingZpGroupElements
     * @memberof MathematicalArrayCompressor
     * @param {ZpGroupElement[]}
     *            elements The array of Zp group elements to compress.
     * @param {number}
     *            numOutputElements The number of output elements required.
     * @returns {ZpGroupElement[]} The array of Zp group elements resulting from
     *          the compression.
     * @throws {Error}
     *             If the input validation fails.
     */
    compressTrailingZpGroupElements: function (elements, numOutputElements) {
        validator.checkZpGroupElements(
            elements,
            'Zp group element array for which to compress trailing elements');
        validator.checkIsPositiveNumber(
            numOutputElements,
            'Number of elements to remain after compressing trailing elements');

		const offset = numOutputElements - 1;
		const outputElements = elements.slice(0, offset);

		const lastElement =
			this.compressZpGroupElements(elements.slice(offset, elements.length));

		outputElements.push(lastElement);

        return outputElements;
    },

    /**
     * Compresses an array of exponents into a single exponent.
     *
     * @function compressExponents
     * @memberof MathematicalArrayCompressor
     * @param {Exponent[]}
     *            exponents The array of exponents to compress.
     * @returns {Exponent} The exponent into which all of the exponents have
     *          been compressed.
     * @throws {Error}
     *             If the input validation fails.
     */
    compressExponents: function (exponents) {
        validator.checkExponents(exponents, 'Exponents to compress');

		let exponent = exponents[0];
		for (let i = 1; i < exponents.length; i++) {
            exponent = exponent.add(exponents[i]);
        }

        return exponent;
    },

    /**
     * Compresses a specified number of trailing exponents of an array of
     * exponents into a single exponent.
     *
     * @function compressTrailingExponents
     * @memberof MathematicalArrayCompressor
     * @param {Exponent[]}
     *            exponents The array of exponents to compress.
     * @param {number}
     *            numOutputExponents The number of output exponents required.
     * @returns {Exponent[]} The array of exponents resulting from the
     *          compression.
     * @throws {Error}
     *             If the input validation fails.
     */
    compressTrailingExponents: function (exponents, numOutputExponents) {
        validator.checkExponents(
            exponents, 'Exponent array for which to compress trailing exponents');
        validator.checkIsPositiveNumber(
            numOutputExponents,
            'Number of exponents to remain after compressing trailing exponents');

		const offset = numOutputExponents - 1;
		const outputExponents = exponents.slice(0, offset);

		const lastExponent =
			this.compressExponents(exponents.slice(offset, exponents.length));

		outputExponents.push(lastExponent);

        return outputExponents;
    }
};
