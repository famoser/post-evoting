/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.primitives.securerandom.constants;

/**
 * Class contains Constants that are used for Secure Random Generators.
 */
public final class SecureRandomConstants {

	/**
	 * The maximum length of randomly generated byte arrays.
	 */
	public static final int MAXIMUM_GENERATED_BYTE_ARRAY_LENGTH = 10000;

	/**
	 * The maximum length in digits of randomly generated BigIntegers.
	 */
	public static final int MAXIMUM_GENERATED_BIG_INTEGER_DIGIT_LENGTH = 100;

	/**
	 * The minimum length in digits of randomly generated BigIntegers.
	 */
	public static final int MINIMUM_GENERATED_BIG_INTEGER_DIGIT_LENGTH = 1;

	/**
	 * The maximum length in characters of randomly generated Strings.
	 */
	public static final int MAXIMUM_GENERATED_STRING_LENGTH = 100;

	/**
	 * A set of 32 alphanumeric characters (RFC 4648 base32 alphabet).
	 *
	 * <p>Contains:
	 *
	 * <ul>
	 *   <li>The 26 uppercase English letter characters
	 *   <li>the numbers 2 - 7 (inclusive)
	 * </ul>
	 */
	public static final String ALPHABET_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	/**
	 * A set of 64 characters (RFC 4648 base64 alphabet).
	 *
	 * <p>Contains:
	 *
	 * <ul>
	 *   <li>The 52 English letter characters (lowercase and uppercase).
	 *   <li>The number characters 0 - 9 (inclusive).
	 *   <li>The 'plus' and 'forward slash' characters.
	 * </ul>
	 */
	public static final String ALPHABET_BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	private SecureRandomConstants() {
	}
}
