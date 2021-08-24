/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.crypto;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import ch.post.it.evoting.votingserver.commons.signature.SignatureFactory;
import ch.post.it.evoting.votingserver.commons.signature.SignatureFactoryImpl;

/**
 * The producer for the signature factory implementation.
 */
public class SignatureFactoryProducer {

	@Produces
	@ApplicationScoped
	public SignatureFactory getInstance() {
		return SignatureFactoryImpl.newInstance();
	}
}
