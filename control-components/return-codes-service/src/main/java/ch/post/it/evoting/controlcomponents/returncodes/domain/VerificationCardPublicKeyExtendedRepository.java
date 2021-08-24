/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import ch.post.it.evoting.controlcomponents.returncodes.domain.exception.VerificationCardPublicKeyExtendedRepositoryException;
import ch.post.it.evoting.controlcomponents.returncodes.service.ReturnCodesKeyRepository;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.VerificationCardPublicKeyRepositoryException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

@Repository
public class VerificationCardPublicKeyExtendedRepository {

	private final VerificationCardPublicKeyExtendedRawRepository verificationCardPublicKeyExtendedRawRepository;
	private final ObjectMapper objectMapper;
	private final ReturnCodesKeyRepository returnCodesKeyRepository;

	public VerificationCardPublicKeyExtendedRepository(
			final VerificationCardPublicKeyExtendedRawRepository verificationCardPublicKeyExtendedRawRepository, final ObjectMapper objectMapper,
			final ReturnCodesKeyRepository returnCodesKeyRepository) {
		this.verificationCardPublicKeyExtendedRawRepository = verificationCardPublicKeyExtendedRawRepository;
		this.objectMapper = objectMapper;
		this.returnCodesKeyRepository = returnCodesKeyRepository;
	}

	public VerificationCardPublicKeyExtended save(final VerificationCardPublicKeyExtended verificationCardPublicKeyExtended) {
		return toVerificationCardPublicKeyExtended(
				verificationCardPublicKeyExtendedRawRepository.save(toVerificationCardPublicKeyExtendedRaw(verificationCardPublicKeyExtended)));
	}

	public Optional<VerificationCardPublicKeyExtended> findById(final String verificationCardId) {
		return verificationCardPublicKeyExtendedRawRepository.findById(verificationCardId).map(this::toVerificationCardPublicKeyExtended);
	}

	private VerificationCardPublicKeyExtendedRaw toVerificationCardPublicKeyExtendedRaw(
			final VerificationCardPublicKeyExtended verificationCardPublicKeyExtended) {

		final String electionEventId = verificationCardPublicKeyExtended.getElectionEventId();
		final String verificationCardId = verificationCardPublicKeyExtended.getVerificationCardId();
		final String verificationCardSetId = verificationCardPublicKeyExtended.getVerificationCardSetId();
		final ElGamalMultiRecipientPublicKey verificationCardPublicKey = verificationCardPublicKeyExtended.getVerificationCardPublicKey();

		final byte[] verificationCardPublicKeyBytes;
		try {
			verificationCardPublicKeyBytes = objectMapper.writeValueAsString(verificationCardPublicKey).getBytes(StandardCharsets.UTF_8);
		} catch (JsonProcessingException e) {
			throw new VerificationCardPublicKeyRepositoryException(
					String.format("An error occurred while saving the provided verification card public key entry with verification card id %s.",
							verificationCardId), e);
		}

		return new VerificationCardPublicKeyExtendedRaw(electionEventId, verificationCardId, verificationCardSetId, verificationCardPublicKeyBytes);
	}

	private VerificationCardPublicKeyExtended toVerificationCardPublicKeyExtended(
			final VerificationCardPublicKeyExtendedRaw verificationCardPublicKeyExtendedRaw) {

		final String electionEventId = verificationCardPublicKeyExtendedRaw.getElectionEventId();
		final String verificationCardId = verificationCardPublicKeyExtendedRaw.getVerificationCardId();
		final String verificationCardSetId = verificationCardPublicKeyExtendedRaw.getVerificationCardSetId();
		final byte[] verificationCardPublicKeyRaw = verificationCardPublicKeyExtendedRaw.getVerificationCardPublicKey();

		final ElGamalMultiRecipientPublicKey verificationCardPublicKey = getVerificationCardPublicKey(electionEventId, verificationCardId,
				verificationCardSetId, verificationCardPublicKeyRaw);

		return new VerificationCardPublicKeyExtended(electionEventId, verificationCardId, verificationCardSetId, verificationCardPublicKey);
	}

	private ElGamalMultiRecipientPublicKey getVerificationCardPublicKey(final String electionEventId, final String verificationCardId,
			final String verificationCardSetId, final byte[] elGamalMultiRecipientPublicKeyByteArray) {

		final ElGamalMultiRecipientPublicKey verificationCardPublicKey;
		try {
			verificationCardPublicKey = getObjectReaderWithGroup(electionEventId, verificationCardSetId)
					.readValue(elGamalMultiRecipientPublicKeyByteArray, ElGamalMultiRecipientPublicKey.class);
		} catch (KeyManagementException | IOException e) {
			throw new VerificationCardPublicKeyExtendedRepositoryException(
					String.format("An error occurred while retrieving the verification card public key extended entry for verification card id %s.",
							verificationCardId), e);
		}
		return verificationCardPublicKey;
	}

	private ObjectReader getObjectReaderWithGroup(final String electionEventId, final String verificationCardSetId) throws KeyManagementException {
		return objectMapper.reader().withAttribute("group", getGqGroup(electionEventId, verificationCardSetId));
	}

	private GqGroup getGqGroup(final String electionEventId, final String verificationCardSetId) throws KeyManagementException {
		final ZpSubgroup mathematicalGroup = returnCodesKeyRepository.getMathematicalGroup(electionEventId, verificationCardSetId);
		return new GqGroup(mathematicalGroup.getP(), mathematicalGroup.getQ(), mathematicalGroup.getG());
	}

}
