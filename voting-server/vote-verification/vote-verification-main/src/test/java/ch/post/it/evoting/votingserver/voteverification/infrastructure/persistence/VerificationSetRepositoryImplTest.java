/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImplTest;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * Junits for the class
 * {@link VerificationSetRepository}
 */
@RunWith(MockitoJUnitRunner.class)
public class VerificationSetRepositoryImplTest extends BaseRepositoryImplTest<VerificationSetEntity, Integer> {

	@InjectMocks
	private static final VerificationSetRepository verificationSetRepository = new VerificationSetRepositoryImpl();

	@Mock
	private TypedQuery<VerificationSetEntity> queryMock;

	/**
	 * Creates a new object of the testing class.
	 */
	public VerificationSetRepositoryImplTest() throws InstantiationException, IllegalAccessException {
		super(VerificationSetEntity.class, verificationSetRepository.getClass());
	}

	@Test
	public void testFindByTenantIdElectionEventIdVerificationCardId() throws ResourceNotFoundException {
		String tenantId = "1";
		String electionEventId = "1";
		String verificationCardIdSet = "1";
		when(entityManagerMock.createQuery(anyString(), eq(VerificationSetEntity.class))).thenReturn(queryMock);
		when(queryMock.setParameter(anyString(), anyString())).thenReturn(queryMock);
		when(queryMock.getSingleResult()).thenReturn(new VerificationSetEntity());

		assertNotNull(verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(tenantId, electionEventId, verificationCardIdSet));
	}

	@Test
	public void testFindByTenantIdElectionEventIdVerificationCardIdNotFound() throws ResourceNotFoundException {
		String tenantId = "2";
		String electionEventId = "2";
		String verificationCardIdSet = "2";
		when(entityManagerMock.createQuery(anyString(), eq(VerificationSetEntity.class))).thenReturn(queryMock);
		when(queryMock.setParameter(anyString(), anyString())).thenReturn(queryMock);
		when(queryMock.getSingleResult()).thenThrow(new NoResultException());

		expectedException.expect(ResourceNotFoundException.class);

		assertNotNull(verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(tenantId, electionEventId, verificationCardIdSet));
	}
}
