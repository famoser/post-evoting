/*
 *  (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.regex.Pattern;

import org.msgpack.core.annotations.VisibleForTesting;

import ch.post.it.evoting.domain.mixnet.exceptions.FailedValidationException;

public final class Validations {

	@VisibleForTesting
	static final int UUID_LENGTH = 32;

	@VisibleForTesting
	static final String UUID_ALPHABET = "0123456789abcdef";

	private static final String UUID_REGEX = String.format("^[%s]{%d}$", UUID_ALPHABET, UUID_LENGTH);
	private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

	private Validations() {
		// Intentionally left blank.
	}

	/**
	 * Validates that the input string is a valid UUID according to the predefined pattern.
	 *
	 * @param toValidate the string to validate. Must be non-null.
	 * @throws NullPointerException      if the string is null.
	 * @throws FailedValidationException if the string validation fails.
	 */
	public static void validateUUID(final String toValidate) {
		checkNotNull(toValidate);

		if (!UUID_PATTERN.matcher(toValidate).matches()) {
			throw new FailedValidationException(
					String.format("The given string (%s) does not comply with the required UUID format (%s).", toValidate, UUID_REGEX));
		}
	}
}
