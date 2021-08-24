/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCodesDataPack;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;
import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

public class CodesMappingTableWriter extends MultiFileDataWriter<GeneratedVotingCardOutput> {

	private static final ConfigObjectMapper mapper = new ConfigObjectMapper();

	public CodesMappingTableWriter(final Path basePath, final int maxNumCredentialsPerFile) {
		super(basePath, maxNumCredentialsPerFile);
	}

	@Override
	protected String getLine(GeneratedVotingCardOutput item) {
		final VerificationCardCodesDataPack verificationCardCodesDataPack = item.getVerificationCardCodesDataPack();
		String mappingAsJSONB64;
		try {
			// The map is reordered to ensure the original order is complete lost
			mappingAsJSONB64 = Base64.getEncoder().encodeToString(
					mapper.fromJavaToJSON(new TreeMap<>(verificationCardCodesDataPack.getCodesMappingTable())).getBytes(StandardCharsets.UTF_8));
		} catch (JsonProcessingException e) {
			throw new CreateVotingCardSetException("Exception while trying to encode codes mapping table to Json", e);
		}
		final String verificationCardId = item.getVerificationCardId();
		return String.format("%s,%s", verificationCardId, mappingAsJSONB64);
	}
}
