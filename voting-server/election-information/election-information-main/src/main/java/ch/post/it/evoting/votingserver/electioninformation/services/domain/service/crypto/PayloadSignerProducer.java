/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.crypto;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.domain.election.payload.sign.CryptolibPayloadSigner;
import ch.post.it.evoting.domain.election.payload.sign.PayloadSigner;

public class PayloadSignerProducer {

	@Inject
	private AsymmetricServiceAPI asymmetricService;

	@Produces
	public PayloadSigner getPayloadSigner() {
		return new CryptolibPayloadSigner(asymmetricService);
	}
}
