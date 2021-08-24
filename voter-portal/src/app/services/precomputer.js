/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

module.exports = function ($q) {
	'ngInject';

	let deferred = null;

	const start = function (serializedEncParams) {
		deferred = $q.defer();

		ovWorker
			.precomputeEncrypterValues(serializedEncParams)
			.then(function (encrypterValues) {
				ovWorker
					.precomputeProofValues(
						serializedEncParams,
						encrypterValues
					)
					.then(function (proofValues) {
						deferred.resolve({
							encrypterValues: encrypterValues,
							proofValues: proofValues,
						});
					});
			});
	};

	const whenReady = function () {
		if (!deferred) {
			throw new Error('Precomputations have not been started');
		}

		return deferred.promise;
	};

	return {
		start: start,
		whenReady: whenReady,
	};
};
