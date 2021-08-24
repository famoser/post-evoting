/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

public class ReturnCodeGenerationResponsePayloadDeserializer extends JsonDeserializer<ReturnCodeGenerationResponsePayload> {

	@Override
	public ReturnCodeGenerationResponsePayload deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) parser.getCodec();

		final JsonNode node = mapper.readTree(parser);
		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup gqGroup = mapper.readValue(encryptionGroupNode.toString(), GqGroup.class);

		final String tenantId = mapper.readValue(node.get("tenantId").toString(), String.class);
		final String electionEventId = mapper.readValue(node.get("electionEventId").toString(), String.class);
		final String verificationCardSetId = mapper.readValue(node.get("verificationCardSetId").toString(), String.class);
		final int chunkId = mapper.readValue(node.get("chunkId").toString(), Integer.class);

		final List<ReturnCodeGenerationOutput> returnCodeGenerationInputs = Arrays.asList(mapper.reader().withAttribute("group", gqGroup)
				.readValue(node.get("returnCodeGenerationOutputs").toString(), ReturnCodeGenerationOutput[].class));

		final int nodeId = mapper.readValue(node.get("nodeId").toString(), Integer.class);

		final CryptolibPayloadSignature signature = mapper.reader().readValue(node.get("signature").toString(), CryptolibPayloadSignature.class);

		return new ReturnCodeGenerationResponsePayload(tenantId, electionEventId, verificationCardSetId, chunkId, gqGroup, returnCodeGenerationInputs,
				nodeId, signature);
	}

}
