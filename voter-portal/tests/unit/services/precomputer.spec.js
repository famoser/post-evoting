/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

describe('precomputer service', () => {

	let $rootScope;
	let precomputer;
	let $q;

	let precomputeEncrypterValuesPromise;
	let precomputeProofValuesPromise;

	beforeEach(angular.mock.module('app.services'));

	beforeEach(inject((_precomputer_, _$q_, _$rootScope_) => {

		$q = _$q_;
		$rootScope = _$rootScope_;
		precomputeEncrypterValuesPromise = $q.defer();
		precomputeProofValuesPromise = $q.defer();

		window.ovWorker = {
			precomputeEncrypterValues: jasmine.createSpy('precomputeEncrypterValues'),
			precomputeProofValues: jasmine.createSpy('precomputeProofValues'),
		};

		window.ovWorker.precomputeEncrypterValues
			.and.returnValue(precomputeEncrypterValuesPromise.promise);
		window.ovWorker.precomputeProofValues
			.and.returnValue(precomputeProofValuesPromise.promise);

		precomputer = _precomputer_;

	}));

	describe('start()', () => {

		it('calls precomputeEncrypterValues', () => {

			precomputer.start('serializedEncParams');

			expect(window.ovWorker.precomputeEncrypterValues)
				.toHaveBeenCalledWith('serializedEncParams');

		});

		it('calls precomputeProofValues when precomputeEncrypterValues resolves', () => {

			precomputer.start('serializedEncParams');

			precomputeEncrypterValuesPromise.resolve('encrypterValues');

			$rootScope.$apply();

			expect(window.ovWorker.precomputeProofValues).toHaveBeenCalledWith(
				'serializedEncParams',
				'encrypterValues'
			);

		});

	});

	describe('whenReady()', () => {

		it('throws an error if start was not called', () => {

			expect(() => {

				precomputer.whenReady();

			}).toThrowError();

		});

		it('resolves when start resolves', () => {

			precomputer.start('serializedEncParams');

			precomputeEncrypterValuesPromise.resolve('encrypterValues');
			precomputeProofValuesPromise.resolve('proofValues');

			$rootScope.$apply();

			precomputer.whenReady().then((result) => {

				expect(result).toEqual({
					encrypterValues: 'encrypterValues',
					proofValues: 'proofValues',
				});

			});

		});

	});

});
