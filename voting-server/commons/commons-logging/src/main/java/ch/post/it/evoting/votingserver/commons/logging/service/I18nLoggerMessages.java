/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.logging.service;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class for internationalization of logging messages.
 */
public enum I18nLoggerMessages {

	INSTANCE;

	private static final Logger LOGGER = LoggerFactory.getLogger(I18nLoggerMessages.class);

	private static final String BUNDLE_NAME = "loggerMessages";

	private static final Locale CURRENT_LOCALE = Locale.getDefault();

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, CURRENT_LOCALE);

	/**
	 * Obtains an instance of the I18nLoggerMessages.
	 *
	 * @return Class instance
	 */
	public static I18nLoggerMessages getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns a message from a bundle.
	 *
	 * @param key The key for search the message in the message bundle.
	 * @return The message.
	 */
	public String getMessage(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException mrE) {
			LOGGER.error("Internationalization logging error: The key {} is not defined!", key, mrE);
			return "Internationalization logging error: The key " + key + " is not defined!";
		}
	}

	/**
	 * Returns a message from a bundle, filling the arguments.
	 *
	 * @param key  The key for search the message in the message bundle.
	 * @param args The arguments, if there are any.
	 * @return The message.
	 */
	public String getMessage(String key, Object... args) {
		return MessageFormat.format(getMessage(key), args);
	}
}
