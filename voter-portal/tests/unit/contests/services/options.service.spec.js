/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

describe('Options Service', () => {

	let ContestService;
	let OptionsService;
	let contest;

	const contestInitialState = require('../mocks/options.json');

	beforeEach(() => {

		angular.mock.module('app.services');
		angular.mock.module('app.contests');

	});

	beforeEach(inject((_ContestService_, _OptionsService_) => {

		ContestService = _ContestService_;
		OptionsService = _OptionsService_;

		contest = angular.copy(contestInitialState);

	}));

	it('should be defined', () => {

		expect(OptionsService).toBeDefined();

	});

	describe('onContestInvalid()', () => {

		let onContestInvalid;

		beforeEach(() => {

			spyOn(console, 'log');

			contest.errors = [];
			onContestInvalid = OptionsService.onContestInvalid.bind(contest);

		});

		it('logs to console if the passed error key is not treated', () => {

			onContestInvalid('SOME_ERROR', 'SOME_ID');

			expect(console.log).toHaveBeenCalled();
			expect(console.log.calls.argsFor(0)[0]).toContain('SOME_ERROR');
			expect(console.log.calls.argsFor(0)[0]).toContain('SOME_ID');

		});

		it('adds error to contest if it was called with MIN_ERROR', () => {

			onContestInvalid('MIN_ERROR');

			expect(contest.errors.length).toBe(1);

		});

		it('adds error to contest if it was called with MIN_NON_BLANK_ERROR', () => {

			onContestInvalid('MIN_NON_BLANK_ERROR');

			expect(contest.errors.length).toBe(1);

		});

	});

});
