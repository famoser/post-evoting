/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true, expr:true */
'use strict';

const { assert, expect } = require('chai');

const mathematicalService = require('../../src/mathematical').newService();

describe('The random generator', function () {
	const sut = mathematicalService.newRandomGenerator();

	it('should fail on wrong prime certainty values', function () {
        expect(function (done) {
            sut.nextQuadraticResidueGroup(1024, 79);
            done();
        }).to.throw();

        expect(function (done) {
            sut.nextQuadraticResidueGroup(2048, 111);
            done();
        }).to.throw();

        expect(function (done) {
            sut.nextQuadraticResidueGroup(4096, 127);
            done();
        }).to.throw();
    });

    it('should produce quadratic residue groups', function () {
        assert.exists(sut.nextQuadraticResidueGroup(16, 80));
    });
});
