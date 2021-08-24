/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const paths = require('./paths.json');
const babelify = require('babelify');

module.exports = function (config) {

	config.set({

		basePath: '',

		frameworks: ['browserify', 'jasmine'],

		// Possible browsers: 'FirefoxHeadless', 'ChromeHeadless', 'ChromeHeadlessNoSandbox'
		// NOTE: For "Windows Subsystem for Linux", only 'ChromeHeadlessNoSandbox' is presently
		// available, and environment variable CHROME_BIN must be set to the Windows Google Chrome
		// executable path. See the "readme.md" file for details.
		browsers: ['FirefoxHeadless'],
		customLaunchers: {
			ChromeHeadlessNoSandbox: {
				base: 'ChromeHeadless',
				flags: ['--no-sandbox']
			}
		},

		files: [
			paths.js.entryFile,
			`${paths.unitTests.baseDir}/mocks/testdata.js`,
			`${paths.unitTests.baseDir}/proofs.spec.js`,
		],

		preprocessors: {
			[paths.js.entryFile]: ['browserify'],
			[`${paths.unitTests.baseDir}/**/*.js`]: ['browserify'],
		},

		browserify: {
			debug: true,
			transform: [
				babelify.configure({
					presets: ["@babel/preset-env"],
					ignore: [/(node_modules)/],
					sourceMaps: true,
				}),
			]
		},

		reporters: ['spec'],

		singleRun: true,

		browserDisconnectTolerance: 1,
		browserDisconnectTimeout: 100000,
		browserNoActivityTimeout: 100000,
		captureTimeout: 100000,

		// level of logging
		// possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN
		//                  || config.LOG_INFO || config.LOG_DEBUG
		logLevel: config.LOG_INFO,

	});

};
