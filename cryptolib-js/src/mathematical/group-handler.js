/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const ZpGroupElement = require('./zp-group-element');
const validator = require('../input-validator');
const forge = require('node-forge');

const BigInteger = forge.jsbn.BigInteger;

module.exports = MathematicalGroupHandler;

/**
 * @class MathematicalGroupHandler
 * @classdesc The mathematical group handler API. To instantiate this object,
 *            use the method {@link MathematicalService.newGroupHandler}.
 * @hideconstructor
 */
function MathematicalGroupHandler() {
    // Empty constructor
}

MathematicalGroupHandler.prototype = {
    /**
     * Exponentiates an array of Zp group elements with the exponent provided as
     * input.
     * <p>
     * The array of Zp group elements must only contain members of the Zp
     * subgroup provided as input. Otherwise, an error will be thrown.
     *
     * @function exponentiateElements
     * @memberof MathematicalGroupHandler
     * @param {ZpSubgroup}
     *            group The Zp subgroup to which the elements must belong.
     * @param {ZpGroupElement[]}
     *            elements The array of Zp group elements to exponentiate.
     * @param {Exponent}
     *            exponent The exponent to use for the exponentiation
     * @param {boolean}
     *            validate If set to true, then the elements are validated
     * @return {ZpGroupElement[]} The array of exponentiated group elements.
     * @throws {Error}
     *             If the input data or group membership validation fails.
     */
    exponentiateElements: function (group, elements, exponent, validate) {
        validator.checkIsObjectWithProperties(
            group, 'Zp subgroup of elements to exponentiate', group);
        validator.checkZpGroupElements(
            elements, 'Zp group elements to exponentiate', group);
        validator.checkExponent(
            exponent, 'Exponent to use for exponentiating Zp group elements',
            group.q);
        if (typeof validate !== 'undefined') {
            validator.checkIsDefinedAndNotNull(
                validate, 'Zp group element exponentiation "validate" flag');
            validator.checkIsType(
                validate, 'boolean',
                'Zp group element exponentiation "validate" flag');
        }

        if (validate) {
            for (let i = 0; i < elements.length; i++) {
                if (!group.isGroupMember(elements[i])) {
                    throw new Error(
                        'Found Zp group element with value: ' + elements[i].value +
                        ' that does not belong to Zp subgroup provided as input: p = ' +
                        group.p + ' ; q = ' + group.q);
                }
            }
        }

		const exponentiatedElements = [];
		for (let j = 0; j < elements.length; j++) {
            exponentiatedElements.push(elements[j].exponentiate(exponent));
        }

        return exponentiatedElements;
    },

    /**
     * Divides the elements of one array of Zp group elements by the
     * corresponding members of another array of Zp group elements and creates a
     * new array of Zp group elements from the results of this operation.
     * <p>
     * The two arrays must only contain elements that belong to the Zp subgroup
     * group that this divider operates on. If either array contains any element
     * that is does not belong to this Zp subgroup, then an Error will be
     * thrown.
     *
     * @function divideElements
     * @memberof MathematicalGroupHandler
     * @param {ZpGroupElement[]}
     *            dividendElements The array of Zp group elements to act as the
     *            <code>dividend</code>.
     * @param {ZpGroupElement[]}
     *            divisorElements The array Zp group elements to act as the
     *            <code>divisor</code>.
     * @returns {ZpGroupElement[]} The array of <code>quotient</code> Zp group
     *          elements resulting from this operation.
     * @throws {Error}
     *             If the input or group membership validation fails.
     */
    divideElements: function (dividendElements, divisorElements) {
        checkDivisionData(dividendElements, divisorElements);

		const quotientArray = [];
		let quotient;
		for (let i = 0; i < dividendElements.length; i++) {
            quotient = dividendElements[i].multiply(divisorElements[i].invert());
            quotientArray.push(quotient);
        }

        return quotientArray;
    },

    /**
     * Iterates through an array of Zp group elements, checking if each element
     * is a member of the Zp subgroup provided as input.
     * <p>
     * If any element is found that is not a member of the Zp subgroup, then an
     * Error will be thrown.
     *
     * @function checkGroupMembership
     * @memberof MathematicalGroupHandler
     * @param {ZpSubgroup}
     *            group The Zp subgroup to which the Zp group elements provided
     *            as input must belong.
     * @param {ZpGroupElement[]}
     *            elements The array of Zp group elements to check for group
     *            membership.
     * @throws {Error}
     *             If the input validation fails or any Zp group element in the
     *             array fails the group membership check.
     */
    checkGroupMembership: function (group, elements) {
        validator.checkIsObjectWithProperties(
            group, 'Zp subgroup for group membership check');
        validator.checkZpGroupElements(
            elements, 'Zp group elements for group membership check');

        for (let i = 0; i < elements.length; i++) {
            if (!group.isGroupMember(elements[i])) {
                throw new Error(
                    'Zp group element with value: ' + elements[i].value +
                    ' is not a member of the Zp subgroup with modulus p: ' + group.p +
                    ' and order q: ' + group.q);
            }
        }
    },

    /**
     * Iterates through an array of Zp group element values, checking if each
     * value is a member of the Zp subgroup provided as input. If a given value
     * is a member of the subgroup, it is encapsulated in a ZpGroupElement
     * object and added to an output array. The method stops iterating once it
     * has found the number of Zp subgroup members provided as input. At that
     * point, the output array containing all of the members found is returned.
     * <p>
     * If the required number of Zp subgroup members are not found then an Error
     * will be thrown.
     *
     * @function extractGroupMembers
     * @memberof MathematicalGroupHandler
     * @param {ZpSubgroup}
     *            group The Zp subgroup to which the output Zp group elements
     *            must belong.
     * @param {forge.jsbn.BigInteger[]}
     *            values The array of Zp group element values from which to
     *            obtain the members.
     * @param {number}
     *            numMembersRequired The required number of Zp subgroup members
     *            to be returned.
     * @returns {ZpGroupElement[]} The array of requested Zp subgroup members.
     * @throws {Error}
     *             If the input validation fails or the required number of Zp
     *             subgroup members was not found.
     */
    extractGroupMembers: function (group, values, numMembersRequired) {
        checkExtractionData(values, group, numMembersRequired);

		const outputArray = [];
		let membersFound = 0;
		let candidate;

		for (let i = 0; i < values.length; i++) {
            candidate = new ZpGroupElement(group.p, group.q, values[i]);

            if (group.isGroupMember(candidate)) {
                outputArray.push(candidate);
                membersFound++;
            }

            if (membersFound === numMembersRequired) {
                return outputArray;
            }
        }

        if (membersFound !== numMembersRequired) {
            throw new Error(
                'Did not find the required number of group members in the given list. The required number of was ' +
                numMembersRequired + ', number of members found was  ' +
                membersFound);
        }
    },

    /**
     * Finds the smallest generator for a given Zp subgroup.
     * <p>
     * This method starts with a candidate generator element of value 2 and
     * checks if this element is a group member. If that element is a group
     * member then it is returned. Otherwise, the candidate element value is
     * incremented by 1 the check is performed again. This process continues
     * until a generator is found or the candidate element value equals the p
     * parameter of the Zp subgroup, in which case an Error is thrown.
     *
     * @function findMinGenerator
     * @memberof MathematicalGroupHandler
     * @param {forge.jsbn.BigInteger}
     *            p The modulus of the Zp subgroup.
     * @param {forge.jsbn.BigInteger}
     *            q The order of the Zp subgroup.
     * @returns {forge.jsbn.BigInteger} The smallest generator.
     * @throws {Error}
     *             If the input data validation failes or a generator cannot be
     *             found.
     */
    findMinGenerator: function (p, q) {
        validator.checkZpSubgroupModulus(
            p, 'Modulus p of Zp subgroup for which to find smallest generator');
        validator.checkZpSubgroupOrder(
            q, 'Order q of Zp subgroup for which to find smallest generator', p);

		let g = new BigInteger('2');
		let generatorFound = false;

		while ((!generatorFound) && (g.compareTo(p) < 0)) {
            if (g.modPow(q, p).equals(BigInteger.ONE)) {
                generatorFound = true;
            } else {
                g = g.add(BigInteger.ONE);
            }
        }

        if (!generatorFound) {
            throw new Error(
                'Failed to find a generator for modulus p: ' + p +
                ' and order q: ' + q);
        }

        return g;
    }
};

function checkDivisionData(dividendElements, divisorElements) {
    validator.checkZpGroupElements(
        dividendElements,
        'Dividend Zp group elements to use for element array division operation');
    validator.checkZpGroupElements(
        divisorElements,
        'Divisor Zp group elements to use for element array division operation');

	const numDividendElements = dividendElements.length;
	const numDivisorElements = divisorElements.length;
	if (numDividendElements !== numDivisorElements) {
        throw new Error(
            'Expected number of divisor elements to equal number of dividend elements: ' +
            numDividendElements + ' ; Found: ' + numDivisorElements);
    }
}

function checkExtractionData(values, group, numMembersRequired) {
    validator.checkIsNonEmptyArray(
        values, 'Zp group element values from which to extract group members');
    validator.checkIsObjectWithProperties(
        group, 'Zp subgroup to which extracted members are to belong');
    validator.checkIsPositiveNumber(
        numMembersRequired,
        'Number of group members required by extraction process');

    if (numMembersRequired > values.length) {
        throw new Error(
            'Required number of group members provided as input: ' +
            numMembersRequired +
            ' is greater then the number of element values provided as input: ' +
            values.length);
    }
}
