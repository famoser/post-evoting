/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

public class EncryptedConfirmationKeyExponentiationLog extends EncryptedExponentiationLog {

	protected EncryptedConfirmationKeyExponentiationLog(ControlComponentContext context, String verificationCardId) {
		super("GENPVCC", context, verificationCardId);
	}
}
