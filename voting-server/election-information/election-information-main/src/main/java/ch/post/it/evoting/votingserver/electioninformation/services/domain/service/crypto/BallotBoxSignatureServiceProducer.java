/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.crypto;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.votingserver.commons.crypto.PrivateKeyForObjectRepository;
import ch.post.it.evoting.votingserver.commons.crypto.SignatureForObjectService;

/**
 * Producer for ballot box signature service.
 */
public class BallotBoxSignatureServiceProducer {

	@Inject
	private PrivateKeyForObjectRepository privateKeyRepository;

	@Inject
	private AsymmetricServiceAPI asymmetricService;

	/**
	 * Returns an instance of the ballot box signature service.
	 *
	 * @return an instance of the ballot box signature service.
	 */
	@Produces
	public SignatureForObjectService getInstance() {
		return new SignatureForObjectService(asymmetricService, privateKeyRepository);
	}
}
