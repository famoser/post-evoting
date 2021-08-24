/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;

@SuppressWarnings("unused")
@JsonPropertyOrder({ "p", "q", "generator", "identity" })
public abstract class GqGroupMixIn {

	@JsonProperty
	@JsonSerialize(converter = BigIntegerToHexConverter.class)
	@JsonDeserialize(converter = HexToBigIntegerConverter.class)
	BigInteger p;

	@JsonProperty
	@JsonSerialize(converter = BigIntegerToHexConverter.class)
	@JsonDeserialize(converter = HexToBigIntegerConverter.class)
	BigInteger q;

	@JsonProperty("g")
	@JsonDeserialize(converter = HexToBigIntegerConverter.class)
	GqElement generator;

	@JsonIgnore
	GqElement identity;

	@JsonCreator
	GqGroupMixIn(
			@JsonProperty(value = "p", required = true)
			final BigInteger p,
			@JsonProperty(value = "q", required = true)
			final BigInteger q,
			@JsonProperty(value = "g", required = true)
			final BigInteger g) {
	}

}
