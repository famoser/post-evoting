/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

module.exports = function () {
	'ngInject';

	return {
		restrict: 'E',
		link: function (scope, element) {
			scope.dissmissToast = function () {
				if (scope.errors) {
					scope.errors.alertClosed = true;
				}
			};
		},
	};
};
