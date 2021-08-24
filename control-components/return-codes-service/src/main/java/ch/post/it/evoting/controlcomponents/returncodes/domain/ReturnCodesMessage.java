/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import java.io.UncheckedIOException;

import org.apache.logging.log4j.message.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReturnCodesMessage implements Message {

	private final ObjectMapper mapper;
	private final transient ReturnCodes objectToSerialize;

	public ReturnCodesMessage(ObjectMapper mapper, ReturnCodes objectToSerialize) {
		this.mapper = mapper;
		this.objectToSerialize = objectToSerialize;
	}

	@Override
	public String getFormattedMessage() {
		try {
			return mapper.writeValueAsString(objectToSerialize);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize object log", e);
		}
	}

	@Override
	public String getFormat() {
		return "";
	}

	@Override
	public Object[] getParameters() {
		return new Object[0];
	}

	@Override
	public Throwable getThrowable() {
		return null;
	}
}
