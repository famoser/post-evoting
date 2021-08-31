/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.validation;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationToken;
import ch.post.it.evoting.votingserver.authentication.services.domain.utils.BeanUtils;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.AuthTokenValidationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;

/**
 * Test of the decorator class
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationTokenSignatureValidationDecoratorTest {

	public static final String TENANT_ID = "100";

	public static final String ELECTION_EVENT_ID = "100";

	public static final String VOTING_CARD_ID = "100";
	@InjectMocks
	private final AuthenticationTokenSignatureValidationDecorator sut = new AuthenticationTokenSignatureValidationDecorator() {
		@Override
		public ValidationResult execute(String tenantId, String electionEventId, String votingCardId, AuthenticationToken authenticationToken)
				throws CertificateException, ResourceNotFoundException {
			return super.execute(tenantId, electionEventId, votingCardId, authenticationToken);
		}
	};
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	@Mock
	private AuthenticationTokenSignatureValidation authenticationTokenSignatureValidation;

	@Test
	public void validate() throws ResourceNotFoundException, CertificateException {
		when(authenticationTokenSignatureValidation.execute(anyString(), anyString(), anyString(), any(AuthenticationToken.class)))
				.thenReturn(new ValidationResult(true));

		assertTrue(sut.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, new AuthenticationToken()).isResult());

	}

	@Test
	public void testValidationFails() throws ResourceNotFoundException, CertificateException {

		expectedException.expect(AuthTokenValidationException.class);
		final AuthenticationToken authenticationToken = BeanUtils.createAuthenticationToken();
		when(authenticationTokenSignatureValidation.execute(anyString(), anyString(), anyString(), any(AuthenticationToken.class)))
				.thenThrow(new AuthTokenValidationException(null));
		sut.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, authenticationToken);

	}

	@Test
	public void testValidationFailsResourceNotFound() throws ResourceNotFoundException, CertificateException {

		expectedException.expect(ResourceNotFoundException.class);
		final AuthenticationToken authenticationToken = BeanUtils.createAuthenticationToken();
		when(authenticationTokenSignatureValidation.execute(anyString(), anyString(), anyString(), any(AuthenticationToken.class)))
				.thenThrow(new ResourceNotFoundException("exception"));
		sut.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, authenticationToken);

	}

	@Test
	public void testValidationFailsCertificateException() throws ResourceNotFoundException, CertificateException {

		expectedException.expect(CertificateException.class);
		final AuthenticationToken authenticationToken = BeanUtils.createAuthenticationToken();
		when(authenticationTokenSignatureValidation.execute(anyString(), anyString(), anyString(), any(AuthenticationToken.class)))
				.thenThrow(new CertificateException("exception"));
		sut.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, authenticationToken);

	}

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this.getClass());
	}

}
