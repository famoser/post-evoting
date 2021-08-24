/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * The results of mixing and decrypting a ballot box in one node.
 */
@Entity
@Table(name = "MIXDEC_NODE_OUTPUT")
public class MixDecNodeOutput {

	@EmbeddedId
	private MixDecNodeOutputId id;

	@Column(name = "PAYLOAD")
	@Lob
	private byte[] payload;

	public String getNodeId() {
		return id.getNodeId();
	}

	public String getElectionEventId() {
		return id.getElectionEventId();
	}

	public String getBallotBoxId() {
		return id.getBallotBoxId();
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public void setId(MixDecNodeOutputId id) {
		this.id = id;
	}

}
