/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.domain.election.VerificationCardPublicKeyAndSignature;
import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateVerificationCardDataException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;
import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

public class VerificationCardDataWriter extends MultiFileDataWriter<GeneratedVotingCardOutput> {

	private static final ConfigObjectMapper mapper = new ConfigObjectMapper();

	public VerificationCardDataWriter(final Path path, final int maxNumCredentialsPerFile) {

		super(path, maxNumCredentialsPerFile);
	}

	@Override
	protected String getLine(GeneratedVotingCardOutput item) {
		final String verificationCardId = item.getVerificationCardId();
		final String verificationCardSetId = item.getVerificationCardSetId();
		final String electionEventId = item.getElectionEventId();
		final VerificationCardCredentialDataPack verificationCardCredentialDataPack = item.getVerificationCardCredentialDataPack();

		final String verificationCardSerializedKeyStoreB64 = verificationCardCredentialDataPack.getSerializedKeyStore();

		confirmAndLogKeystore(verificationCardSerializedKeyStoreB64, verificationCardId, verificationCardSetId, electionEventId, loggingWriter);

		final ElGamalPublicKey verificationCardPublicKey = verificationCardCredentialDataPack.getVerificationCardKeyPair().getPublicKeys();
		String verificationCardPublicKeyB64;
		try {
			verificationCardPublicKeyB64 = Base64.getEncoder().encodeToString(verificationCardPublicKey.toJson().getBytes(StandardCharsets.UTF_8));
		} catch (GeneralCryptoLibException e) {
			throw new CreateVotingCardSetException(
					"Exception while trying to obtain a representation of the verification card publickey: " + e.getMessage(), e);
		}

		final byte[] signatureBytes = verificationCardCredentialDataPack.getSignatureVCardPubKeyEEIDVCID();
		final String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);
		VerificationCardPublicKeyAndSignature verCardBean = new VerificationCardPublicKeyAndSignature();
		verCardBean.setPublicKey(verificationCardPublicKeyB64);
		verCardBean.setSignature(signatureBase64);

		try {
			String serializedVerCardBeanB64 = Base64.getEncoder().encodeToString(mapper.fromJavaToJSON(verCardBean).getBytes(StandardCharsets.UTF_8));

			return String
					.format("%s,%s,%s,%s,%s", verificationCardId, verificationCardSerializedKeyStoreB64, serializedVerCardBeanB64, electionEventId,
							verificationCardSetId);

		} catch (JsonProcessingException e) {
			throw new CreateVotingCardSetException("Exception while trying to encode verification card to Json", e);
		}
	}

	private void confirmAndLogKeystore(final String verificationCardSerializedKeyStoreB64, final String verificationCardId,
			final String verificationCardSetId, final String electionEventId, final LoggingWriter loggingWriter) {

		if (verificationCardSerializedKeyStoreB64.length() > 0) {
			loggingWriter.log(Level.INFO,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCD_SUCCESS_GENERATING_VERIFICATION_CARD_KEYSTORE)
							.electionEvent(electionEventId).user("adminID").additionalInfo("verifcs_id", verificationCardSetId)
							.additionalInfo("verifc_id", verificationCardId).createLogInfo());

		} else {
			final String errorMsg = "Error - the keyStore that is being written to file is invalid";
			loggingWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCD_ERROR_GENERATING_VERIFICATION_KEYSTORE)
							.electionEvent(electionEventId).user("adminID").additionalInfo("verifcs_id", verificationCardSetId)
							.additionalInfo("verifc_id", verificationCardId).additionalInfo("err_desc", errorMsg).createLogInfo());

			throw new GenerateVerificationCardDataException(errorMsg);
		}
	}
}
