/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Keys identifier used in the keys cache.
 */
final class KeysId implements Serializable {
	private final Class<?> keysClass;

	private final String[] tags;

	private KeysId(Class<?> keysClass, String... tags) {
		this.keysClass = keysClass;
		this.tags = tags;
	}

	/**
	 * Returns a CCR_j Return Codes keys instance for a given election event and verification card set.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @return an instance.
	 */
	public static KeysId getCcrjReturnCodesKeysInstance(String electionEventId, String verificationCardSetId) {
		return new KeysId(CcrjReturnCodesKeys.class, electionEventId, verificationCardSetId);
	}

	/**
	 * Returns an election signing keys instance for a given election event.
	 *
	 * @param electionEventId the election event identifier
	 * @return an instance.
	 */
	public static KeysId getElectionSigningInstance(String electionEventId) {
		return new KeysId(ElectionSigningKeys.class, electionEventId);
	}

	/**
	 * Returns a CCM_j election keys instance for a given election event.
	 *
	 * @param electionEventId the election event identifier
	 * @return an instance.
	 */
	public static KeysId getCcmjElectionKeysInstance(final String electionEventId) {
		return new KeysId(CcmjElectionKeys.class, electionEventId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		KeysId other = (KeysId) obj;
		if (keysClass == null) {
			if (other.keysClass != null) {
				return false;
			}
		} else if (!keysClass.equals(other.keysClass)) {
			return false;
		}
		return Arrays.equals(tags, other.tags);
	}

	@Override
	public int hashCode() {
		return Objects.hash(keysClass, Arrays.asList(tags));
	}
}
