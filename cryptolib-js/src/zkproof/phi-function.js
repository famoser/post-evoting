/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

module.exports = PhiFunction;

/**
 * @class PhiFunction
 * @classdesc Encapsulates the PHI function defined by Maurer's unified
 *            framework for zero-knowledge proofs of knowledge. This object is
 *            instantiated internally by the <code>prove</code> method of any
 *            zero-knowledge proof of knowledge prover object or the
 *            <code>verify</code> method of any zero-knowledge proof of
 *            knowledge verifier object.
 * @private
 * @param {number}
 *            numInputs The number of inputs of the PHI function.
 * @param {number}
 *            numOutputs The number of outputs of the PHI function.
 * @param {number[][][]}
 *            computationRules The computation rules of the PHI function.
 * @param {ZpGroupElement[]}
 *            baseElements The base elements of the PHI function.
 */
function PhiFunction(numInputs, numOutputs, computationRules, baseElements) {
	validateComputationRules(
		numInputs, numOutputs, computationRules, baseElements);

	this.numInputs = numInputs;
	this.numOutputs = numOutputs;
	this.computationRules = computationRules;
	Object.freeze(this.computationRules);
	this.baseElements = baseElements;
	Object.freeze(this.baseElements);

	function validateComputationRules(
		numSecrets, numOutputs, computationRules, baseElements) {
		// Validate that both of the values in each of the
		// pairs of rules have values in the correct ranges.
		const numBaseElements = baseElements.length;
		for (let i = 0; i < numOutputs; i++) {
			for (let j = 0; j < computationRules[i].length; j++) {
				// Validate the first value of the pair
				const pairValue1 = computationRules[i][j][0];
				if ((pairValue1 < 1) || (pairValue1 > numBaseElements)) {
					throw new Error(
						'First value in index pair is invalid; It should be within the range [1, ' +
						numBaseElements + '], but it was ' + pairValue1);
				}

				// Validate the second value of the pair.
				const pairValue2 = computationRules[i][j][1];
				if ((pairValue2 < 1) || (pairValue2 > numSecrets)) {
					throw new Error(
						'Second value in index pair is valid; It should be within the range [1, ' +
						numSecrets + '], but it was ' + pairValue2);
				}
			}
		}
	}
}

PhiFunction.prototype = {
	/**
	 * Performs the PHI function calculation.
	 *
	 * @function calculate
	 * @memberof PhiFunction
	 * @param {Exponent[]}
	 *            exponents The exponents with which to exponentiate the base
	 *            elements.
	 * @returns {ZpGroupElement[]} The array of Zp group elements that comprise
	 *          the output of the PHI function calculation.
	 */
	calculate: function (exponents) {
		let partialResult;
		let resultForThisListOfPairs;
		const result = [];
		for (let i = 0; i < this.computationRules.length; i++) {
			const numPairsInList = this.computationRules[i].length;

			resultForThisListOfPairs =
				this.baseElements[this.computationRules[i][0][0] - 1].exponentiate(
					exponents[this.computationRules[i][0][1] - 1]);

			for (let j = 1; j < numPairsInList; j++) {
				let index1 = this.computationRules[i][j][0];
				index1 = index1 - 1;
				let index2 = this.computationRules[i][j][1];
				index2 = index2 - 1;

				partialResult =
					this.baseElements[index1].exponentiate(exponents[index2]);

				resultForThisListOfPairs =
					resultForThisListOfPairs.multiply(partialResult);
			}
			result.push(resultForThisListOfPairs);
		}

		return result;
	}
};
