/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

module.exports = function (ContestService, gettext) {
	'ngInject';

	// Called by vote correctness function for each error found in the contest
	// e.g. if contest has 2 errors, the onContestInvalid function will be called twice
	const onContestInvalid = function (errorKey, violatorId) {
		const contest = this;
		let invalidQuestion;

		switch (errorKey) {
			case 'MIN_ERROR':
			case 'MIN_NON_BLANK_ERROR':
				if (contest.errors.length === 0) {
					contest.errors.push({
						message: gettext(
							'The selection is not completed. Make sure you choose an answer for the highlighted questions.',
						),
						type: 'warning',
					});
				}

				invalidQuestion = contest.questions.find(q => q.id === violatorId);

				if (invalidQuestion) {
					invalidQuestion.error = true;
				}

				break;

			// case 'MULTIPLE_SELECTION_ERROR':
			// break;
			default:
				console.log(`Unhandled validation: ${errorKey} for ${violatorId}`);
				break;
		}
	};

	const validate = contest => {
		contest.error = false;
		contest.errors = [];

		contest.questions.forEach(q => {
			q.error = false;
		});
	};


	return {
		initialize: function () {
			// Empty default function
		},
		onContestInvalid,
		validate,
	};
};
