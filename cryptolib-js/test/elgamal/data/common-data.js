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

const SHORT_EXPONENT_BIT_LENGTH = 256;

const NUM_GROUP_ELEMENTS = 2;
const P_STR = '18388150860058403802920178412529336901045699792821062607508310191219067488929344819011655681691429908099537898007051346381319540197913' +
	'9859203647659085471016920713495528166363693675446161613934552485057155482567306193272596584186000158109106046352639037376165933064299110722219' +
	'4099935594482476175383869987955168985814863206910604761983038024345803144240965961120000788895995343754304465559705608854777189131264826250622' +
	'7920704399229781324390822268662093433904228476979779245119127884271965731790161943746483421514488899439030602938576698046317134953671828836992' +
	'240482213650086228584024738151779796555132971162736548421';
const Q_STR = '109188178987346060540931492760016322087867834859560658858348863050652467381219';
const G_STR = '64315942550503136727981620414161451551235371761622064645996731542026880278205502893376821127325878287867087600488108608174175740134352' +
	'2632440811523808637204698240464370595766369624228246094791988729125222453918573776737735985768664998564576263067086207133483775488562203819523' +
	'2217444313694852522479206669610421474444346400342471527071738523665348936494890059311136916656053115112260529618114987507430695124467855162053' +
	'7555711772724435465216769559244231082935608963969604150969568180801274619806459252465580241072439295435401728321355412502207080111934428493082' +
	'85218922194166541952394599374171304353189155270026540947';
const KEY_ELEMENT_1_VALUE_STR = '62229054538371822821734116921313203138422396593965523636451726194231071535399085644280537063552549326953632112754446' +
	'7158815885855473035942199291485922877934908678968632828210597201167102597675040395486667689132392103796804836049178862530845404104504282781233' +
	'8911329303818338444139116110904609342037068708505736425363404846988928149354066943779308411527397358595118508207472337003094349381941646433934' +
	'5413331180428695702784427237522512443432186319201326699933668616571295128589930070014990681925057703199931395506690500526619322599562284367704' +
	'08044264115085140506310958619509430485406379957819291807571476403880321112';
const KEY_ELEMENT_2_VALUE_STR = '77969395437035688473517017985488631925034612117228110489202621272771994385523186897682724651436254908474368066649232' +
	'7109293663983146055895182685877287377284850801736821248888741662330478956885051799331211982552371700315586597730976844291083607678265825611321' +
	'2875943434284036296553805260329288532663730222682668689437209426574842836922715691267700619893831036441384453480056663931420016122005091410629' +
	'3390786038034379311496356599393184259049396199745274592595077995063096975303537903197226568294398320131528201482675297308595972742488272018058' +
	'01501442059620434483336111723779741433057594607940013666624632357374094475';
const KEY_EXPONENT_1_VALUE_STR = '68379038900637051517929857043396627159524942841517755089254237132167541867842';
const KEY_EXPONENT_2_VALUE_STR = '97029418700179232960780546578551913071897870306385395803266262100579700109881';

const P_LARGE_STR = '22588801568735561413035633152679913053449200833478689904902877673687016391844561133376032309307885537704777240609087377993341380' +
	'7516976052355411312738684400709203621484318668297877844450191479993794985036932474295794802892266027483973353278908844646850516827037097427241' +
	'2178321782704072241536010317928916005658175937247584598543897730732357053075336202714538412477182611465171026476643727304475969095505198283968' +
	'4910462609395741692689616014805965573558015387956017183286848440036954926101719205598449898400180082053755864070690174202432196678045052744337' +
	'832802051787273056312757384654145455745603262082348042780103679';
const Q_LARGE_STR = '11294400784367780706517816576339956526724600416739344952451438836843508195922280566688016154653942768852388620304543688996670690' +
	'3758488026177705656369342200354601810742159334148938922225095739996897492518466237147897401446133013741986676639454422323425258413518548713620' +
	'6089160891352036120768005158964458002829087968623792299271948865366178526537668101357269206238591305732585513238321863652237984547752599141984' +
	'2455231304697870846344808007402982786779007693978008591643424220018477463050859602799224949200090041026877932035345087101216098339022526372168' +
	'916401025893636528156378692327072727872801631041174021390051839';

const KEY_ELEMENT_1_LARGE_VALUE_STR = '14118699621894172980225611272933299131353865752258016996468846378094236212113806734240074225471706756995850140' +
	'2867010578226285138565568904241580704234067053342101029634401485384801343884244854638821541794079269793557996994757927213493285569866246429658' +
	'8041016690289664326306032036158927871248999953135148890369588302849737792699054076966877562361404921967542599040009508142638108888851011719795' +
	'3523516058777073002219883725425471937542330916875364597553428048871435204853236243029794903219193338440077676265388558538372159054850966207324' +
	'937461436773385170716507816121235795917120775830529453756801220404513557541311805';
const KEY_ELEMENT_2_LARGE_VALUE_STR = '22860349954246379047084638720156205384629020036895535155842687951145076930662118304464264614804905136130399460' +
	'9237845580091353523997134037100095212152964731911131485347196095880238618433047733666284153881980966738893640106761573279064384684288796735107' +
	'6932791728093612816780245718133241298280781417089157937994738735722008099904009805487090943629954985489752405326037840780529255201896418042380' +
	'3809565278816547744405317378849143416277867653397110448962570405354349135513501420031916277697814483458787677968773954485321874021508873024555' +
	'64641487745680779011369631297159219091991925518416300110600784368420122244795314';
const KEY_EXPONENT_1_LARGE_VALUE_STR = '5246195210525433809991041832497500420159820502611922642940594499141336290822436187204306951878394812419367396' +
	'8702886959975964996326378305655546882565111753230803069530202284669707170877127120959862568460534617048959477602369911294178626114927928022059' +
	'8958460324584294946084974584688905975997251217471168093893503082818854773946625150702742678211399798148706312886828262419013280628208871949747' +
	'0116612846257985325063934014036332157021951620977724180154700796454307668822217381945535502098030621793703965238911963021713929460908714295226' +
	'392914329713320481974582030903807983051516340305356363705307675208448710968910281';
const KEY_EXPONENT_2_LARGE_VALUE_STR = '6217830292449137912442681116256872859548577845796124096346308302486784713606511013510921690485062872494403744' +
	'4680015069483182133744234021647454163804342841602083233491885857202150665844613080866345288989368622110461121404406545567802412720906788293704' +
	'7088949310307493796623838827613876350829591581777945620588016845852330294144189864821052111767837217657053273874677832245023478151258184888723' +
	'1094958009745386789647429837654456159563537678760039640284263420416349623609340765899018438834432932191721559272684472356094055314376404579914' +
	'705151171906397740417398220907146365470603508856967989388013687983762445746354133';


/**
 * Provides the common data needed by the ElGamal service unit tests.
 */
function CommonTestData() {
	const policy = cryptoPolicy.newInstance();
	policy.mathematical.groups.type =
		cryptoPolicy.options.mathematical.groups.type.ZP_2048_256;


	let _mathService = mathematical.newService({policy: policy});
	let elGamalService = elGamal.newService({policy: policy});

	const _mathRandomGenerator = _mathService.newRandomGenerator();

	let p = new BigInteger(P_STR);
	let q = new BigInteger(Q_STR);
	let g = new BigInteger(G_STR);
	const _group = _mathService.newZpSubgroup(p, q, g);

	const _publicKeyElements = [];
	_publicKeyElements[0] = _mathService.newZpGroupElement(p, q, new BigInteger(KEY_ELEMENT_1_VALUE_STR));
	_publicKeyElements[1] = _mathService.newZpGroupElement(p, q, new BigInteger(KEY_ELEMENT_2_VALUE_STR));
	const _publicKey = elGamalService.newPublicKey(_group, _publicKeyElements);

	const _privateKeyExponents = [];
	_privateKeyExponents[0] = _mathService.newExponent(q, new BigInteger(KEY_EXPONENT_1_VALUE_STR));
	_privateKeyExponents[1] = _mathService.newExponent(q, new BigInteger(KEY_EXPONENT_2_VALUE_STR));
	const _privateKey = elGamalService.newPrivateKey(_group, _privateKeyExponents);

	const _keyPair = elGamalService.newKeyPair(_publicKey, _privateKey);

	const _elements = generateGroupElements(_group, NUM_GROUP_ELEMENTS);
	const _elementValueStrings = getGroupElementValueStrings(_elements);

	p = new BigInteger(P_LARGE_STR);
	q = new BigInteger(Q_LARGE_STR);
	g = new BigInteger(G_STR);
	/* Get a new service with the default policy to be able to test
	QR_2048 group type */
	_mathService = mathematical.newService();
	const _largeGroup = _mathService.newZpSubgroup(p, q, g);

	const _largePublicKeyElements = [];
	_largePublicKeyElements[0] = _mathService.newZpGroupElement(p, q, new BigInteger(KEY_ELEMENT_1_LARGE_VALUE_STR));
	_largePublicKeyElements[1] = _mathService.newZpGroupElement(p, q, new BigInteger(KEY_ELEMENT_2_LARGE_VALUE_STR));

	/* Get a new service with the default policy to be able to test
	QR_2048 group type */
	elGamalService = elGamal.newService();
	const _largePublicKey = elGamalService.newPublicKey(_largeGroup, _largePublicKeyElements);

	const _largePrivateKeyExponents = [];
	_largePrivateKeyExponents[0] = _mathService.newExponent(q, new BigInteger(KEY_EXPONENT_1_LARGE_VALUE_STR));
	_largePrivateKeyExponents[1] = _mathService.newExponent(q, new BigInteger(KEY_EXPONENT_2_LARGE_VALUE_STR));
	const _largePrivateKey = elGamalService.newPrivateKey(_largeGroup, _largePrivateKeyExponents);

	const _elementsFromLargeGroup = generateGroupElements(_largeGroup, NUM_GROUP_ELEMENTS);
	const _exponentsFromLargeGroup = generateRandomExponents(_largeGroup, NUM_GROUP_ELEMENTS);

	p = new BigInteger(P_LARGE_STR);
	q = new BigInteger(Q_LARGE_STR).subtract(BigInteger.ONE);
	g = new BigInteger(G_STR);
	const _elementsFromLargeNonQrGroup = generateGroupElements(_mathService.newZpSubgroup(p, q, g), NUM_GROUP_ELEMENTS);

	p = new BigInteger(P_STR);
	q = new BigInteger(Q_STR);
	const _identityElements = [];
	for (let i = 0; i < NUM_GROUP_ELEMENTS; i++) {
		_identityElements[i] = _mathService.newZpGroupElement(p, q, BigInteger.ONE);
	}

	this.getGroup = function () {
		return _group;
	};

	this.getLargeGroup = function () {
		return _largeGroup;
	};

	this.getPublicKey = function () {
		return _publicKey;
	};

	this.getPrivateKey = function () {
		return _privateKey;
	};

	this.getKeyPair = function () {
		return _keyPair;
	};

	this.getPublicKeyElements = function () {
		return _publicKeyElements;
	};

	this.getLargePublicKeyElements = function () {
		return _largePublicKeyElements;
	};

	this.getPrivateKeyExponents = function () {
		return _privateKeyExponents;
	};

	this.getZpGroupElements = function () {
		return _elements;
	};

	this.getZpGroupElementValueStrings = function () {
		return _elementValueStrings;
	};

	this.getLargePublicKey = function () {
		return _largePublicKey;
	};

	this.getLargePrivateKey = function () {
		return _largePrivateKey;
	};

	this.getElementsFromLargeZpSubgroup = function () {
		return _elementsFromLargeGroup;
	};

	this.getExponentsFromLargeZpSubgroup = function () {
		return _exponentsFromLargeGroup;
	};

	this.getElementsFromLargeNonQrZpSubgroup = function () {
		return _elementsFromLargeNonQrGroup;
	};

	this.getIdentityElements = function () {
		return _identityElements;
	};

	this.getShortExponentBitLength = function () {
		return SHORT_EXPONENT_BIT_LENGTH;
	};

	function generateRandomExponents(group, numExponents) {
		const exponents = [];
		for (let i = 0; i < numExponents; i++) {
			exponents.push(_mathRandomGenerator.nextExponent(group));
		}

		return exponents;
	}

	function generateGroupElements(group, numElements) {
		const elements = [];
		for (let i = 0; i < numElements; i++) {
			elements.push(_mathRandomGenerator.nextZpGroupElement(group));
		}

		return elements;
	}

	function getGroupElementValueStrings(elements) {
		const elementValueStrings = [];
		for (let i = 0; i < elements.length; i++) {
			elementValueStrings.push(elements[i].value.toString());
		}

		return elementValueStrings;
	}
}
