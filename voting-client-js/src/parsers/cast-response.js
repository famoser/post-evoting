/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
/* jshint maxlen: 666  */

const certificateService = require('cryptolib-js/src/certificate').newService();

module.exports = (function () {
	'use strict';

	// validate vote cast response
	// returns: vote cast code
	const validateCastResponse = function (
		response,
		credentialCert,
		authenticationTokenSignerCert,
		ballotBoxCert,
		trustedVotingCardId,
		trustedElectionEventId,
		trustedVerificationCardId,
	) {
		const electionEventId = response.electionEventId;
		const votingCardId = response.votingCardId;
		const verificationCardId = response.verificationCardId;

		if (
			electionEventId !== trustedElectionEventId ||
			votingCardId !== trustedVotingCardId ||
			verificationCardId !== trustedVerificationCardId
		) {
			throw new Error('Bad vote');
		}

		const voteCastMessage = response.voteCastMessage;

		return {
			voteCastCode: voteCastMessage.voteCastCode
		};
	};

	return validateCastResponse;
})();
