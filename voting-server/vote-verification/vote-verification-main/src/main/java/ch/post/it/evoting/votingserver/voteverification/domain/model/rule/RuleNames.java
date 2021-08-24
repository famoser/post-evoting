/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

/**
 * The rules defined in this context.
 */
public enum RuleNames {

	VOTE_PLAINTEXT_EQUALITY_PROOF("plaintext_equality_proof"),
	VOTE_EXPONENTIATION_PROOF("exponentiation_proof"),
	VOTE_VERIFICATION_CARD_PUBLIC_KEY_SIGNATURE_VALIDATION("verification_card_public_key_signature_validation"),
	VOTE_EXPONENTIATED_CIPHER_TEXT("exponentiated_ciphertext_rule");

	/**
	 * The actual name of the rule.
	 */
	private final String text;

	RuleNames(String name) {
		this.text = name;
	}

	/**
	 * Returns the enum element for a given text.
	 *
	 * @param ruleName2Find The rule to be searched in the enum.
	 * @return a RulesName element having the given text.
	 */
	public static RuleNames getRuleName4Text(String ruleName2Find) {
		RuleNames result = null;
		for (RuleNames ruleName : RuleNames.values()) {
			if (ruleName.getText().equals(ruleName2Find)) {
				result = ruleName;
				break;
			}
		}
		return result;
	}

	/**
	 * Returns the current text of the field text.
	 *
	 * @return Returns the text.
	 */
	public String getText() {
		return text;
	}
}
