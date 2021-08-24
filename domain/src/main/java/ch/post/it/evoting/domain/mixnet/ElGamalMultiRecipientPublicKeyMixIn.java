/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;

@SuppressWarnings({ "java:S1610", "unused" })
public abstract class ElGamalMultiRecipientPublicKeyMixIn {

	@JsonCreator
	ElGamalMultiRecipientPublicKeyMixIn(final List<GqElement> keyElements) {
	}

	@JsonValue
	abstract List<GqElement> getKeyElements();

}
