/*
 *  (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import static ch.post.it.evoting.domain.Validations.UUID_ALPHABET;
import static ch.post.it.evoting.domain.Validations.UUID_LENGTH;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

import ch.post.it.evoting.cryptoprimitives.math.RandomService;

public class UUIDGenerator {

	private static final RandomService randomService = new RandomService();
	private static final Random random = new SecureRandom();

	private UUIDGenerator() {
		//Intentionally left blank
	}

	public static String genValidUUID() {
		return randomService.genRandomBase16String(UUID_LENGTH).toLowerCase(Locale.ROOT);
	}

	public static String genUUIDWithOneWrongAlphabetCharacter() {
		StringBuilder uuid = new StringBuilder(genValidUUID());
		int index = random.nextInt(UUID_LENGTH);
		char c = genCharacterNotInAlphabet();
		uuid.setCharAt(index, c);
		return uuid.toString();
	}

	private static char genCharacterNotInAlphabet() {
		char c;
		do {
			c = genRandomSingleCharacter();
		} while (UUID_ALPHABET.contains(String.valueOf(c)));
		return c;
	}

	private static char genRandomSingleCharacter() {
		int codePoint = random.nextInt(Character.MAX_VALUE);
		assert (Character.isValidCodePoint(codePoint));
		assert (Character.charCount(codePoint) == 1);
		return (char) codePoint;
	}

	public String genTooLongUUID() {
		return randomService.genRandomBase16String(UUID_LENGTH + 1).toLowerCase(Locale.ROOT);
	}
}
