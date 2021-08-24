/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.signature;

import static java.util.Objects.requireNonNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Implementation of {@link OutputStream} as a decorator which updates a given {@link Signature} while reading data from the decorated {@link
 * OutputStream}.
 * <p>
 * A typical usage of the class looks like the following:
 *
 * <pre>
 * <code>
 * SignatureFactory factory = ...
 * Signature signature = factory.newSignature();
 * signature.initSign(privateKey);
 * try (OutputStream stream = new SignatureOutputStream(..., signature)) {
 *     // write the data
 *     ...
 * }
 * byte[] bytes = signature.sign();
 * </code>
 * </pre>
 * <p>
 * Buffering is not used so all the bytes passed to {@code write} methods are immediately written to the underlying stream and are used to update the
 * supplied {@link Signature}. The following code demonstrates how write data with the signature to the same {@link OutputStream}:
 *
 * <pre>
 * <code>
 * SignatureFactory factory = ...
 * Signature signature = factory.newSignature();
 * signature.initSign(privateKey);
 * try (OutputStream stream = ...) {
 *     SignatureOutputStream signatureStream = new SignatureOutputStream(stream);
 *     signatureStream.write(data);
 *     stream.write(SEPARATOR);
 *     stream.write(Base64.getEncoder().encodeToString(signature.sign()));
 * }
 * </code>
 * </pre>
 */
public final class SignatureOutputStream extends FilterOutputStream {
	private final Signature signature;

	/**
	 * Constructor.
	 *
	 * @param out       the underlying output stream
	 * @param signature the signature.
	 */
	public SignatureOutputStream(final OutputStream out, final Signature signature) {
		super(out);
		this.signature = requireNonNull(signature, "Signature is null.");
	}

	@Override
	public void write(final int b) throws IOException {
		super.write(b);
		try {
			signature.update((byte) b);
		} catch (SignatureException e) {
			throw new IOException("failed ot update signature.", e);
		}
	}
}
