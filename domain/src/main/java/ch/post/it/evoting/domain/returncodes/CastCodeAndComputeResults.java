/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

/**
 * The class representing the vote cast message.
 */
public class CastCodeAndComputeResults extends ComputeResults {

	// the calculated vote cast code
	private String voteCastCode;

	/**
	 * Gets voteCastCode.
	 *
	 * @return Value of voteCastCode.
	 */
	public String getVoteCastCode() {
		return voteCastCode;
	}

	/**
	 * Sets new voteCastCode.
	 *
	 * @param voteCastCode New value of voteCastCode.
	 */
	public void setVoteCastCode(String voteCastCode) {
		this.voteCastCode = voteCastCode;
	}
}
