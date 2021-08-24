/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.config.commands.primes;

public class PrimesParametersHolder {

	private final String p12Path;

	private final String encryptionParametersPath;

	private final String outputPath;

	private final String trustedCAPath;

	PrimesParametersHolder(final String p12Path, final String encryptionParametersPath, final String trustedCAPath, final String outputPath) {
		this.p12Path = p12Path;
		this.encryptionParametersPath = encryptionParametersPath;
		this.trustedCAPath = trustedCAPath;
		this.outputPath = outputPath;
	}

	public String getEncryptionParametersPath() {
		return encryptionParametersPath;
	}

	public String getP12Path() {
		return p12Path;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public String getTrustedCAPath() {
		return trustedCAPath;
	}
}
