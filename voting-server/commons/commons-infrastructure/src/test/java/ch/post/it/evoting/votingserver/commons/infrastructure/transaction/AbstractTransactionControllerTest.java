/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ejb.EJBContext;
import javax.ejb.EJBException;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link AbstractTransactionController}.
 */
public class AbstractTransactionControllerTest {
	private static final Exception SYSTEM_EXCEPTION = new EJBException("test");

	private static final Exception TRANSACTIONAL_ACTION_EXCEPTION = new TransactionalActionException(new Exception("test"));

	private EJBContext context;

	private TransactionalAction<Boolean> action;

	private RollbackPolicy policy;

	private AbstractTransactionController controller;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		context = mock(EJBContext.class);
		action = mock(TransactionalAction.class);
		when(action.execute(any(TransactionContext.class))).thenReturn(true);
		policy = mock(RollbackPolicy.class);
		when(policy.impliesRollback(SYSTEM_EXCEPTION)).thenReturn(true);
		when(policy.impliesRollback(TRANSACTIONAL_ACTION_EXCEPTION)).thenReturn(false);
		controller = new TestableTransactionController();
		controller.setContext(context);
	}

	@Test
	public void testDoInNewTransactionTransactionalActionOfT() throws TransactionalActionException {
		assertTrue(controller.doInNewTransaction(action));
	}

	@Test(expected = TransactionalActionException.class)
	public void testDoInNewTransactionTransactionalActionOfTApplicationException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(TRANSACTIONAL_ACTION_EXCEPTION);
		try {
			controller.doInNewTransaction(action);
		} catch (TransactionalActionException e) {
			assertEquals(TRANSACTIONAL_ACTION_EXCEPTION, e.getCause());
			verify(context, never()).setRollbackOnly();
			throw e;
		}
	}

	@Test
	public void testDoInNewTransactionTransactionalActionOfTRollbackPolicy() throws TransactionalActionException {
		assertTrue(controller.doInNewTransaction(action, policy));
	}

	@Test(expected = EJBException.class)
	public void testDoInNewTransactionTransactionalActionOfTRollbackPolicyEJBException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(SYSTEM_EXCEPTION);
		try {
			controller.doInNewTransaction(action, policy);
		} catch (EJBException e) {
			assertEquals(SYSTEM_EXCEPTION, e);
			verify(context).setRollbackOnly();
			throw e;
		}
	}

	@Test(expected = TransactionalActionException.class)
	public void testDoInNewTransactionTransactionalActionOfTRollbackPolicyIOException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(TRANSACTIONAL_ACTION_EXCEPTION);
		try {
			controller.doInNewTransaction(action, policy);
		} catch (TransactionalActionException e) {
			assertEquals(TRANSACTIONAL_ACTION_EXCEPTION, e.getCause());
			verify(context, never()).setRollbackOnly();
			throw e;
		}
	}

	@Test(expected = EJBException.class)
	public void testDoInNewTransactionTransactionalActionOfTSystemException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(SYSTEM_EXCEPTION);
		try {
			controller.doInNewTransaction(action);
		} catch (EJBException e) {
			assertEquals(SYSTEM_EXCEPTION, e);
			verify(context).setRollbackOnly();
			throw e;
		}
	}

	@Test
	public void testDoInTransactionTransactionalActionOfT() throws TransactionalActionException {
		assertTrue(controller.doInTransaction(action));
	}

	@Test(expected = TransactionalActionException.class)
	public void testDoInTransactionTransactionalActionOfTApplicationException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(TRANSACTIONAL_ACTION_EXCEPTION);
		try {
			controller.doInTransaction(action);
		} catch (TransactionalActionException e) {
			assertEquals(TRANSACTIONAL_ACTION_EXCEPTION, e.getCause());
			verify(context, never()).setRollbackOnly();
			throw e;
		}
	}

	@Test
	public void testDoInTransactionTransactionalActionOfTRollbackPolicy() throws TransactionalActionException {
		assertTrue(controller.doInTransaction(action, policy));
	}

	@Test(expected = TransactionalActionException.class)
	public void testDoInTransactionTransactionalActionOfTRollbackPolicyApplicationException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(TRANSACTIONAL_ACTION_EXCEPTION);
		try {
			controller.doInTransaction(action, policy);
		} catch (TransactionalActionException e) {
			assertEquals(TRANSACTIONAL_ACTION_EXCEPTION, e.getCause());
			verify(context, never()).setRollbackOnly();
			throw e;
		}
	}

	@Test(expected = EJBException.class)
	public void testDoInTransactionTransactionalActionOfTRollbackPolicySystemException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(SYSTEM_EXCEPTION);
		try {
			controller.doInTransaction(action, policy);
		} catch (EJBException e) {
			assertEquals(SYSTEM_EXCEPTION, e);
			verify(context).setRollbackOnly();
			throw e;
		}
	}

	@Test(expected = EJBException.class)
	public void testDoInTransactionTransactionalActionOfTSystemException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(SYSTEM_EXCEPTION);
		try {
			controller.doInTransaction(action);
		} catch (EJBException e) {
			assertEquals(SYSTEM_EXCEPTION, e);
			verify(context).setRollbackOnly();
			throw e;
		}
	}

	@Test
	public void testDoOutOfTransaction() throws TransactionalActionException {
		assertTrue(controller.doOutOfTransaction(action));
	}

	@Test(expected = TransactionalActionException.class)
	public void testDoOutOfTransactionApplicationException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(TRANSACTIONAL_ACTION_EXCEPTION);
		try {
			controller.doOutOfTransaction(action);
		} catch (TransactionalActionException e) {
			assertEquals(TRANSACTIONAL_ACTION_EXCEPTION, e.getCause());
			throw e;
		}
	}

	@Test(expected = EJBException.class)
	public void testDoOutOfTransactionSystemException() throws Exception {
		when(action.execute(any(TransactionContext.class))).thenThrow(SYSTEM_EXCEPTION);
		try {
			controller.doOutOfTransaction(action);
		} catch (EJBException e) {
			assertEquals(SYSTEM_EXCEPTION, e);
			throw e;
		}
	}

	private static class TestableTransactionController extends AbstractTransactionController {
	}
}
