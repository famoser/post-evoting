/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.readAllBytes;
import static java.text.MessageFormat.format;
import static java.util.Arrays.fill;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore.PasswordProtection;

import javax.security.auth.DestroyFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidKeyStoreException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidNodeCAException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidPasswordException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyAlreadyExistsException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyNotFoundException;

@Transactional(noRollbackFor = { InvalidKeyStoreException.class, InvalidPasswordException.class, InvalidNodeCAException.class,
		NoSuchFileException.class, IOException.class }, rollbackFor = KeyManagementException.class)
@Service
public class NodeKeysActivator {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeKeysActivator.class);

	private final KeysManager manager;

	public NodeKeysActivator(final KeysManager manager) {
		this.manager = manager;
	}

	private static void deletePasswordFile(Path passwordFile) throws IOException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(format("Deleting password file ''{0}''...", passwordFile));
		}
		deleteIfExists(passwordFile);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(format("Password file ''{0}'' has been deleted.", passwordFile));
		}
	}

	private static void destroyPassword(PasswordProtection password) {
		try {
			password.destroy();
		} catch (DestroyFailedException e) {
			LOGGER.warn("Failed to destroy password.", e);
		}
	}

	private static PasswordProtection loadPassword(Path file) throws IOException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(format("Loading password from file ''{0}''...", file));
		}
		ByteBuffer bytes = ByteBuffer.wrap(readAllBytes(file));
		try {
			CharBuffer chars = StandardCharsets.UTF_8.decode(bytes);
			try {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(format("Password has been loaded from file ''{0}''.", file));
				}
				return new PasswordProtection(chars.array());
			} finally {
				fill(chars.array(), '\u0000');
			}
		} finally {
			fill(bytes.array(), (byte) 0);
		}
	}

	public void activateNodeKeys(final Path keyStoreFile, final String nodeCAAlias, final Path passwordFile)
			throws KeyManagementException, IOException {
		checkNotNull(manager);
		checkNotNull(keyStoreFile);
		checkNotNull(passwordFile);

		PasswordProtection password = null;
		boolean activated = false;
		try {
			password = loadPassword(passwordFile);
			while (!activated) {
				try {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Activating existing node keys....");
					}
					manager.activateNodeKeys(password);
					LOGGER.info("Node keys have been activated.");
					activated = true;
				} catch (KeyNotFoundException nFE) {
					LOGGER.debug("Node keys have not been found.", nFE);
					try {
						manager.createAndActivateNodeKeys(keyStoreFile, nodeCAAlias, password);
						LOGGER.info("Node keys have been created and activated.");
						activated = true;
					} catch (KeyAlreadyExistsException aEE) {
						LOGGER.warn("Problem saving node keys, with retry.", aEE);
					}
				}
			}
		} finally {
			if (password != null) {
				destroyPassword(password);
			}
			deletePasswordFile(passwordFile);
		}
	}
}
