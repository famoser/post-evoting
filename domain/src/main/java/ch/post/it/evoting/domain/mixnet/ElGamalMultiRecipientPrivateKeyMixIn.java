/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

@SuppressWarnings("unused")
public abstract class ElGamalMultiRecipientPrivateKeyMixIn {

	@JsonValue
	GroupVector<ZqElement, ZqGroup> privateKeyElements;

	@JsonCreator
	ElGamalMultiRecipientPrivateKeyMixIn(final List<ZqElement> keyElements) {
	}

}
