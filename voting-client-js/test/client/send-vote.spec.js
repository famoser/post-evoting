/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global _ */
/* global OV */

describe('Send vote', function () {
	'use strict';

	const response = require('../parsers/mocks/ballot_with_writeins.json');

	it('send a vote', function () {
		expect(OV).toBeDefined();
		expect(response).toBeDefined();

		OV.BallotParser.parseBallot(response.ballot);
	});
});
