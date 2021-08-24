/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.configuration;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Defines character sets that are supported during the generation and verification of proofs.
 */
public enum ConfigProofHashCharset {
	UTF8(StandardCharsets.UTF_8);

	private final Charset charset;

	ConfigProofHashCharset(final Charset algorithm) {
		charset = algorithm;
	}

	public Charset getCharset() {
		return charset;
	}
}
