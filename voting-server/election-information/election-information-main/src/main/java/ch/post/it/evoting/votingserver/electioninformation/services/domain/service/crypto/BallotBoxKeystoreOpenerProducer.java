/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.crypto;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.votingserver.commons.crypto.KeystoreForObjectOpener;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxKeystoreRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence.BallotBoxPasswordRepository;

/**
 * The producer for ballot box keystore opener.
 */
public class BallotBoxKeystoreOpenerProducer {

	@Inject
	private KeyStoreService storesService;

	@Inject
	private BallotBoxKeystoreRepository keystoreRepository;

	@Inject
	private BallotBoxPasswordRepository passwordRepository;

	/**
	 * Returns an instance of this service.
	 *
	 * @return returns a KeystoreForObjectOpener.
	 */
	@Produces
	@BallotBoxKeystoreOpener
	public KeystoreForObjectOpener getInstance() {
		return new KeystoreForObjectOpener(storesService, keystoreRepository, passwordRepository);
	}
}
