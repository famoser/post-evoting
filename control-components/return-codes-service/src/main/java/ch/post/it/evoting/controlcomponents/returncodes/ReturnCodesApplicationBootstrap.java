/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;

import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.NodeKeysActivator;
import ch.post.it.evoting.controlcomponents.returncodes.securelogger.SecureLogAppender;

@Component
public class ReturnCodesApplicationBootstrap {

	private static final String KEY_ALIAS = "ccncakey";

	private final NodeKeysActivator nodeKeysActivator;
	private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
	private final String keyStoreFile;
	private final String passwordFile;

	public ReturnCodesApplicationBootstrap(final NodeKeysActivator nodeKeysActivator,
			final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry,
			@Value("${keys.keystore.dir}/${keystore}")
			final String keyStoreFile,
			@Value("${keys.keystore.dir}/${keystore.password.file}")
			final String passwordFile) {
		this.nodeKeysActivator = nodeKeysActivator;
		this.rabbitListenerEndpointRegistry = rabbitListenerEndpointRegistry;
		this.keyStoreFile = keyStoreFile;
		this.passwordFile = passwordFile;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void bootstrap() throws IOException, KeyManagementException {

		final Path keyStorePath = getFilePath(keyStoreFile);
		final Path passwordPath = getFilePath(passwordFile);

		nodeKeysActivator.activateNodeKeys(keyStorePath, KEY_ALIAS, passwordPath);

		// Log initial checkpoint of Secure Logs
		final SecureLogAppender secureLogAppender = SecureLogAppender.getAppender();
		secureLogAppender.logInitialCheckpoint();

		rabbitListenerEndpointRegistry.getListenerContainers().forEach(Lifecycle::start);
	}

	private Path getFilePath(final String file) throws IOException {
		final Resource resource = new DefaultResourceLoader().getResource(file);
		return Paths.get(resource.getFile().getPath());
	}
}
