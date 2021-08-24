/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

const _ = require('lodash');

module.exports = function (
	ContestService,
	ListService,
	CandidateService,
	gettext,
) {
	'ngInject';

	let _this = {};

	const initialize = contest => {
		// If displayLists exists it means the contest was previously initialized
		// and now the reinitialization is triggered by "change vote"
		if (!contest.displayedLists) {
			contest.displayedLists = [];
		}

		// If selectedCandidates exists it means the contest was previously initialized
		// and now the reinitialization is triggered by "change vote"
		if (!contest.selectedCandidates) {
			contest.selectedCandidates = [];

			_.each(contest.lists, list => {
				ListService.mapDetailsToModel(list);

				_.each(list.candidates, candidate => {
					CandidateService.mapDetailsToModel(candidate);
					candidate.chosen = 0;
				});

				list.candidatesSummary = ListService.getCandidatesSummary(list);
			});
		}
	};

	// Called by vote correctness function for each error found in the contest
	// e.g. if contest has 2 errors, the onContestInvalid function will be called twice
	const onContestInvalid = function (errorKey, violatorId) {
		const contest = this;

		switch (errorKey) {
			case 'MIN_ERROR':
			case 'MIN_NON_BLANK_ERROR':
				contest.errors.push({
					message: gettext(
						'The selection is not completed. Make sure you choose at least {{min}} candidate/s.',
					),
					params: {min: contest.candidatesQuestion.minChoices},
					type: 'warning',
				});
				break;

			default:
				console.log(`Unhandled validation: ${errorKey} for ${violatorId}`);
				break;
		}
	};

	const validate = contest => {
		contest.error = false;
		contest.errors = [];

		if (contest.selectedCandidates.length === 0) {
			if (!contest.allowFullBlank) {
				contest.errors.push({
					message: gettext(
						'Blank vote is not allowed. Please make your choice from the options provided below.',
					),
					type: 'warning',
				});
			}
		}

		_this._fillEmptiesWithBlanks(contest);
	};

	const addSelectedListToContest = list => {
		if (!list) {
			return;
		}

		const contest = list.parent;

		ContestService.clearCandidates(contest);

		contest.displayedLists.length = 0;
		contest.displayedLists.push(list);

		ListService.addListCandidatesToContest(
			list,
			ContestService.precomputePartialChoiceCode,
		);

		return {
			contestId: contest.id,
			listId: list.getQualifiedId(),
		};
	};

	const clearList = contest => {
		contest.displayedLists.length = 0;

		ContestService.clearCandidates(contest);
	};

	const _fillEmptiesWithBlanks = contest => {
		for (let i = 0; i < contest.candidatesQuestion.maxChoices; i++) {
			if (!contest.selectedCandidates[i]) {
				ContestService.addBlankCandidate(contest, i);
			}
		}
	};

	_this = {
		initialize,
		validate,
		onContestInvalid,
		addSelectedListToContest,
		clearList,
		_fillEmptiesWithBlanks,
	};

	return _this;
};
