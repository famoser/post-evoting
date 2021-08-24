/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.votingserver;

import java.security.Security;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.logging.log4j.LogManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@Singleton
@Startup
public class BouncyCastleLoader {

	@PostConstruct
	public void addBouncyCastleProvider() {
		Security.addProvider(new BouncyCastleProvider());
		LogManager.getLogger(BouncyCastleLoader.class).info("Added BouncyCastle as a security provider.");
	}
}
