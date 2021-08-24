/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.cache;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Local;
import javax.ejb.Singleton;

import ch.post.it.evoting.votingserver.commons.cache.Cache;
import ch.post.it.evoting.votingserver.commons.cache.KeyStorePasswordCache;

@Singleton
@Local(value = Cache.class)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ChoiceCodesEncryptionKeystorePasswordCache extends KeyStorePasswordCache {

	public ChoiceCodesEncryptionKeystorePasswordCache() {
		super("ChoiceCodesEncryptionKeystorePassword");
	}

}
