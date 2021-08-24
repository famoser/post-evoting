/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

/**
 * Defines an equality method for Array objects. Only intended for internal use.
 *
 * @function equals
 * @private
 * @param {Array}
 *            array The Array object that should be checked against this Array
 *            object for equality.
 * @param {boolean}
 *            [checkOrder=false] If <code>true</code>, checks if the element
 *            order is the same in both Array objects.
 * @returns {boolean} <code>true</code> if the equality holds,
 *          <code>false</code> otherwise.
 */
Array.prototype.equals = function (array, checkOrder) {
	if (!array) {
		return false;
	}

	if (arguments.length === 1) {
		checkOrder = true;
	}

	if (this.length !== array.length) {
		return false;
	}

	for (let i = 0; i < this.length; i++) {
		if (this[i] instanceof Array && array[i] instanceof Array) {
			if (!this[i].equals(array[i], checkOrder)) {
				return false;
			}
		} else if (checkOrder && (!this[i].equals(array[i]))) {
			return false;
		} else if (!checkOrder) {
			return this.sort().equals(array.sort(), true);
		}
	}
	return true;
};
