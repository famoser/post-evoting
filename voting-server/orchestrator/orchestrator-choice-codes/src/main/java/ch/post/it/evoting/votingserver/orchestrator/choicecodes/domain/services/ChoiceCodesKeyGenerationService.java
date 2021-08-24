/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.time.ZonedDateTime;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingException;

public interface ChoiceCodesKeyGenerationService {

	ChoiceCodesKeysResponse requestChoiceCodesKeyGenerationSync(String trackingId, String tenantId, String electionEventId,
			final List<String> verificationCardSetIds, ZonedDateTime keysDateFrom, ZonedDateTime keysDateTo,
			ElGamalEncryptionParameters elGamalEncryptionParameters) throws ResourceNotFoundException, GeneralCryptoLibException;

	void startup() throws MessagingException;

	void shutdown() throws MessagingException;
}
