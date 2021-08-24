/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import java.util.Properties;

/**
 * A bean inside the {@link ElectionInformationContents} bean, representing some properties to be passed as a output on the Create Election Event
 * command.
 */
public class ElectionInformationParams {

	private String numVotesPerVotingCard;

	private String numVotesPerAuthToken;

	/**
	 *
	 */
	public ElectionInformationParams() {
	}

	/**
	 * @param numVotesPerVotingCard
	 * @param numVotesPerAuthToken
	 */
	public ElectionInformationParams(final String numVotesPerVotingCard, final String numVotesPerAuthToken) {
		super();
		this.numVotesPerVotingCard = numVotesPerVotingCard;
		this.numVotesPerAuthToken = numVotesPerAuthToken;
	}

	/**
	 * @return Returns the numVotesPerVotingCard.
	 */
	public String getNumVotesPerVotingCard() {
		return numVotesPerVotingCard;
	}

	/**
	 * @param numVotesPerVotingCard The numVotesPerVotingCard to set.
	 */
	public void setNumVotesPerVotingCard(final String numVotesPerVotingCard) {
		this.numVotesPerVotingCard = numVotesPerVotingCard;
	}

	/**
	 * @return Returns the numVotesPerAuthToken.
	 */
	public String getNumVotesPerAuthToken() {
		return numVotesPerAuthToken;
	}

	/**
	 * @param numVotesPerAuthToken The numVotesPerAuthToken to set.
	 */
	public void setNumVotesPerAuthToken(final String numVotesPerAuthToken) {
		this.numVotesPerAuthToken = numVotesPerAuthToken;
	}

	public void setFromProperties(final Properties properties) {

		numVotesPerVotingCard = (String) properties.get("numVotesPerVotingCard");

		numVotesPerAuthToken = (String) properties.get("numVotesPerAuthToken");
	}
}
