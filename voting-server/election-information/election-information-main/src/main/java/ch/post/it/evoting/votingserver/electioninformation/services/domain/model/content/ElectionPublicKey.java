/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.post.it.evoting.votingserver.commons.domain.model.Constants;

/**
 * Class representing the entity ElectionPublicKey
 */
@Entity
@Table(name = "ELECTION_PUBLIC_KEY", uniqueConstraints = @UniqueConstraint(name = "ELECTION_PUBLIC_KEY_UK1", columnNames = { "TENANT_ID",
		"ELECTION_EVENT_ID", "ELECTORAL_AUTHORITY_ID" }))

public class ElectionPublicKey {

	/**
	 * The identifier for this election public key.
	 */
	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "electionPublicKeySeq")
	@SequenceGenerator(name = "electionPublicKeySeq", sequenceName = "ELECTION_PUBLIC_KEY_SEQ", allocationSize = 1)
	@JsonIgnore
	private Integer id;

	/**
	 * The identifier of a tenant related to this election public key.
	 */
	@Column(name = "TENANT_ID")
	@NotNull
	@Size(max = Constants.COLUMN_LENGTH_100)
	@JsonIgnore
	private String tenantId;

	/**
	 * The identifier of an election event related to this election public key.
	 */
	@Column(name = "ELECTION_EVENT_ID")
	@NotNull
	@Size(max = Constants.COLUMN_LENGTH_100)
	@JsonIgnore
	private String electionEventId;

	/**
	 * The identifier of the electoral authority.
	 */
	@Column(name = "ELECTORAL_AUTHORITY_ID")
	@NotNull
	@Size(max = Constants.COLUMN_LENGTH_100)
	@JsonIgnore
	private String electoralAuthorityId;

	/**
	 * The election public key in json format.
	 */
	@Lob
	@Column(name = "JSON", length = Integer.MAX_VALUE)
	@NotNull
	private String json;

	/**
	 * Returns the current value of the field json.
	 *
	 * @return Returns the json ballot.
	 */
	public String getJson() {
		return json;
	}

	/**
	 * Sets the value of the field json.
	 *
	 * @param json The json ballot to set.
	 */
	public void setJson(String json) {
		this.json = json;
	}

	/**
	 * Returns the current value of the field id.
	 *
	 * @return Returns the id.
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Sets the value of the field id.
	 *
	 * @param id The id to set.
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Returns the current value of the field tenantId.
	 *
	 * @return Returns the tenantId.
	 */
	public String getTenantId() {
		return tenantId;
	}

	/**
	 * Sets the value of the field tenantId.
	 *
	 * @param tenantId The tenantId to set.
	 */
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * Returns the current value of the field electionEventId.
	 *
	 * @return Returns the electionEventId.
	 */
	public String getElectionEventId() {
		return electionEventId;
	}

	/**
	 * Sets the value of the field electionEventId.
	 *
	 * @param electionEventId The electionEventId to set.
	 */
	public void setElectionEventId(String electionEventId) {
		this.electionEventId = electionEventId;
	}

	/**
	 * Returns the current value of the field electoralAuthorityId.
	 *
	 * @return Returns the electoralAuthorityId.
	 */
	public String getElectoralAuthorityId() {
		return electoralAuthorityId;
	}

	/**
	 * Sets the value of the field electoralAuthorityId.
	 *
	 * @param electoralAuthorityId The electoralAuthorityId to set.
	 */
	public void setElectoralAuthorityId(String electoralAuthorityId) {
		this.electoralAuthorityId = electoralAuthorityId;
	}
}
