/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import java.security.KeyManagementException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.CcrjReturnCodesKeysSpec;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.domain.returncodes.CCPublicKey;
import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;
import ch.post.it.evoting.domain.returncodes.KeyType;

@Service
public class ReturnCodesKeyRepository {

	private static final int CCR_J_RETURN_CODES_GENERATION_KEY_LENGTH = 1;

	/**
	 * The size of the CCRj Choice Return Codes encryption key corresponds to 𝜑 in the specification and must support the maximum number of
	 * selectable voting options
	 */
	private static final int PHI = 120;

	private final KeysManager keysManager;

	public ReturnCodesKeyRepository(final KeysManager keysManager) {
		this.keysManager = keysManager;
	}

	@Transactional
	public void addGeneratedKey(final KeyCreationDTO toBeReturned) throws GeneralCryptoLibException, KeyManagementException {

		if (!hasElectionSigningKey(toBeReturned.getElectionEventId(), toBeReturned.getFrom(), toBeReturned.getTo())) {
			generateElectionSigningKey(toBeReturned.getElectionEventId(), toBeReturned.getFrom(), toBeReturned.getTo());
		}

		if (!keysManager.hasCcrjReturnCodesKeys(toBeReturned.getElectionEventId(), toBeReturned.getResourceId())) {
			CcrjReturnCodesKeysSpec ccrjReturnCodesKeysSpec = new CcrjReturnCodesKeysSpec.Builder().setCcrjChoiceReturnCodesEncryptionKeyLength(PHI)
					.setElectionEventId(toBeReturned.getElectionEventId())
					.setCcrjReturnCodesGenerationKeyLength(CCR_J_RETURN_CODES_GENERATION_KEY_LENGTH)
					.setVerificationCardSetId(toBeReturned.getResourceId())
					.setParameters(ElGamalEncryptionParameters.fromJson(toBeReturned.getEncryptionParameters())).build();

			keysManager.createCcrjReturnCodesKeys(ccrjReturnCodesKeysSpec);
		}

		final X509Certificate signingCertificate = keysManager.getElectionSigningCertificate(toBeReturned.getElectionEventId());
		final ElGamalPublicKey ccrjReturnCodesGenerationPublicKey = keysManager
				.getCcrjReturnCodesGenerationPublicKey(toBeReturned.getElectionEventId(), toBeReturned.getResourceId());

		final CCPublicKey ccrReturnCodesGenerationPublicKey = new CCPublicKey();
		ccrReturnCodesGenerationPublicKey.setKeytype(KeyType.CCR_J_RETURN_CODES_GENERATION_KEY);
		ccrReturnCodesGenerationPublicKey.setPublicKey(ccrjReturnCodesGenerationPublicKey);
		ccrReturnCodesGenerationPublicKey.setSignerCertificate(signingCertificate);
		ccrReturnCodesGenerationPublicKey.setNodeCACertificate(keysManager.nodeCACertificate());
		ccrReturnCodesGenerationPublicKey.setKeySignature(
				keysManager.getCcrjReturnCodesGenerationPublicKeySignature(toBeReturned.getElectionEventId(), toBeReturned.getResourceId()));

		final ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey = keysManager
				.getCcrjChoiceReturnCodesEncryptionPublicKey(toBeReturned.getElectionEventId(), toBeReturned.getResourceId());

		final CCPublicKey ccrChoiceReturnCodesEncryptionPublicKey = new CCPublicKey();
		ccrChoiceReturnCodesEncryptionPublicKey.setKeytype(KeyType.CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_KEY);
		ccrChoiceReturnCodesEncryptionPublicKey.setPublicKey(ccrjChoiceReturnCodesEncryptionPublicKey);
		ccrChoiceReturnCodesEncryptionPublicKey.setSignerCertificate(signingCertificate);
		ccrChoiceReturnCodesEncryptionPublicKey.setNodeCACertificate(keysManager.nodeCACertificate());
		ccrChoiceReturnCodesEncryptionPublicKey.setKeySignature(
				keysManager.getCcrjChoiceReturnCodesEncryptionPublicKeySignature(toBeReturned.getElectionEventId(), toBeReturned.getResourceId()));

		// Contains both the CcrjReturnCodesGenerationPublicKey and the ccrChoiceReturnCodesEncryptionPublicKey
		toBeReturned.setPublicKey(Arrays.asList(ccrReturnCodesGenerationPublicKey, ccrChoiceReturnCodesEncryptionPublicKey));
	}

	public ElGamalPrivateKey getCcrjReturnCodesGenerationSecretKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return keysManager.getCcrjReturnCodesGenerationSecretKey(electionEventId, verificationCardSetId);
	}

	public ElGamalPrivateKey getCcrjChoiceReturnCodesEncryptionSecretKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return keysManager.getCcrjChoiceReturnCodesEncryptionSecretKey(electionEventId, verificationCardSetId);
	}

	public ElGamalPublicKey getCcrjChoiceReturnCodesEncryptionPublicKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return keysManager.getCcrjChoiceReturnCodesEncryptionPublicKey(electionEventId, verificationCardSetId);
	}

	private void generateElectionSigningKey(final String electionEventId, final ZonedDateTime from, final ZonedDateTime to)
			throws KeyManagementException {
		keysManager.createElectionSigningKeys(electionEventId, from, to);
	}

	private boolean hasElectionSigningKey(final String electionEventId, final ZonedDateTime from, final ZonedDateTime to)
			throws KeyManagementException {
		return keysManager.hasValidElectionSigningKeys(electionEventId, from, to);
	}

	public ZpSubgroup getMathematicalGroup(final String electionEventId, final String verificationCardSetId) throws KeyManagementException {
		return (ZpSubgroup) keysManager.getEncryptionParameters(electionEventId, verificationCardSetId).getGroup();
	}

	public X509Certificate getPlatformCACertificate() {
		return keysManager.getPlatformCACertificate();
	}
}
