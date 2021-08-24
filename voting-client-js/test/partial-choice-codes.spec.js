/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
const forge = require('node-forge');
/* global OV */
/* jshint maxlen: 6666 */
/* global TD */

const mathematicalService = require('cryptolib-js/src/mathematical/index').newService();

describe('Partial choice code api', function () {
	'use strict';

	const TD = require('./mocks/testdata.json');

	it('should generate partial choice codes', function () {
		const encParms = OV.parseEncryptionParams(TD.authResponse);

		const options = [
			mathematicalService.newZpGroupElement(
				encParms.p,
				encParms.q,
				new forge.jsbn.BigInteger('3'),
			),
			mathematicalService.newZpGroupElement(
				encParms.p,
				encParms.q,
				new forge.jsbn.BigInteger('5'),
			),
			mathematicalService.newZpGroupElement(
				encParms.p,
				encParms.q,
				new forge.jsbn.BigInteger('7'),
			),
		];

		const exponent = new forge.jsbn.BigInteger('34987025184313239');

		const partialChoiceCodes = OV.generatePartialChoiceCodes(
			options,
			encParms,
			exponent,
		);
		expect(partialChoiceCodes.length).toBe(3);
		expect(partialChoiceCodes[0].toString()).toBe('99867357963399228511326981172633387934698474129050174883278715125828874621216036310708781246' +
			'98925687253367283823397728207754813164342128784023826179105290512290203109575998512961942343360028979832215626996038117069354453410501' +
			'52246546721998979129116666611779935295246258225119194154721369129766073612460286398270611204621632383163083935887661693457083674274939' +
			'03896174569752892794323251100762005602433150904905386417850819849310484466162379496330362650411717492718660159335820482884506866151625' +
			'69657351442943677074009524648636723992338244688398041241450246405650607021177240658360094549239016220972024713542883920155');
		expect(partialChoiceCodes[1].toString()).toBe('13442590789211861092929308976204638195468178767664662841113321796863283046047543918693427439' +
			'57392390423366057890700955590039216502298779605129222988274283912428839896867501728399195548197507816969253474996347908040378401293457' +
			'75143696617284127285585799727778838460419053166134033176574561723961821959262693767113276095930062616648536501044331384721458640868732' +
			'13616740107373200706180851199136715526801293940291445562383745106505647087650096281526793000471925803965438181875790946685365295338214' +
			'54819873876549699192820807368313197886301618391039465217179925756659310075593706069265849365198043789874401771285020985232');
		expect(partialChoiceCodes[2].toString()).toBe('10377576473055795969259679036301270470023783840222303597658849830954729974280680179536380807' +
			'20459371061043621236901946491253631349928584105393313754503126832494471876933117689196804445977740896710438500469474987792746575510283' +
			'42994004178943495069773562909243190239717102814752735092265975983109864448875363142886783550658762175125720058309588223827910954062282' +
			'40833051702651529090528195110013374772031202980245745421615050739787190787909114127801081746704132620600809034841747608371408287682669' +
			'349009391766088416346269598958028859118996479299423934338174574596499451054709629455559401437623375985252963081688388024614');
	});
});