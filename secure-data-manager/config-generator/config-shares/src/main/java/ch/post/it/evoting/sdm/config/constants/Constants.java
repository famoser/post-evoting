/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.constants;

public final class Constants {

	public static final int MAX_EXPONENT_SIZE = 257;

	public static final String CSR_SIGNING_ALGORITHM = "SHA256withRSA";

	public static final String CSR_BEGIN_STRING = "-----BEGIN CERTIFICATE REQUEST-----";

	public static final String CSR_END_STRING = "-----END CERTIFICATE REQUEST-----";

	private Constants() {
	}
}
