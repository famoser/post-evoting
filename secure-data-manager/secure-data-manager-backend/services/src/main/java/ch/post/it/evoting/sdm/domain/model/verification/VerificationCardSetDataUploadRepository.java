/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.verification;

import java.io.IOException;
import java.io.InputStream;

import javax.json.JsonObject;

/**
 * Interface providing upload operations with verificationCardSets card set.
 */
public interface VerificationCardSetDataUploadRepository {

	/**
	 * Uploads codes mapping from a given stream. Client is responsible for closing stream.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @param adminBoardId          the administrative board identifier
	 * @param stream                the stream
	 * @throws IOException failed to upload the mapping.
	 */
	void uploadCodesMapping(String electionEventId, String verificationCardSetId, String adminBoardId, InputStream stream) throws IOException;

	/**
	 * Uploads the verification card data from a given stream. Client is responsible for closing the
	 * stream.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @param adminBoardId          the administrative board identifier
	 * @param stream                the stream
	 * @throws IOException failed to upload the data.
	 */
	void uploadVerificationCardData(String electionEventId, String verificationCardSetId, String adminBoardId, InputStream stream) throws IOException;

	/**
	 * Uploads the verification card set data from a given stream. Client is responsible for closing
	 * the stream.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @param adminBoardId          the administrative board identifier
	 * @param stream                the stream
	 * @throws IOException failed to upload the data.
	 */
	void uploadVerificationCardSetData(String electionEventId, String verificationCardSetId, String adminBoardId, JsonObject voterInformationString)
			throws IOException;

	/**
	 * Uploads the verification card derived keys from a given stream.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @param adminBoardId          the administrative board identifier
	 * @param stream                the stream
	 * @throws IOException failed to upload the data.
	 */
	void uploadVerificationCardDerivedKeys(String electionEventId, String verificationCardSetId, String adminBoardId, InputStream stream)
			throws IOException;

}
