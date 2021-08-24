/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing.service;

import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Collections;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.CcmjElectionKeysSpec;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.domain.returncodes.CCPublicKey;
import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;
import ch.post.it.evoting.domain.returncodes.KeyType;

@Service
public class CcmjKeyRepository {

	/**
	 * The size of the CCMj election keys corresponds to μ in the specification and must support the maximum supported number of write-ins.
	 */
	private static final int MU = 16;

	private final KeysManager keysManager;

	@Autowired
	public CcmjKeyRepository(final KeysManager keysManager) {
		this.keysManager = keysManager;
	}

	public ElGamalPrivateKey getCcmjElectionSecretKey(final String electionId) throws KeyManagementException {
		return keysManager.getCcmjElectionSecretKey(electionId);
	}

	public PrivateKey getSigningKey(final String electionEventId) throws KeyManagementException {
		return keysManager.getElectionSigningPrivateKey(electionEventId);
	}

	public X509Certificate[] getVerificationCertificateChain(final String electionEventId) throws KeyManagementException {
		// The chain consists of the verification key certificate and the node CA certificate.
		return new X509Certificate[] { keysManager.getElectionSigningCertificate(electionEventId), keysManager.nodeCACertificate() };
	}

	public X509Certificate getPlatformCACertificate() {
		// The chain consists only of the verification key certificate.
		return keysManager.getPlatformCACertificate();
	}

	@Transactional
	public void addGeneratedKey(final KeyCreationDTO toBeReturned) throws GeneralCryptoLibException, KeyManagementException {

		if (!hasElectionSigningKey(toBeReturned.getElectionEventId(), toBeReturned.getFrom(), toBeReturned.getTo())) {
			generateElectionSigningKey(toBeReturned.getElectionEventId(), toBeReturned.getFrom(), toBeReturned.getTo());
		}

		if (!keysManager.hasCcmjElectionKeys(toBeReturned.getElectionEventId())) {
			final CcmjElectionKeysSpec keySpec = new CcmjElectionKeysSpec.Builder().setElectionEventId(toBeReturned.getElectionEventId())
					.setLength(MU).setElectoralAuthorityId(toBeReturned.getResourceId())
					.setParameters(ElGamalEncryptionParameters.fromJson(toBeReturned.getEncryptionParameters())).build();

			keysManager.createCcmElectionKey(keySpec);
		}

		final X509Certificate signingCertificate = keysManager.getElectionSigningCertificate(toBeReturned.getElectionEventId());
		final ElGamalPublicKey ccmjElectionPublicKey = keysManager.getCcmjElectionPublicKey(toBeReturned.getElectionEventId());

		final CCPublicKey generatedKey = new CCPublicKey();
		generatedKey.setKeytype(KeyType.CCM_J_ELECTION_KEY);
		generatedKey.setPublicKey(ccmjElectionPublicKey);
		generatedKey.setSignerCertificate(signingCertificate);
		generatedKey.setKeySignature(keysManager.getCcmjElectionPublicKeySignature(toBeReturned.getElectionEventId()));
		generatedKey.setNodeCACertificate(keysManager.nodeCACertificate());

		toBeReturned.setPublicKey(Collections.singletonList(generatedKey));
	}

	private void generateElectionSigningKey(final String electionEventId, final ZonedDateTime from, final ZonedDateTime to)
			throws KeyManagementException {
		keysManager.createElectionSigningKeys(electionEventId, from, to);
	}

	private boolean hasElectionSigningKey(final String electionEventId, final ZonedDateTime from, final ZonedDateTime to)
			throws KeyManagementException {
		return keysManager.hasValidElectionSigningKeys(electionEventId, from, to);
	}

}
