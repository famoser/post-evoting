/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationDerivedKeys;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationDerivedKeysRepository;

@Stateless
public class VerificationDerivedKeysService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationDerivedKeysService.class);

	@EJB
	private VerificationDerivedKeysRepository verificationDerivedKeysRepository;

	public void save(VerificationDerivedKeys entity) throws DuplicateEntryException {

		LOGGER.info("Saving derived key for tenantId {} electionEventId {} verificationCardId {} ", entity.getTenantId(), entity.getElectionEventId(),
				entity.getVerificationCardId());

		verificationDerivedKeysRepository.save(entity);
	}

}
