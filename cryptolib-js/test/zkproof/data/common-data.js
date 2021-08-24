/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

/* jshint node:true */
'use strict';

const elGamal = require('../../../src/elgamal');
const mathematical = require('../../../src/mathematical');
const forge = require('node-forge');
const cryptoPolicy = require('../../../src/cryptopolicy');

module.exports = CommonTestData;

const BigInteger = forge.jsbn.BigInteger;

/**
 * Provides the common data and functions needed by the zero-knowledge proof of
 * knowledge unit tests.
 */
function CommonTestData() {
	let policy;
	policy = cryptoPolicy.newInstance();
	policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;
	const mathService = mathematical.newService({policy: policy});
	const _mathRandomGenerator = mathService.newRandomGenerator();
	const _elGamalService = elGamal.newService({policy: policy});

	const _p = new BigInteger('2570132201668844157610303214867411077492515642739763713220257774193893624711212549221165937899002713482987302634875900' +
		'451064293920938285279792342323360639845576833950377443919647328520649941901724951968708391886229274545236977007301737086270393718406696215' +
		'269247497464738431752867985928394820144989812377449146250797482823662336512196717040184797450635977505325902490217737697403679167603888718' +
		'006566821885176961070818327839540739310309828649367245009756656764127726275190592527259365597962732684310458971710084560921333182532209152' +
		'8671293329804211235906640910489075537455976735341285644494144220105240375300711488393');
	const _q = new BigInteger('81259987775362040571620137377019110900790585487969528507141757273696249070213');
	const _g = new BigInteger('2130391631460499855148479642042287100746854324783178923687921536969484273409940565053514828724979114405737598769720568' +
		'751834001631617989285128424027813804570363369803625661152798864031305152862511709068954238089015387606476860270400677416434122696913895249' +
		'507575867562966510976196858938551456168816132100267690076807702295729940786888146875226850139725524107090879389395418867874764444382646506' +
		'870107820231187670976430256127774454808422863937245859892539355143945295952794976800649859864651836550522662931318156704863251623751497109' +
		'1380204424481498506499338185144271546651990709723565928420753129878206710329253950578');
	const _group = mathService.newZpSubgroup(_p, _q, _g);

	const _anotherP = new BigInteger('221153960145810831435531957923849159833759499937533652998243741827786488872131056753739528396958343121259545139' +
		'564650416097183921317887412282592661784342942755759788093656539039913463663836375000690107952768892668796618593623406550850504848911493899' +
		'711909450806436926699200599287879827908088249705973618250604084360046507399312866054155524803632423643195444239596203300104138477435833010' +
		'911828700217471969278897632059550782753533073124084801550020177303056723598147130506329226941617392813536761873085016827288265077180028506' +
		'98488786266115311952718697872338756199429262811547458969260835283688979875294699655014592833');
	const _anotherQ = new BigInteger('79783631514788897688995869254536231266441581267248557197866166084121363399063');
	const _anotherG = new BigInteger('948433703384853655735894537251753614644408150578345908298321331591502092238882229901779846193301039171225950120' +
		'128508423593075585854731193486529839611094736291373390005788156120173895172569712882280224969311661129456921389578982329734186575991904529' +
		'191570578312817348178122117683943486697623503135896958352709374317925782735545382026883291354736450920816467068038144951569130044030533391' +
		'909810514719308768118302048972421485015127295587986944846783293482263978368834432218354062180834577171717240414154824286649353942427929636' +
		'5911355186761897533654055060787119955689115570505208857288933136456026541073605147828202749');
	const _anotherGroup =
		mathService.newZpSubgroup(_anotherP, _anotherQ, _anotherG);

	const _zp224p = new BigInteger('23330121053339090065180473019828557680126288186687420191227114027428963518575355112568287576783357103627134597501' +
		'287919094273775271525391550117967295946350906919053725693223257731018934387825414245460370593031471864424752338154137402119990082323618136' +
		'759989656840006666994424890443022480340127058894741374567040781029426982461294323995089079990981091111642685712538436286474691967016668165' +
		'336902724933907331138140652221710281620834973050788043387565773998028620187625465836450514911228498359908722259975323784581885189594202550' +
		'283116659893254185053600327705705901238756414110189334506631785061506969310906070625753569');
	const _zp224q = new BigInteger('20011095635984899756519639129778847114064603773670390741976185312051');
	const _zp224g = new BigInteger('14138591810368660077770617014948800940455816268749237780294777248044533408748671920527250339513270283270495871392' +
		'396183408464848650879105125811917228278841316400311463451395948677949577575673815993767530995544215880562619524739013179770354930784328914' +
		'705450040782621775699186420159537190381933180887604207505861996772265190505496622602427666955297743551267614789886000222621142870660160490' +
		'262075671470250960441073900045928475450306792841362481719714003114225907777247401415978941425563996143418021744245253007584307265097799157' +
		'878318764526513696897134784334051219021999074413006642287896190657840583005917943930607670');
	const _zp224Group = mathService.newZpSubgroup(_zp224p, _zp224q, _zp224g);

	/**
	 * @function getP
	 * @returns {forge.jsbn.BigInteger} The modulus of a Zp subgroup.
	 */
	this.getP = function () {
		return _p;
	};

	/**
	 * @function getQ
	 * @returns {forge.jsbn.BigInteger} The order of a Zp subgroup.
	 */
	this.getQ = function () {
		return _q;
	};

	/**
	 * @function getG
	 * @returns {forge.jsbn.BigInteger} The generator of a Zp subgroup.
	 */
	this.getG = function () {
		return _g;
	};

	/**
	 * @function getGroup
	 * @returns {ZpSubgroup} A Zp subgroup.
	 */
	this.getGroup = function () {
		return _group;
	};

	/**
	 * @function getAnotherQ
	 * @returns {forge.jsbn.BigInteger} The order of another Zp subgroup.
	 */
	this.getAnotherQ = function () {
		return _anotherQ;
	};

	/**
	 * @function getAnotherGroup
	 * @returns {ZpSubgroup} Another Zp subgroup.
	 */
	this.getAnotherGroup = function () {
		return _anotherGroup;
	};

	/**
	 * @function getZP224Group
	 * @returns {ZpSubgroup} ZP_2048_224 subgroup.
	 */
	this.getZP224Group = function () {
		return _zp224Group;
	};

	/**
	 * Generates an ElGamal key pair.
	 *
	 * @function generateKeyPair
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which the public key elements belong
	 *            and to which the private key exponents are associated.
	 * @param {number}
	 *            numElements The number of Zp group elements in the public key.
	 * @param {ElGamalCryptographyService}
	 *            [elGamalService] optional instantiated ElGamalCryptographyService.
	 * @returns {ElGamalKeyPair} The ElGamal key pair.
	 */
	this.generateKeyPair = function (group, numElements, elGamalService) {
		const elements = [];
		const exponents = [];

		let exponent;
		let element;
		for (let i = 0; i < numElements; i++) {
			exponent = _mathRandomGenerator.nextExponent(group);
			exponents.push(exponent);
			element = group.generator.exponentiate(exponent);
			elements.push(element);
		}

		if (!elGamalService) {
			elGamalService = _elGamalService;
		}

		const publicKey = elGamalService.newPublicKey(group, elements);
		const privateKey = elGamalService.newPrivateKey(group, exponents);

		return elGamalService.newKeyPair(publicKey, privateKey);
	};

	/**
	 * Generates a random array of Zp group elements.
	 *
	 * @function generateRandomGroupElements
	 * @param {ZpSubgroup}
	 *            group The Zp subgroup to which the Zp group elements belong.
	 * @param {number}
	 *            numElements The number of Zp group elements to generate.
	 * @returns {ZpGroupElement[]} The array of Zp group elements.
	 */
	this.generateRandomGroupElements = function (group, numElements) {
		const elements = [];
		for (let i = 0; i < numElements; i++) {
			elements.push(_mathRandomGenerator.nextZpGroupElement(group));
		}

		return elements;
	};

	/**
	 * Exponentiates an array of Zp group base elements with an exponent.
	 *
	 * @function exponentiateElements
	 * @param {ZpGroupElement[]}
	 *            baseElements The Zp group base elements to exponentiate.
	 * @param {Exponent}
	 *            exponent The exponent to use for the exponentiation.
	 * @return {ZpGroupElement[]} The array of exponentiated Zp group base
	 *         elements.
	 */
	this.exponentiateElements = function (baseElements, exponent) {
		const exponentiatedElements = [];
		for (let i = 0; i < baseElements.length; i++) {
			exponentiatedElements.push(baseElements[i].exponentiate(exponent));
		}

		return exponentiatedElements;
	};
}
