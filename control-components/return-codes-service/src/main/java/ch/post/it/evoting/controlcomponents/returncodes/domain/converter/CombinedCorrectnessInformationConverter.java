/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain.converter;

import java.nio.charset.StandardCharsets;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponents.returncodes.domain.converter.exception.CombinedCorrectnessInformationConverterException;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;

@Converter
public class CombinedCorrectnessInformationConverter implements AttributeConverter<CombinedCorrectnessInformation, byte[]> {

	private final ObjectMapper objectMapper;

	public CombinedCorrectnessInformationConverter(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public byte[] convertToDatabaseColumn(final CombinedCorrectnessInformation combinedCorrectnessInformation) {
		byte[] mapped = new byte[] {};

		if (combinedCorrectnessInformation != null) {
			try {
				mapped = objectMapper.writeValueAsString(combinedCorrectnessInformation).getBytes(StandardCharsets.UTF_8);
			} catch (JsonProcessingException e) {
				throw new CombinedCorrectnessInformationConverterException(
						"Could not map the provided combined correctness information into the dedicated database byte[] type.", e);
			}
		}
		return mapped;
	}

	@Override
	public CombinedCorrectnessInformation convertToEntityAttribute(final byte[] blob) {
		CombinedCorrectnessInformation mapped = null;

		if (blob != null && blob.length > 0) {
			try {
				mapped = objectMapper.readValue(new String(blob, StandardCharsets.UTF_8), CombinedCorrectnessInformation.class);
			} catch (JsonProcessingException e) {
				throw new CombinedCorrectnessInformationConverterException(
						"Could not map the read byte[] from database into a combined correctness information.", e);
			}
		}

		return mapped;
	}

}
