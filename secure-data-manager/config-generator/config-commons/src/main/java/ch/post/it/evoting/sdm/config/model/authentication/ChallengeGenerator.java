/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

/**
 * Interface for defining creation strategies of the authentication key
 */
public interface ChallengeGenerator {

	/**
	 * @return
	 */
	ExtraParams generateExtraParams();
}
