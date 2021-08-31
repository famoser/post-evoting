/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import java.util.Properties;

import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * This is a mapper of the set of settings in json format to properties. The properties to be mapped are hardcoded in the class as constants.
 */
@Component
public class SettingsJson2EEPropertiesMapper {

	static final String CONFIG_PROPERTY_NAME_MAX_NUMBER_OF_ATTEMPTS = "maxNumberOfAttempts";
	static final String CONFIG_PROPERTY_NAME_NUM_VOTES_PER_AUTH_TOKEN = "numVotesPerAuthToken";
	static final String CONFIG_PROPERTY_NAME_NUM_VOTES_PER_VOTING_CARD = "numVotesPerVotingCard";
	static final String CONFIG_PROPERTY_NAME_AUTH_TOKEN_EXP_TIME = "authTokenExpTime";
	static final String CONFIG_PROPERTY_NAME_CHALLENGE_RES_EXP_TIME = "challengeResExpTime";
	static final String CONFIG_PROPERTY_NAME_VALIDITY_PERIOD = "validityPeriod";
	static final String CONFIG_PROPERTY_NAME_END = "end";
	static final String CONFIG_PROPERTY_NAME_START = "start";
	private static final String JSON_PARAM_NAME_MAXIMUM_NUMBER_OF_ATTEMPTS = "maximumNumberOfAttempts";
	private static final String JSON_PARAM_NAME_NUMBER_VOTES_PER_AUTH_TOKEN = "numberVotesPerAuthToken";
	private static final String JSON_PARAM_NAME_NUMBER_VOTES_PER_VOTING_CARD = "numberVotesPerVotingCard";
	private static final String JSON_PARAM_NAME_AUTH_TOKEN_EXPIRATION_TIME = "authTokenExpirationTime";
	private static final String JSON_PARAM_NAME_CHALLENGE_LENGTH = "challengeLength";
	static final String CONFIG_PROPERTY_NAME_CHALLENGE_LENGTH = JSON_PARAM_NAME_CHALLENGE_LENGTH;
	private static final String JSON_PARAM_NAME_CHALLENGE_RESPONSE_EXPIRATION_TIME = "challengeResponseExpirationTime";
	private static final String JSON_PARAM_NAME_CERTIFICATES_VALIDITY_PERIOD = "certificatesValidityPeriod";
	private static final String JSON_PARAM_NAME_DATE_TO = "dateTo";
	private static final String JSON_PARAM_NAME_DATE_FROM = "dateFrom";

	/**
	 * This method creates a set of properties by reading field values from the given election event in json format.
	 *
	 * @param electionEventAsJson The election event in json format from which to read the properties.
	 * @return a Properties object containing a set of properties read from the json.
	 */
	public Properties createConfigPropertiesFromJson(String electionEventAsJson) {
		Properties properties = new Properties();
		if (StringUtils.isBlank(electionEventAsJson)) {
			return properties;
		}
		JsonObject electionEvent = JsonUtils.getJsonObject(electionEventAsJson);

		String dateFromJson = electionEvent.getString(JSON_PARAM_NAME_DATE_FROM);
		properties.setProperty(CONFIG_PROPERTY_NAME_START, dateFromJson);
		String dateToJson = electionEvent.getString(JSON_PARAM_NAME_DATE_TO);
		properties.setProperty(CONFIG_PROPERTY_NAME_END, dateToJson);

		JsonObject settings = electionEvent.getJsonObject(JsonConstants.SETTINGS);
		int validityPeriodJson = settings.getInt(JSON_PARAM_NAME_CERTIFICATES_VALIDITY_PERIOD);
		properties.setProperty(CONFIG_PROPERTY_NAME_VALIDITY_PERIOD, String.valueOf(validityPeriodJson));
		int challengeResponseExpirationTimeJson = settings.getInt(JSON_PARAM_NAME_CHALLENGE_RESPONSE_EXPIRATION_TIME);
		properties.setProperty(CONFIG_PROPERTY_NAME_CHALLENGE_RES_EXP_TIME, String.valueOf(challengeResponseExpirationTimeJson));
		int authTokenExpirationTimeJson = settings.getInt(JSON_PARAM_NAME_AUTH_TOKEN_EXPIRATION_TIME);
		properties.setProperty(CONFIG_PROPERTY_NAME_AUTH_TOKEN_EXP_TIME, String.valueOf(authTokenExpirationTimeJson));
		int challengeLengthJson = settings.getInt(JSON_PARAM_NAME_CHALLENGE_LENGTH);
		properties.setProperty(CONFIG_PROPERTY_NAME_CHALLENGE_LENGTH, String.valueOf(challengeLengthJson));
		int numberVotesPerVotingCardJson = settings.getInt(JSON_PARAM_NAME_NUMBER_VOTES_PER_VOTING_CARD);
		properties.setProperty(CONFIG_PROPERTY_NAME_NUM_VOTES_PER_VOTING_CARD, String.valueOf(numberVotesPerVotingCardJson));
		int numberVotesPerAuthTokenJson = settings.getInt(JSON_PARAM_NAME_NUMBER_VOTES_PER_AUTH_TOKEN);
		properties.setProperty(CONFIG_PROPERTY_NAME_NUM_VOTES_PER_AUTH_TOKEN, String.valueOf(numberVotesPerAuthTokenJson));
		int maximumNumberOfAttemptsJson = settings.getInt(JSON_PARAM_NAME_MAXIMUM_NUMBER_OF_ATTEMPTS);
		properties.setProperty(CONFIG_PROPERTY_NAME_MAX_NUMBER_OF_ATTEMPTS, String.valueOf(maximumNumberOfAttemptsJson));
		return properties;
	}
}
