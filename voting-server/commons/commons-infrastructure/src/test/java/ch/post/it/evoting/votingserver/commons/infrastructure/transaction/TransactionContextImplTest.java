/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.transaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ejb.EJBContext;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link TransactionContextImpl}.
 */
public class TransactionContextImplTest {
	private EJBContext context;

	@Before
	public void setUp() {
		context = mock(EJBContext.class);
	}

	@Test
	public void testIsRollbackOnly() {
		TransactionContextImpl wrapper = new TransactionContextImpl(context, true);
		assertFalse(wrapper.isRollbackOnly());
		when(context.getRollbackOnly()).thenReturn(true);
		assertTrue(wrapper.isRollbackOnly());
	}

	@Test(expected = IllegalStateException.class)
	public void testIsRollbackOnlyNonTransactional() {
		new TransactionContextImpl(context, false).isRollbackOnly();
	}

	@Test
	public void testIsTransactional() {
		assertTrue(new TransactionContextImpl(context, true).isTransactional());
		assertFalse(new TransactionContextImpl(context, false).isTransactional());
	}

	@Test
	public void testSetRollbackOnly() {
		new TransactionContextImpl(context, true).setRollbackOnly();
		verify(context).setRollbackOnly();
	}

	@Test(expected = IllegalStateException.class)
	public void testSetRollbackOnlyNonTransactional() {
		new TransactionContextImpl(context, false).setRollbackOnly();
	}
}
