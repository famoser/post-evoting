/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

module.exports = function ($rootScope) {
	'ngInject';

	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$rootScope.pageInfo = attrs.setPageInfo;
		},
	};
};
