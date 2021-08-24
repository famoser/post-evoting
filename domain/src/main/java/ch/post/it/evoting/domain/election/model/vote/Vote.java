/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.post.it.evoting.domain.election.errors.SemanticErrorGroup;
import ch.post.it.evoting.domain.election.errors.SyntaxErrorGroup;
import ch.post.it.evoting.domain.election.model.constants.Constants;
import ch.post.it.evoting.domain.election.model.constants.Patterns;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * This class represents the vote in this context.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vote {

	// The identifier of the tenant.
	@NotNull(groups = SyntaxErrorGroup.class)
	@Pattern(regexp = Patterns.ID, groups = SemanticErrorGroup.class)
	@Size(min = 1, max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	private String tenantId;

	// The identifier of the election event.
	@NotNull(groups = SyntaxErrorGroup.class)
	@Pattern(regexp = Patterns.ID, groups = SemanticErrorGroup.class)
	@Size(min = 1, max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	private String electionEventId;

	// The identifier of the ballot.
	@NotNull(groups = SyntaxErrorGroup.class)
	@Pattern(regexp = Patterns.ID, groups = SemanticErrorGroup.class)
	@Size(min = 1, max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	private String ballotId;

	// The identifier of the ballot box to which the vote belongs.
	@NotNull(groups = SyntaxErrorGroup.class)
	@Pattern(regexp = Patterns.ID, groups = SemanticErrorGroup.class)
	@Size(min = 1, max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	private String ballotBoxId;

	// The identifier of the voting card.
	@NotNull(groups = SyntaxErrorGroup.class)
	@Pattern(regexp = Patterns.ID, groups = SemanticErrorGroup.class)
	@Size(min = 1, max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	private String votingCardId;

	// The encrypted options as input.
	@NotNull(groups = SyntaxErrorGroup.class)
	private String encryptedOptions;

	// The partial choice codes.
	// @NotNull(groups = SyntaxErrorGroup.class)
	private String encryptedPartialChoiceCodes;

	// The correctness ids.
	@NotNull(groups = SyntaxErrorGroup.class)
	private String correctnessIds;

	// The verification card public key
	@NotNull(groups = SyntaxErrorGroup.class)
	private String verificationCardPublicKey;

	// Signature on the verification card public key
	// @NotNull(groups = SyntaxErrorGroup.class)
	private String verificationCardPKSignature;

	// Signature on the encrypted vote
	// @NotNull(groups = SyntaxErrorGroup.class)
	private String signature;

	// Signing certificate
	// @NotNull(groups = SyntaxErrorGroup.class)
	private String certificate;

	// Credential identifier
	@NotNull(groups = SyntaxErrorGroup.class)
	private String credentialId;

	// Authentication Token Signature
	private String authenticationTokenSignature;

	// Authentication token
	private String authenticationToken;

	/**
	 * The vote ciphertext that contains the exponentiated elements (C'0, C'1).
	 */
	private String cipherTextExponentiations;

	/**
	 * The exponentiation proof.
	 */
	private String exponentiationProof;

	/**
	 * The plaintext equality proof.
	 */
	private String plaintextEqualityProof;

	/**
	 * The identifier of the verification card.
	 */
	private String verificationCardId;

	/**
	 * The identifier of the verification card set.
	 */
	private String verificationCardSetId;

	/**
	 * Returns the current value of the field tenantId.
	 *
	 * @return Returns the tenantId.
	 */
	public String getTenantId() {
		return tenantId;
	}

	/**
	 * Sets the value of the field tenantId.
	 *
	 * @param tenantId The tenantId to set.
	 */
	public void setTenantId(final String tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * Returns the current value of the field electionEventId.
	 *
	 * @return Returns the electionEventId.
	 */
	public String getElectionEventId() {
		return electionEventId;
	}

	/**
	 * Sets the value of the field electionEventId.
	 *
	 * @param electionEventId The electionEventId to set.
	 */
	public void setElectionEventId(final String electionEventId) {
		this.electionEventId = electionEventId;
	}

	/**
	 * Returns the current value of the field votingCardId.
	 *
	 * @return Returns the votingCardId.
	 */
	public String getVotingCardId() {
		return votingCardId;
	}

	/**
	 * Sets the value of the field votingCardId.
	 *
	 * @param votingCardId The votingCardId to set.
	 */
	public void setVotingCardId(final String votingCardId) {
		this.votingCardId = votingCardId;
	}

	/**
	 * Returns the current value of the field encryptedOptions.
	 *
	 * @return Returns the encryptedOptions.
	 */

	public String getEncryptedOptions() {
		return encryptedOptions;
	}

	/**
	 * Sets the value of the field encryptedOptions
	 *
	 * @param encryptedOptions The vote to set.
	 */
	public void setEncryptedOptions(final String encryptedOptions) {
		this.encryptedOptions = encryptedOptions;
	}

	/**
	 * Returns the current value of the field ballotId.
	 *
	 * @return Returns the ballotId.
	 */
	public String getBallotId() {
		return ballotId;
	}

	/**
	 * Sets the value of the field ballotId.
	 *
	 * @param ballotId The ballotId to set.
	 */
	public void setBallotId(final String ballotId) {
		this.ballotId = ballotId;
	}

	/**
	 * Returns the current value of the field ballotBoxId.
	 *
	 * @return Returns the ballotBoxId.
	 */
	public String getBallotBoxId() {
		return ballotBoxId;
	}

	/**
	 * Sets the value of the field ballotBoxId.
	 *
	 * @param ballotBoxId The ballotBoxId to set.
	 */
	public void setBallotBoxId(final String ballotBoxId) {
		this.ballotBoxId = ballotBoxId;
	}

	/**
	 * Returns the current value of the field verificationCardPublicKey.
	 *
	 * @return Returns the verificationCardPublicKey.
	 */
	public String getVerificationCardPublicKey() {
		return verificationCardPublicKey;
	}

	/**
	 * Sets the value of the field verificationCardPublicKey.
	 *
	 * @param verificationCardPublicKey The verificationCardPublicKey to set.
	 */
	public void setVerificationCardPublicKey(final String verificationCardPublicKey) {
		this.verificationCardPublicKey = verificationCardPublicKey;
	}

	/**
	 * Returns the current value of the field verificationCardPKSignature.
	 *
	 * @return Returns the verificationCardPKSignature.
	 */
	public String getVerificationCardPKSignature() {
		return verificationCardPKSignature;
	}

	/**
	 * Sets the value of the field verificationCardPKSignature.
	 *
	 * @param verificationCardPKSignature The verificationCardPKSignature to set.
	 */
	public void setVerificationCardPKSignature(final String verificationCardPKSignature) {
		this.verificationCardPKSignature = verificationCardPKSignature;
	}

	/**
	 * Returns the current value of the field signature.
	 *
	 * @return Returns the signature.
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * Sets the value of the field signature.
	 *
	 * @param signature The signature to set.
	 */
	public void setSignature(final String signature) {
		this.signature = signature;
	}

	/**
	 * Returns the current value of the field certificate.
	 *
	 * @return Returns the certificate.
	 */
	public String getCertificate() {
		return certificate;
	}

	/**
	 * Sets the value of the field certificate.
	 *
	 * @param certificate The certificate to set.
	 */
	public void setCertificate(final String certificate) {
		this.certificate = certificate;
	}

	/**
	 * Returns the current value of the field credentialId.
	 *
	 * @return Returns the credentialId.
	 */
	public String getCredentialId() {
		return credentialId;
	}

	/**
	 * Sets the value of the field credentialId.
	 *
	 * @param credentialId The credentialId to set.
	 */
	public void setCredentialId(final String credentialId) {
		this.credentialId = credentialId;
	}

	/**
	 * Returns the current value of the field encryptedPartialChoiceCodes.
	 *
	 * @return Returns the encryptedPartialChoiceCodes.
	 */
	public String getEncryptedPartialChoiceCodes() {
		return encryptedPartialChoiceCodes;
	}

	/**
	 * Sets the value of the field encryptedPartialChoiceCodes.
	 *
	 * @param encryptedPartialChoiceCodes The encryptedPartialChoiceCodes to set.
	 */
	public void setEncryptedPartialChoiceCodes(final String encryptedPartialChoiceCodes) {
		this.encryptedPartialChoiceCodes = encryptedPartialChoiceCodes;
	}

	/**
	 * Returns the current value of the field authenticationTokenSignature.
	 *
	 * @return Returns the authenticationTokenSignature.
	 */
	public String getAuthenticationTokenSignature() {
		return authenticationTokenSignature;
	}

	/**
	 * Sets the value of the field authenticationTokenSignature.
	 *
	 * @param authenticationTokenSignature The authenticationTokenSignature to set.
	 */
	public void setAuthenticationTokenSignature(final String authenticationTokenSignature) {
		this.authenticationTokenSignature = authenticationTokenSignature;
	}

	public String getCorrectnessIds() {
		return correctnessIds;
	}

	public void setCorrectnessIds(String correctnessIds) {
		this.correctnessIds = correctnessIds;
	}

	/**
	 * Returns the current value of the field cipherTextExponentiations.
	 *
	 * @return Returns the cipherTextExponentiations.
	 */
	public String getCipherTextExponentiations() {
		return cipherTextExponentiations;
	}

	/**
	 * Sets the value of the field cipherTextExponentiations.
	 *
	 * @param cipherTextExponentiations The cipherTextExponentiations to set.
	 */
	public void setCipherTextExponentiations(final String cipherTextExponentiations) {
		this.cipherTextExponentiations = cipherTextExponentiations;
	}

	/**
	 * Returns the current value of the field exponentiationProof.
	 *
	 * @return Returns the exponentiationProof.
	 */
	public String getExponentiationProof() {
		return exponentiationProof;
	}

	/**
	 * Sets the value of the field exponentiationProof.
	 *
	 * @param exponentiationProof The exponentiationProof to set.
	 */
	public void setExponentiationProof(final String exponentiationProof) {
		this.exponentiationProof = exponentiationProof;
	}

	/**
	 * Returns the current value of the field plaintextEqualityProof.
	 *
	 * @return Returns the plaintextEqualityProof.
	 */
	public String getPlaintextEqualityProof() {
		return plaintextEqualityProof;
	}

	/**
	 * Sets the value of the field plaintextEqualityProof.
	 *
	 * @param plaintextEqualityProof The plaintextEqualityProof to set.
	 */
	public void setPlaintextEqualityProof(final String plaintextEqualityProof) {
		this.plaintextEqualityProof = plaintextEqualityProof;
	}

	/**
	 * Gets authenticationToken.
	 *
	 * @return Value of authenticationToken.
	 */
	public String getAuthenticationToken() {
		return authenticationToken;
	}

	/**
	 * Sets new authenticationToken.
	 *
	 * @param authenticationToken New value of authenticationToken.
	 */
	public void setAuthenticationToken(final String authenticationToken) {
		this.authenticationToken = authenticationToken;
	}

	/**
	 * Gets verificationCardId.
	 *
	 * @return Value of verificationCardId.
	 */
	public String getVerificationCardId() {
		return verificationCardId;
	}

	/**
	 * Sets the value of verification card Id
	 *
	 * @param verificationCardId
	 */
	public void setVerificationCardId(String verificationCardId) {
		this.verificationCardId = verificationCardId;
	}

	/**
	 * Gets verificationCardSetId.
	 *
	 * @return Value of verificationCardSetId.
	 */
	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	/**
	 * Sets the value of verification card set id
	 *
	 * @param verificationCardSetId
	 */
	public void setVerificationCardSetId(String verificationCardSetId) {
		this.verificationCardSetId = verificationCardSetId;
	}

	/**
	 * Return all the field values as a String array
	 *
	 * @return
	 */

	@JsonIgnore
	public String[] getFieldsAsStringArray() {
		return new String[] { encryptedOptions, correctnessIds, verificationCardPKSignature, authenticationTokenSignature, votingCardId,
				electionEventId };
	}

}
