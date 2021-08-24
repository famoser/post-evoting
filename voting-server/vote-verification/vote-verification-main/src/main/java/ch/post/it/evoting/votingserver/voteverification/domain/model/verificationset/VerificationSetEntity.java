/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.post.it.evoting.domain.election.errors.SemanticErrorGroup;
import ch.post.it.evoting.domain.election.errors.SyntaxErrorGroup;
import ch.post.it.evoting.votingserver.commons.domain.model.Constants;

/**
 * Entity which contains the information of verification set data
 */
@Entity
@Table(name = "VERIFICATION_SET_DATA")
public class VerificationSetEntity {

	/**
	 * The identifier of this verification set.
	 */
	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "verificationSetSeq")
	@SequenceGenerator(name = "verificationSetSeq", sequenceName = "VERIFICATION_SET_DATA_SEQ", allocationSize = 1)
	@JsonIgnore
	private Integer id;

	/**
	 * The identifier of the tenant, to which this verification belongs.
	 */
	@Column(name = "TENANT_ID")
	@NotNull(groups = SyntaxErrorGroup.class)
	@Size(max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	private String tenantId;

	/**
	 * The external id provided.
	 */
	@Column(name = "VERIFICATION_CARD_SET_ID")
	@NotNull(groups = SyntaxErrorGroup.class)
	@Size(max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	@JsonProperty(value = "id")
	private String verificationCardSetId;

	/**
	 * The identifier of the election event, to which this verification belongs.
	 */
	@Column(name = "ELECTION_EVENT_ID")
	@NotNull(groups = SyntaxErrorGroup.class)
	@Size(max = Constants.COLUMN_LENGTH_100, groups = SemanticErrorGroup.class)
	private String electionEventId;

	/**
	 * The choices codes public key value information and certificates
	 */
	@Column(name = "JSON")
	@NotNull(groups = SyntaxErrorGroup.class)
	@Lob
	private String json;

	/**
	 * The verification card set signature.
	 */
	@Column(name = "SIGNATURE", length = Integer.MAX_VALUE)
	@NotNull(groups = SyntaxErrorGroup.class)
	@Lob
	private String signature;

	@JsonIgnore
	public Integer getId() {
		return id;
	}

	@JsonIgnore
	public void setId(Integer id) {
		this.id = id;
	}

	@JsonIgnore
	public String getTenantId() {
		return tenantId;
	}

	@JsonProperty
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@JsonIgnore
	public String getElectionEventId() {
		return electionEventId;
	}

	@JsonProperty
	public void setElectionEventId(String electionEventId) {
		this.electionEventId = electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public void setVerificationCardSetId(String verificationCardSetId) {
		this.verificationCardSetId = verificationCardSetId;
	}

	@JsonProperty("data")
	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

}
