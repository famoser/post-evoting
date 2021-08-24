/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global _ */
/* global OV */

describe('Ballot parser api', function () {
	'use strict';

	const response = require('./mocks/ballot.json');

	it('should parse a ballot', function () {
		expect(OV).toBeDefined();
		expect(response).toBeDefined();

		const ballot = OV.BallotParser.parseBallot(response.ballot);

		expect(ballot.correctnessIds).toEqual({
			10009: ['11ea5166652f492180f819c7d804ba68'],
			10037: ['11ea5166652f492180f819c7d804ba68'],
			10039: ['11ea5166652f492180f819c7d804ba68'],
			10069: ['11ea5166652f492180f819c7d804ba68'],
			100003: ['11ea5166652f492180f819c7d804ba68'],
			100019: ['11ea5166652f492180f819c7d804ba68'],
			100103: ['11ea5166652f492180f819c7d804ba68'],
			100109: ['11ea5166652f492180f819c7d804ba68'],
			100129: ['11ea5166652f492180f819c7d804ba68'],
			100153: ['11ea5166652f492180f819c7d804ba68'],
			100169: ['11ea5166652f492180f819c7d804ba68'],
			100183: ['11ea5166652f492180f819c7d804ba68'],
			100193: ['11ea5166652f492180f819c7d804ba68'],
			100207: ['11ea5166652f492180f819c7d804ba68'],
			100237: ['11ea5166652f492180f819c7d804ba68'],
			100267: ['11ea5166652f492180f819c7d804ba68'],
			100297: ['11ea5166652f492180f819c7d804ba68'],
			100343: ['11ea5166652f492180f819c7d804ba68'],
			100363: ['11ea5166652f492180f819c7d804ba68'],
			100379: ['11ea5166652f492180f819c7d804ba68'],
			100393: ['11ea5166652f492180f819c7d804ba68'],
			100417: ['11ea5166652f492180f819c7d804ba68'],
			100523: ['11ea5166652f492180f819c7d804ba68'],
			100549: ['11ea5166652f492180f819c7d804ba68'],
			100559: ['11ea5166652f492180f819c7d804ba68'],
			100613: ['11ea5166652f492180f819c7d804ba68'],
			100673: ['11ea5166652f492180f819c7d804ba68'],
			100693: ['11ea5166652f492180f819c7d804ba68'],
			100699: ['11ea5166652f492180f819c7d804ba68'],
			100703: ['11ea5166652f492180f819c7d804ba68'],
			100733: ['11ea5166652f492180f819c7d804ba68'],
			100741: ['11ea5166652f492180f819c7d804ba68'],
			100747: ['11ea5166652f492180f819c7d804ba68'],
			100769: ['11ea5166652f492180f819c7d804ba68'],
			100787: ['11ea5166652f492180f819c7d804ba68'],
			100799: ['11ea5166652f492180f819c7d804ba68'],
			100823: ['11ea5166652f492180f819c7d804ba68'],
		});
	});
});
