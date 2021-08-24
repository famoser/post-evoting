/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnegative;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;

/**
 * Specification for the CCM_j election keys
 */
public final class CcmjElectionKeysSpec {

	private final String electionEventId;
	private final String electoralAuthorityId;
	private final ElGamalEncryptionParameters parameters;
	private final int length;

	private CcmjElectionKeysSpec(final String electionEventId, final String electoralAuthorityId, final ElGamalEncryptionParameters parameters,
			final int length) {
		this.electionEventId = electionEventId;
		this.electoralAuthorityId = electoralAuthorityId;
		this.parameters = parameters;
		this.length = length;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getElectoralAuthorityId() {
		return electoralAuthorityId;
	}

	@Nonnegative
	public int getLength() {
		return length;
	}

	public ElGamalEncryptionParameters getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return String.format("CcmjElectionKeysSpec [electionEventId=%s, electoralAuthorityId=%s, parameters=%s, length=%s]", electionEventId,
				electoralAuthorityId, parameters, length);
	}

	/**
	 * Builder for creating {@link CcmjElectionKeysSpec} instances.
	 */
	public static final class Builder {

		private String electionEventId;
		private String electoralAuthorityId;
		private ElGamalEncryptionParameters parameters;
		private int length;

		public CcmjElectionKeysSpec build() {
			requireNonNull(electionEventId, "Election event id is null.");
			requireNonNull(electoralAuthorityId, "Electoral authority id is null.");
			requireNonNull(parameters, "Parameters are null.");
			if (length < 0) {
				throw new IllegalArgumentException("Length is negative.");
			}
			return new CcmjElectionKeysSpec(electionEventId, electoralAuthorityId, parameters, length);
		}

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setElectoralAuthorityId(final String electoralAuthorityId) {
			this.electoralAuthorityId = electoralAuthorityId;
			return this;
		}

		public Builder setLength(
				@Nonnegative
				final int length) {
			this.length = length;
			return this;
		}

		public Builder setParameters(final ElGamalEncryptionParameters parameters) {
			this.parameters = parameters;
			return this;
		}
	}
}
