/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnegative;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;

/**
 * Specification for the CCR_j Return Codes keys.
 */
public final class CcrjReturnCodesKeysSpec {

	private final String electionEventId;
	private final String verificationCardSetId;
	private final ElGamalEncryptionParameters parameters;
	private final int ccrjReturnCodesGenerationKeyLength;
	private final int ccrjChoiceReturnCodesEncryptionKeyLength;

	private CcrjReturnCodesKeysSpec(final String electionEventId, final String verificationCardSetId, final ElGamalEncryptionParameters parameters,
			final int ccrjReturnCodesGenerationKeyLength, final int ccrjChoiceReturnCodesEncryptionKeyLength) {

		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.parameters = parameters;
		this.ccrjReturnCodesGenerationKeyLength = ccrjReturnCodesGenerationKeyLength;
		this.ccrjChoiceReturnCodesEncryptionKeyLength = ccrjChoiceReturnCodesEncryptionKeyLength;
	}

	@Nonnegative
	public int getCcrjChoiceReturnCodesEncryptionKeyLength() {
		return ccrjChoiceReturnCodesEncryptionKeyLength;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	@Nonnegative
	public int getCcrjReturnCodesGenerationKeyLength() {
		return ccrjReturnCodesGenerationKeyLength;
	}

	public ElGamalEncryptionParameters getParameters() {
		return parameters;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	@Override
	public String toString() {
		return String
				.format("CcrjReturnCodesKeysSpec [electionEventId=%s, verificationCardSetId=%s, parameters=%s, ccrjReturnCodesGenerationKeyLength=%s, ccrjChoiceReturnCodesEncryptionKeyLength=%s]",
						electionEventId, verificationCardSetId, parameters, ccrjReturnCodesGenerationKeyLength,
						ccrjChoiceReturnCodesEncryptionKeyLength);
	}

	/**
	 * Builder for creating {@link CcrjReturnCodesKeysSpec} instances.
	 */
	public static final class Builder {

		private String electionEventId;
		private String verificationCardSetId;
		private ElGamalEncryptionParameters parameters;
		private int ccrjReturnCodesGenerationKeyLength;
		private int ccrjChoiceReturnCodesEncryptionKeyLength;

		public CcrjReturnCodesKeysSpec build() {
			requireNonNull(electionEventId, "Election event id is null.");
			requireNonNull(verificationCardSetId, "Verification card set id is null.");
			requireNonNull(parameters, "Parameters are null.");

			if (ccrjReturnCodesGenerationKeyLength < 0) {
				throw new IllegalArgumentException("CcrjReturnCodesGenerationKeyLength is negative.");
			}

			if (ccrjChoiceReturnCodesEncryptionKeyLength < 0) {
				throw new IllegalArgumentException("CcrjChoiceReturnCodesEncryptionKeyLength is negative.");
			}

			return new CcrjReturnCodesKeysSpec(electionEventId, verificationCardSetId, parameters, ccrjReturnCodesGenerationKeyLength,
					ccrjChoiceReturnCodesEncryptionKeyLength);
		}

		public Builder setCcrjChoiceReturnCodesEncryptionKeyLength(
				@Nonnegative
				final int ccrjChoiceReturnCodesEncryptionKeyLength) {
			this.ccrjChoiceReturnCodesEncryptionKeyLength = ccrjChoiceReturnCodesEncryptionKeyLength;
			return this;
		}

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setCcrjReturnCodesGenerationKeyLength(
				@Nonnegative
				final int ccrjReturnCodesGenerationKeyLength) {
			this.ccrjReturnCodesGenerationKeyLength = ccrjReturnCodesGenerationKeyLength;
			return this;
		}

		public Builder setParameters(ElGamalEncryptionParameters parameters) {
			this.parameters = parameters;
			return this;
		}

		public Builder setVerificationCardSetId(String verificationCardSetId) {
			this.verificationCardSetId = verificationCardSetId;
			return this;
		}
	}
}
