/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.controlcomponents.commons.payloadsignature.CryptolibPayloadSignatureService;
import ch.post.it.evoting.controlcomponents.mixing.KeyManagerMockConfig;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetService;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofService;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.mixnet.BallotBoxDetails;
import ch.post.it.evoting.domain.mixnet.MixnetInitialPayload;
import ch.post.it.evoting.domain.mixnet.MixnetState;

@SpringBootTest(classes = { MixDecryptOnlineService.class, MixDecryptMessageConsumer.class,
		CcmjKeyRepository.class, AsymmetricService.class, CryptolibPayloadSignatureService.class, CertificatesService.class, MixnetService.class,
		ZeroKnowledgeProofService.class })
@ActiveProfiles("test")
@ContextConfiguration(classes = { TestConfig.class, KeyManagerMockConfig.class })
class MixDecryptMessageConsumerTest {

	private static final String BALLOT_BOX_ID = "f0dd956605bb47d589f1bd7b195d6f38";
	private static final String ELECTION_EVENT_ID = "0b88257ec32142bb8ee0ed1bb70f362e";
	private static final BallotBoxDetails ballotBoxDetails = new BallotBoxDetails(BALLOT_BOX_ID, ELECTION_EVENT_ID);

	private static final GqGroup gqGroup = new GqGroup(new BigInteger(
			"16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759"),
			new BigInteger(
					"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379"),
			BigInteger.valueOf(2));

	private static final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);

	private MixnetInitialPayload payload;

	@Autowired
	private KeysManager keysManager;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private MixDecryptMessageConsumer mixDecryptMessageConsumer;
	@Autowired
	private CryptolibPayloadSignatureService cryptolibPayloadSignatureService;

	@MockBean
	private RabbitTemplate rabbitTemplate;

	@BeforeEach
	void setup() throws PayloadSignatureException, KeyManagementException {
		final List<ElGamalMultiRecipientCiphertext> encryptedVotes = new ArrayList<>(elGamalGenerator.genRandomCiphertextVector(2, 1));
		final ElGamalMultiRecipientPublicKey electionPublicKey = elGamalGenerator.genRandomPublicKey(2);

		payload = new MixnetInitialPayload(gqGroup, encryptedVotes, electionPublicKey);
		final byte[] payloadHash = mixDecryptMessageConsumer.hashPayload(payload);
		X509Certificate[] certificateChain = new X509Certificate[1];
		certificateChain[0] = keysManager.getPlatformCACertificate();
		final CryptolibPayloadSignature signature = cryptolibPayloadSignatureService
				.sign(payloadHash, keysManager.getElectionSigningPrivateKey(ELECTION_EVENT_ID), certificateChain);
		payload.setSignature(signature);
	}

	@Test
	void onMessageWithMixnetStateForWrongNodeGivesError() throws IOException, KeyManagementException, PayloadSignatureException {
		final MixnetState mixnetState = new MixnetState(ballotBoxDetails, 4, payload, 5, null);
		final Message message = createMessage(mixnetState);

		mixDecryptMessageConsumer.onMessage(message);

		MixnetState outputMixnetState = getOutputMixnetState();
		assertErrorMessage(mixnetState, outputMixnetState,
				"The following fields present validation errors: [Node to visit is expected to be 1, but was 4]");
	}

	@Test
	void onMessageWithNullBallotBoxDetails() throws IOException, KeyManagementException, PayloadSignatureException {
		final MixnetState mixnetState = new MixnetState(null, 1, payload, 5, null);
		final Message message = createMessage(mixnetState);

		mixDecryptMessageConsumer.onMessage(message);

		MixnetState outputMixnetState = getOutputMixnetState();
		assertErrorMessage(mixnetState, outputMixnetState, "The following fields present validation errors: [No ballot box details provided]");
	}

	@Test
	void onMessageWithNullPayload() throws IOException, KeyManagementException, PayloadSignatureException {
		final MixnetState mixnetState = new MixnetState(ballotBoxDetails, 1, null, 5, null);
		final Message message = createMessage(mixnetState);

		mixDecryptMessageConsumer.onMessage(message);

		MixnetState outputMixnetState = getOutputMixnetState();
		assertErrorMessage(mixnetState, outputMixnetState, "The following fields present validation errors: [No payload provided]");
	}

	@Test
	void onMessageWithValidInput() throws IOException, KeyManagementException, PayloadSignatureException {
		final MixnetState mixnetState = new MixnetState(ballotBoxDetails, 1, payload, 5, null);
		final Message message = createMessage(mixnetState);

		mixDecryptMessageConsumer.onMessage(message);

		MixnetState outputMixnetState = getOutputMixnetState();
		assertNull(outputMixnetState.getMixnetError());
		assertNotEquals(mixnetState, outputMixnetState);
		assertEquals(mixnetState.getBallotBoxDetails(), outputMixnetState.getBallotBoxDetails());
		assertNotNull(outputMixnetState.getPayload());
		assertEquals(mixnetState.getNodeToVisit(), outputMixnetState.getNodeToVisit());
		assertEquals(mixnetState.getRetryCount(), outputMixnetState.getRetryCount());
	}

	@Test
	void onMessageWithIncompatibleArguments() throws IOException, KeyManagementException, PayloadSignatureException {
		final GqGroup otherGqGroup = new GqGroup(new BigInteger(
				"22588801568735561413035633152679913053449200833478689904902877673687016391844561133376032309307885537704777240609087377993341380751697605235541131273868440070920362148431866829787784445019147999379498503693247429579480289226602748397335327890884464685051682703709742724121783217827040722415360103179289160056581759372475845985438977307323570530753362027145384124771826114651710264766437273044759690955051982839684910462609395741692689616014805965573558015387956017183286848440036954926101719205598449898400180082053755864070690174202432196678045052744337832802051787273056312757384654145455745603262082348042780103679"),
				new BigInteger(
						"11294400784367780706517816576339956526724600416739344952451438836843508195922280566688016154653942768852388620304543688996670690375848802617770565636934220035460181074215933414893892222509573999689749251846623714789740144613301374198667663945442232342525841351854871362060891608913520361207680051589644580028290879686237922992719488653661785265376681013572692062385913057325855132383218636522379845477525991419842455231304697870846344808007402982786779007693978008591643424220018477463050859602799224949200090041026877932035345087101216098339022526372168916401025893636528156378692327072727872801631041174021390051839"),
				BigInteger.valueOf(2));

		final ElGamalGenerator otherGenerator = new ElGamalGenerator(otherGqGroup);
		final List<ElGamalMultiRecipientCiphertext> otherEncryptedVotes = new ArrayList<>(otherGenerator.genRandomCiphertextVector(2, 1));
		final ElGamalMultiRecipientPublicKey otherElectionPublicKey = otherGenerator.genRandomPublicKey(2);
		final MixnetInitialPayload otherPayload = new MixnetInitialPayload(otherGqGroup, otherEncryptedVotes, otherElectionPublicKey);
		final byte[] payloadHash = mixDecryptMessageConsumer.hashPayload(otherPayload);
		X509Certificate[] certificateChain = new X509Certificate[1];
		certificateChain[0] = keysManager.getPlatformCACertificate();
		final CryptolibPayloadSignature signature = cryptolibPayloadSignatureService
				.sign(payloadHash, keysManager.getElectionSigningPrivateKey(ELECTION_EVENT_ID), certificateChain);
		otherPayload.setSignature(signature);

		final MixnetState mixnetState = new MixnetState(ballotBoxDetails, 1, otherPayload, 5, null);
		final Message message = createMessage(mixnetState);

		mixDecryptMessageConsumer.onMessage(message);

		MixnetState outputMixnetState = getOutputMixnetState();
		assertErrorMessage(mixnetState, outputMixnetState,
				"Incompatible input arguments: The remaining election public key and the ccm election key must have the same group.");
	}

	@Test
	void onMessageWithPayloadContainingNullObjects() throws IOException, KeyManagementException, PayloadSignatureException, NoSuchAlgorithmException {
		final List<ElGamalMultiRecipientCiphertext> otherEncryptedVotes = new ArrayList<>(elGamalGenerator.genRandomCiphertextVector(2, 1));
		final MixnetInitialPayload otherPayload = new MixnetInitialPayload(gqGroup, otherEncryptedVotes, null, null);
		final byte[] payloadHash = new HashService(MessageDigest.getInstance("SHA-256"))
				.recursiveHash(otherPayload.getEncryptionGroup(), HashableList.from(otherPayload.getEncryptedVotes()));
		X509Certificate[] certificateChain = new X509Certificate[1];
		certificateChain[0] = keysManager.getPlatformCACertificate();
		final CryptolibPayloadSignature signature = cryptolibPayloadSignatureService
				.sign(payloadHash, keysManager.getElectionSigningPrivateKey(ELECTION_EVENT_ID), certificateChain);
		otherPayload.setSignature(signature);

		final MixnetState mixnetState = new MixnetState(ballotBoxDetails, 1, otherPayload, 5, null);
		final Message message = createMessage(mixnetState);

		mixDecryptMessageConsumer.onMessage(message);

		MixnetState outputMixnetState = getOutputMixnetState();
		assertErrorMessage(mixnetState, outputMixnetState, "The following fields present validation errors: [The payload contains null objects.]");
	}

	// Utility functions

	/**
	 * Asserts that both MixnetStates are the same, with the output MixnetState having an error message in addition.
	 *
	 * @param mixnetState       the original MixnetState sent to the control component.
	 * @param outputMixnetState the MixnetState received from the control component after processing.
	 * @param errorMessage      the error message that the outputMixnetState is expected to contain.
	 */
	private void assertErrorMessage(final MixnetState mixnetState, final MixnetState outputMixnetState, final String errorMessage) {
		assertNotNull(outputMixnetState.getMixnetError());
		assertNotEquals(mixnetState, outputMixnetState);
		assertEquals(mixnetState.getBallotBoxDetails(), outputMixnetState.getBallotBoxDetails());
		assertEquals(mixnetState.getPayload(), outputMixnetState.getPayload());
		assertEquals(mixnetState.getNodeToVisit(), outputMixnetState.getNodeToVisit());
		assertEquals(mixnetState.getRetryCount(), outputMixnetState.getRetryCount());
		assertEquals(errorMessage, outputMixnetState.getMixnetError());
	}

	/**
	 * Serializes a MixnetState and puts it into a Message.
	 *
	 * @param mixnetState the MixnetState object to be passed to the message.
	 * @return a Message object that can be passed into a queue.
	 * @throws JsonProcessingException if the MixnetState object cannot be serialized
	 */
	private Message createMessage(final MixnetState mixnetState) throws JsonProcessingException {
		final String mixnetStateJson = objectMapper.writeValueAsString(mixnetState);
		byte[] serializedMixnetState = mixnetStateJson.getBytes(StandardCharsets.UTF_8);
		byte[] byteContent = new byte[serializedMixnetState.length + 1];
		byteContent[0] = 0;
		System.arraycopy(serializedMixnetState, 0, byteContent, 1, serializedMixnetState.length);

		return new Message(byteContent, new MessageProperties());
	}

	/**
	 * Returns the MixnetState object that was sent to the control components output queue.
	 *
	 * @return a MixnetState object
	 * @throws IOException if the sending of the MixnetState failed or it could not be deserialized.
	 */
	private MixnetState getOutputMixnetState() throws IOException {
		ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(rabbitTemplate).convertAndSend(any(), argumentCaptor.capture());
		final byte[] update = argumentCaptor.getValue();
		byte[] mixnetStateBytes = new byte[update.length - 1];
		System.arraycopy(update, 1, mixnetStateBytes, 0, update.length - 1);

		return objectMapper.readValue(mixnetStateBytes, MixnetState.class);
	}
}
