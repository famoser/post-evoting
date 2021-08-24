/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* global require */
/* global self */
/* jshint -W020 */

const Q = require('q');

const model = require('./model/model');
const secureRandom = require('cryptolib-js/src/securerandom');
const codec = require('cryptolib-js/src/codec');

let secureRandomService;

// ov client api export
const OV = {

	initWorker: function (name, config) {
		'use strict';

		const deferred = Q.defer();

		if (self) {
			self.name = name;
		}

		secureRandomService = secureRandom.newService();

		OV.config(JSON.parse(config));
		deferred.resolve('ok');
		return deferred.promise;
	},

	// High level API

	// main vote cycle
	authenticate: require('./client/authenticate.js').authenticate,
	requestBallot: require('./client/request-ballot.js').requestBallot,
	sendVote: require('./client/send-vote.js').sendVote,
	castVote: require('./client/cast-vote.js').castVote,

	// cycle completions
	requestChoiceCodes: require('./client/request-choicecodes.js'),
	requestVoteCastCode: require('./client/request-votecastcode.js'),

	// data models
	model: model,
	Ballot: model.ballot.Ballot,
	ListsAndCandidates: model.ballot.ListsAndCandidates,
	Options: model.ballot.Options,
	Question: model.ballot.Question,
	Option: model.ballot.Option,
	List: model.ballot.List,
	Candidate: model.ballot.Candidate,

	// session and config
	config: require('./client/config.js'),
	session: require('./client/session.js'),
	updateConfig: function (configData) {
		'use strict';
		OV.config(JSON.parse(configData));
	},
	getAuthentication: function () {
		'use strict';
		return OV.session('authenticationToken');
	},
	getSerializedEncryptionParams: function () {
		'use strict';
		const ep = OV.session('encParams');
		return {
			serializedP: ep.serializedP,
			serializedQ: ep.serializedQ,
			serializedG: ep.serializedG,
			serializedOptionsEncryptionKey: ep.serializedOptionsEncryptionKey,
			serializedChoiceCodesEncryptionKey: ep.serializedChoiceCodesEncryptionKey,
		};
	},
	getSerializedVerificationCardSecret: function () {
		'use strict';
		return OV.session('verificationCardSecret').toString();
	},
	getSessionData: function (key) {
		'use strict';
		return OV.session(key);
	},
	getRandomInt: function (length) {
		'use strict';
		return secureRandomService
			.newRandomGenerator()
			.nextBigIntegerByDigits(length)
			.toString();
	},
	getRandomBytes: function (length) {
		'use strict';
		return codec.base64Encode(
			codec.binaryEncode(
				secureRandomService.newRandomGenerator().nextBytes(length),
			),
		);
	},

	// message processing API / vote cycle
	processInformationsResponse: require('./client/request-ballot.js')
		.processInformationsResponse,
	processTokensResponse: require('./client/request-ballot.js')
		.processTokensResponse,
	createVoteRequest: require('./client/send-vote.js').createVoteRequest,
	processVoteResponse: require('./client/send-vote.js').processVoteResponse,
	createConfirmRequest: require('./client/cast-vote.js').createConfirmRequest,
	processConfirmResponse: require('./client/cast-vote.js')
		.processConfirmResponse,

	// message processing API / cycle completions
	processCastCodeResponse: require('./client/cast-vote.js')
		.processConfirmResponse,
	processChoiceCodesResponse: require('./client/send-vote.js')
		.processVoteResponse,

	// parsers

	BallotParser: require('./parsers/ballot-parser.js'),
	OptionsParser: require('./parsers/options.js'),
	ListsAndCandidatesParser: require('./parsers/lists-and-candidates.js'),
	parseStartVotingKey: require('./parsers/start-voting-key.js'),
	parseServerChallenge: require('./parsers/challenge.js'),
	parseBallotResponse: require('./parsers/ballot-response.js'),
	parseEncryptionParams: require('./parsers/encryption-params.js'),
	parseTokenResponse: require('./parsers/auth-token.js')
		.validateAuthTokenResponse,
	parseToken: require('./parsers/auth-token.js').validateAuthToken,
	parseCastResponse: require('./parsers/cast-response.js'),
	parseVerificationCard: require('./parsers/verification-card.js'),
	parseCredentials: require('./parsers/credential-extractor.js'),

	// pre-computations
	precomputeEncrypterValues: require('./protocol/precompute/encrypter-values.js')
		.precomputeEncryptionValues,
	deserializeEncrypterValues: require('./protocol/precompute/encrypter-values.js')
		.deserializeEncryptionValues,
	precomputePartialChoiceCode: require('./protocol/precompute/partial-choice-codes.js')
		.precomputePartialChoiceCode,
	getPrecomputedPartialChoiceCodes: require('./protocol/precompute/partial-choice-codes.js')
		.getPrecomputedPartialChoiceCodes,
	precomputeProofs: require('./protocol/precompute/proofs.js').precomputeProofs,

	// encryption
	encryptRho: require('./protocol/cipher.js').encryptRho,
	encryptPartialChoiceCodes: require('./protocol/cipher.js')
		.encryptPartialChoiceCodes,

	// proofs
	generateCipherTextExponentiations: require('./protocol/proof.js')
		.generateCipherTextExponentiations,
	generateExponentiationProof: require('./protocol/proof.js')
		.generateExponentiationProof,
	generatePlaintextEqualityProof: require('./protocol/proof.js')
		.generatePlaintextEqualityProof,

	// return codes
	generatePartialChoiceCodes: require('./protocol/partial-choice-codes.js'),
	generateConfirmationKey: require('./protocol/confirmation-key.js'),

	// signature
	signData: require('./signature/signature.js').signData,
	verifySignature: require('./signature/signature.js').verifySignature,

	// certificate chain validation
	validateCertificateChain: require('./certificate/certificate-chain.js'),
};

if (typeof self !== 'undefined') {
	self.OV = OV;
} else {
	module.exports = OV;
}

/* jshint ignore: end */
