/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

module.exports = function (ContestService) {
	'ngInject';

	return {
		restrict: 'EA',
		scope: {
			question: '=option',
		},
		templateUrl: 'contests/components/option-selector.tpl.html',
		link: function (scope) {
			scope.resetRadio = function () {
				scope.question.chosen = '';
			};

			scope.precompute = function (option) {
				ContestService.precomputePartialChoiceCode(option);
			};
		},
	};
};
