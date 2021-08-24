/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.extendedauthentication.services.infrastructure.persistence;

import javax.persistence.EntityManager;

import ch.post.it.evoting.votingserver.extendedauthentication.domain.model.extendedauthentication.ExtendedAuthentication;

import de.akquinet.jbosscc.needle.db.testdata.AbstractTestdataBuilder;

public class ExtendedAuthenticationTestdataBuilder extends AbstractTestdataBuilder<ExtendedAuthentication> {

	private String authId;

	private String extraParam;

	private String salt;

	private Integer attempts;

	private String encryptedStartVotingKey;

	private String tenantId;

	private String electionEvent;

	private String credentialId;

	public ExtendedAuthenticationTestdataBuilder() {
	}

	public ExtendedAuthenticationTestdataBuilder(EntityManager entityManager) {
		super(entityManager);
	}

	public static ExtendedAuthenticationTestdataBuilder anExtendedAuthentication() {
		return new ExtendedAuthenticationTestdataBuilder();
	}

	public ExtendedAuthenticationTestdataBuilder withVotingCardId2(String votingCardId2) {
		this.authId = votingCardId2;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder withExtraParam(String extraParam) {
		this.extraParam = extraParam;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder withSalt(String salt) {
		this.salt = salt;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder withAttempts(Integer attempts) {
		this.attempts = attempts;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder withEncryptedStartVotingKey(String encryptedStartVotingKey) {
		this.encryptedStartVotingKey = encryptedStartVotingKey;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder withTenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder withElectionEvent(String electionEvent) {
		this.electionEvent = electionEvent;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder withCredentialId(String credentialId) {
		this.credentialId = credentialId;
		return this;
	}

	public ExtendedAuthenticationTestdataBuilder but() {
		return anExtendedAuthentication().withVotingCardId2(authId).withExtraParam(extraParam).withSalt(salt).withAttempts(attempts)
				.withEncryptedStartVotingKey(encryptedStartVotingKey).withTenantId(tenantId).withElectionEvent(electionEvent)
				.withCredentialId(credentialId);
	}

	@Override
	public ExtendedAuthentication build() {
		ExtendedAuthentication extendedAuthentication = new ExtendedAuthentication();
		extendedAuthentication.setAuthId(authId);
		extendedAuthentication.setExtraParam(extraParam);
		extendedAuthentication.setSalt(salt);
		extendedAuthentication.setAttempts(attempts);
		extendedAuthentication.setEncryptedStartVotingKey(encryptedStartVotingKey);
		extendedAuthentication.setTenantId(tenantId);
		extendedAuthentication.setElectionEvent(electionEvent);
		extendedAuthentication.setCredentialId(credentialId);
		return extendedAuthentication;
	}

}
