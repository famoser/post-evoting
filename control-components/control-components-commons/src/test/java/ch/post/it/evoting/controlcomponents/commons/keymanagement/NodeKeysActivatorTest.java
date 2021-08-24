/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore.PasswordProtection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidPasswordException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyNotFoundException;

/**
 * Tests of {@link NodeKeysActivator}.
 */
class NodeKeysActivatorTest {
	private Path keyStoreFile;

	private Path passwordFile;

	private KeysManager manager;

	private NodeKeysActivator activator;

	@BeforeEach
	public void setUp() throws IOException {
		keyStoreFile = createTempFile("keys", ".p12");
		passwordFile = createTempFile("password", null);
		write(passwordFile, "password".getBytes(StandardCharsets.UTF_8));
		manager = mock(KeysManager.class);
		activator = new NodeKeysActivator(manager);
	}

	@AfterEach
	public void tearDown() throws IOException {
		deleteIfExists(keyStoreFile);
		deleteIfExists(passwordFile);
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPath() throws IOException, KeyManagementException {
		activator.activateNodeKeys(keyStoreFile, "alias", passwordFile);
		assertFalse(exists(passwordFile));
		verify(manager, never()).createAndActivateNodeKeys(any(Path.class), anyString(), any());
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoDatabase() throws IOException, KeyManagementException {
		doThrow(new KeyNotFoundException("test")).when(manager).activateNodeKeys(any());
		doAnswer((Answer<Void>) invocation -> {
			PasswordProtection protection = invocation.getArgument(2, PasswordProtection.class);
			assertEquals("password", new String(protection.getPassword()));
			return null;
		}).when(manager).createAndActivateNodeKeys(any(Path.class), eq("alias"), any());
		activator.activateNodeKeys(keyStoreFile, "alias", passwordFile);
		assertFalse(exists(passwordFile));
		verify(manager).createAndActivateNodeKeys(eq(keyStoreFile), eq("alias"), argThat(PasswordProtection::isDestroyed));
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoDatabaseFiles() throws IOException, KeyManagementException {
		delete(keyStoreFile);
		delete(passwordFile);
		doThrow(new KeyNotFoundException("test")).when(manager).activateNodeKeys(any());
		assertThrows(NoSuchFileException.class, () -> activator.activateNodeKeys(keyStoreFile, "alias", passwordFile));
		assertFalse(exists(passwordFile));
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoDatabaseInvalidPassword() throws IOException, KeyManagementException {
		doThrow(new KeyNotFoundException("test")).when(manager).activateNodeKeys(any());
		doThrow(new InvalidPasswordException("test")).when(manager).createAndActivateNodeKeys(any(Path.class), anyString(), any());
		assertThrows(KeyManagementException.class, () -> activator.activateNodeKeys(keyStoreFile, "alias", passwordFile));
		assertFalse(exists(passwordFile));
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoDatabaseKeyStore() throws IOException, KeyManagementException {
		delete(keyStoreFile);
		doThrow(new KeyNotFoundException("test")).when(manager).activateNodeKeys(any());
		doThrow(new NoSuchFileException("test")).when(manager).createAndActivateNodeKeys(any(Path.class), anyString(), any());
		assertThrows(NoSuchFileException.class, () -> activator.activateNodeKeys(keyStoreFile, "alias", passwordFile));
		assertFalse(exists(passwordFile));
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoDatabasePassword() throws IOException, KeyManagementException {
		delete(passwordFile);
		doThrow(new KeyNotFoundException("test")).when(manager).activateNodeKeys(any());
		assertThrows(NoSuchFileException.class, () -> activator.activateNodeKeys(keyStoreFile, "alias", passwordFile));
		assertFalse(exists(passwordFile));
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoFiles() throws IOException {
		delete(keyStoreFile);
		delete(passwordFile);
		assertThrows(NoSuchFileException.class, () -> activator.activateNodeKeys(keyStoreFile, "alias", passwordFile));
		assertFalse(exists(passwordFile));
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoKeyStore() throws IOException, KeyManagementException {
		delete(keyStoreFile);
		activator.activateNodeKeys(keyStoreFile, "alias", passwordFile);
		assertFalse(exists(passwordFile));
		verify(manager, never()).createAndActivateNodeKeys(any(Path.class), anyString(), any());
	}

	@Test
	void testActivateNodeKeysKeyManagerPathStringPathNoKeyStoreInvalidPassword() throws IOException, KeyManagementException {
		delete(keyStoreFile);
		doThrow(new InvalidPasswordException("test")).when(manager).activateNodeKeys(any());
		assertThrows(InvalidPasswordException.class, () -> activator.activateNodeKeys(keyStoreFile, "alias", passwordFile));
		assertFalse(exists(passwordFile));
	}
}
