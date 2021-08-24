/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.transaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.ejb.ApplicationException;

import org.junit.Test;

/**
 * Tests of {@link EJBRollbackPolicy}.
 */
public class EJBRollbackPolicyTest {
	@Test
	public void testImpliesRollbackChecked() {
		EJBRollbackPolicy policy = EJBRollbackPolicy.getInstance();
		assertFalse(policy.impliesRollback(new IOException("test")));
	}

	@Test
	public void testImpliesRollbackRemote() {
		EJBRollbackPolicy policy = EJBRollbackPolicy.getInstance();
		assertTrue(policy.impliesRollback(new RemoteException()));
	}

	@Test
	public void testImpliesRollbackUncheckedA() {
		EJBRollbackPolicy policy = EJBRollbackPolicy.getInstance();
		assertTrue(policy.impliesRollback(new ExceptionA()));
	}

	@Test
	public void testImpliesRollbackUncheckedB() {
		EJBRollbackPolicy policy = EJBRollbackPolicy.getInstance();
		assertTrue(policy.impliesRollback(new ExceptionB()));
	}

	@Test
	public void testImpliesRollbackUncheckedC() {
		EJBRollbackPolicy policy = EJBRollbackPolicy.getInstance();
		assertFalse(policy.impliesRollback(new ExceptionC()));
	}

	@Test
	public void testImpliesRollbackUncheckedD() {
		EJBRollbackPolicy policy = EJBRollbackPolicy.getInstance();
		assertTrue(policy.impliesRollback(new ExceptionD()));
	}

	@ApplicationException(rollback = true)
	public static class ExceptionA extends RuntimeException {
		private static final long serialVersionUID = -2059792068032343479L;
	}

	public static class ExceptionB extends ExceptionA {
		private static final long serialVersionUID = -230438548958479539L;
	}

	@ApplicationException(inherited = false, rollback = false)
	public static class ExceptionC extends ExceptionB {
		private static final long serialVersionUID = 6761900516813693832L;
	}

	public static class ExceptionD extends ExceptionC {
		private static final long serialVersionUID = 4715666027348599771L;
	}
}
