/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

const istanbul = require('browserify-istanbul');

module.exports = function (config) {
	'use strict';
	config.set({
		basePath: '.',
		singleRun: true,
		logLevel: config.LOG_INFO,
		browserNoActivityTimeout: 5 * 60 * 1000,
		browserDisconnectTimeout: 60 * 1000,
		frameworks: ['browserify', 'jasmine'],
		files: [
			'zkproof/**/*.js'
		],
		browsers: ['ChromeHeadlessNoSandbox'],
		customLaunchers: {
			ChromeHeadlessNoSandbox: {
				base: 'ChromeHeadless',
				flags: ['--no-sandbox']
			}
		},
		preprocessors: {
			'../src/zkproof/**/*.js': ['browserify'], 'zkproof/**/*.js': ['browserify'],
		},
		browserify: {
			debug: true,
			transform: [
				[
					istanbul({
						ignore: ['**/node_modules/**'],
					}),
				]
			],
			extensions: ['.js'],
		},
		reporters: ['progress', 'coverage'],
		coverageReporter: {
			reporters: [
				{
					type: 'json',
					dir: '../coverage',
					subdir: 'unit-run',
					file: 'unit-run-2.json'
				}
			],
		},
	});
};
