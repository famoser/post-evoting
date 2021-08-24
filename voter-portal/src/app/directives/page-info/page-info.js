/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

module.exports = function () {
	'ngInject';

	return {
		restrict: 'A',
		template: '<span>{{pageInfo}}</span>',
	};
};
