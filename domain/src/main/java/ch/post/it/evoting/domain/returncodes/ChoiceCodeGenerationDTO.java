/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "correlationId", "requestId", "payload" })
public class ChoiceCodeGenerationDTO<T> extends CorrelatedSupport {

	@JsonProperty
	private final String requestId;

	@JsonProperty
	private final T payload;

	@JsonCreator
	public ChoiceCodeGenerationDTO(
			@JsonProperty("correlationId")
			final UUID correlationId,
			@JsonProperty("requestId")
			final String requestId,
			@JsonProperty("payload")
			final T payload) {

		super(checkNotNull(correlationId));
		this.requestId = checkNotNull(requestId);
		this.payload = checkNotNull(payload);
	}

	public String getRequestId() {
		return requestId;
	}

	public T getPayload() {
		return payload;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ChoiceCodeGenerationDTO<?> that = (ChoiceCodeGenerationDTO<?>) o;
		return requestId.equals(that.requestId) && payload.equals(that.payload);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, payload);
	}
}
