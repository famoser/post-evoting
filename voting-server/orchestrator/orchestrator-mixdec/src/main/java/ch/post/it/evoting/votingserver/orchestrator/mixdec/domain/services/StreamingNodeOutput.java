/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.services;

import java.io.InputStream;

import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.MixDecNodeOutputId;

/**
 * A node output representation whose payload can be consumed as an input stream.
 */
public class StreamingNodeOutput {
	private final String fileName;

	private final InputStream payloadInputStream;

	public StreamingNodeOutput(MixDecNodeOutputId id, InputStream payloadInputStream) {
		this.payloadInputStream = payloadInputStream;

		fileName = buildFileName(id);
	}

	/**
	 * Builds the file name a payload should be given according to its identifiers.
	 *
	 * @param id the node output identifier
	 * @return the node output's file name
	 */
	public static String buildFileName(MixDecNodeOutputId id) {
		return String.format("%s-%s-%s.dat", id.getElectionEventId(), id.getBallotBoxId(), id.getNodeId());
	}

	/**
	 * @return an input stream to stream the payload out the database.
	 */
	public InputStream getPayloadInputStream() {
		return payloadInputStream;
	}

	/**
	 * @return the file name this payload should be given
	 */
	public String getFileName() {
		return fileName;
	}
}
