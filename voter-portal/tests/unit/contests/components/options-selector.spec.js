/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

const {getCompiledDirective} = require('../../helpers');

describe('options-selector component', () => {

	let ContestService;
	let $rootScope;
	let scope;

	/**
	 * Init module
	 */
	beforeEach(angular.mock.module('app.contests'));

	const directive = getCompiledDirective('<option-selector option="question">');

	/**
	 * Inject dependencies
	 */
	beforeEach(inject((_$rootScope_, _ContestService_) => {

		ContestService = _ContestService_;
		$rootScope = _$rootScope_;
		scope = $rootScope.$new();

		scope.question = {};

	}));

	/**
	 * Controller
	 */
	describe('resetRadio()', () => {

		it('removes the chosen option from the question', () => {

			scope.question.chosen = '1';

			const elementUT = directive(scope);
			const isolatedScope = elementUT.isolateScope();

			isolatedScope.resetRadio();

			expect(scope.question.chosen)
				.toBe('');

		});

	});

	describe('precompute()', () => {

		it('precomputes partial choice codes for a given option', () => {

			spyOn(ContestService, 'precomputePartialChoiceCode');

			const elementUT = directive(scope);
			const isolatedScope = elementUT.isolateScope();
			const option = {};

			isolatedScope.precompute(option);

			expect(ContestService.precomputePartialChoiceCode)
				.toHaveBeenCalledWith(option);

		});

	});

});
