/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.crypto;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import ch.post.it.evoting.votingserver.commons.crypto.KeystoreForObjectOpener;
import ch.post.it.evoting.votingserver.commons.crypto.PrivateKeyForObjectRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence.BallotBoxPasswordRepository;

/**
 * The producer for ballot box private key repository.
 */
public class BallotBoxPrivateKeyRepositoryProducer {

	@Inject
	@BallotBoxKeystoreOpener
	private KeystoreForObjectOpener keystoreOpener;

	@Inject
	private BallotBoxPasswordRepository passwordRepository;

	@Produces
	public PrivateKeyForObjectRepository getInstance() {
		return new PrivateKeyForObjectRepository(keystoreOpener, passwordRepository);
	}
}
