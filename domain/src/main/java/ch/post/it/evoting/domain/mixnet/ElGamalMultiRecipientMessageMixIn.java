/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

@SuppressWarnings("unused")
public abstract class ElGamalMultiRecipientMessageMixIn {

	@JsonProperty("message")
	GroupVector<GqElement, GqGroup> messageElements;

	ElGamalMultiRecipientMessageMixIn(
			@JsonProperty(value = "message", required = true)
			final List<GqElement> messageElements) {
	}

	@JsonIgnore
	abstract GqGroup getGroup();

	@JsonIgnore
	abstract GroupVector<GqElement, GqGroup> getElements();

}
