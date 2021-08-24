/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

public class EncryptedPartialChoiceReturnCodeExponentiationLog extends EncryptedExponentiationLog {

	protected EncryptedPartialChoiceReturnCodeExponentiationLog(ControlComponentContext context, String verificationCardId) {
		super("GENPCC", context, verificationCardId);
	}
}
