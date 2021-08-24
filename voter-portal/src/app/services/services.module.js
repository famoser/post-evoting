/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

module.exports = angular
	.module('app.services', [])

	.service('i18n', require('./i18n'))
	.factory('precomputer', require('./precomputer'))
	.factory('searchService', require('./search'))
	.factory('sessionService', require('./session')).name;
