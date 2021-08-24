/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.sign;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.CiphertextImpl;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.Contest;
import ch.post.it.evoting.domain.election.ElectionOption;

/**
 * Voting data generator for tests.
 */
public class TestVotingDataService {

	private static final Logger logger = LoggerFactory.getLogger(TestVotingDataService.class);

	private static final SecureRandom generator = new SecureRandom();

	private static final int NANOSECONDS_PER_SECOND = 1_000_000_000;

	private final CryptoAPIElGamalEncrypter encrypter;

	private final ElGamalKeyPair electoralAuthorityKeyPair;

	private final Ballot ballot;

	private final List<List<ZpGroupElement>> unencryptedVotes = new ArrayList<>();

	/**
	 * Constructs a voting data provider with a generated electoral authority key pair.
	 *
	 * @param ballot the definition of the ballot
	 * @throws GeneralCryptoLibException
	 */
	public TestVotingDataService(Ballot ballot) throws GeneralCryptoLibException {
		// Create voting data.
		this(ballot, CryptoTestData.generateElGamalKeyPair(1));
	}

	/**
	 * Constructs voting test data with a pre-defined electoral authority key pair.
	 *
	 * @param ballot                    the definition of the ballot
	 * @param electoralAuthorityKeyPair the pre-defined electoral authority key pair
	 * @throws GeneralCryptoLibException
	 */
	public TestVotingDataService(Ballot ballot, ElGamalKeyPair electoralAuthorityKeyPair) throws GeneralCryptoLibException {
		this.ballot = ballot;
		this.electoralAuthorityKeyPair = electoralAuthorityKeyPair;
		// Create vote encrypter.
		encrypter = new ElGamalService().createEncrypter(electoralAuthorityKeyPair.getPublicKeys());
	}

	/**
	 * @return the generated vote encryption key.
	 */
	public ElGamalPublicKey getVoteEncryptionKey() {
		return electoralAuthorityKeyPair.getPublicKeys();
	}

	/**
	 * Generate votes.
	 *
	 * @param numVotes the number of votes to generate
	 * @return a list of votes
	 * @throws GeneralCryptoLibException
	 */
	public List<Ciphertext> generateVotes(int numVotes) throws GeneralCryptoLibException {
		logger.info("Generating {} votes...", numVotes);

		// Store the Zp subgroup locally.
		ZpSubgroup zpSubgroup = CryptoTestData.getZpSubgroup();

		// Extract the representations from each of the options in each of the
		// contests.
		logger.info("Getting representations...");
		List<String> representations = ballot.getContests().stream().map(Contest::getOptions).flatMap(Collection::stream)
				.map(ElectionOption::getRepresentation).collect(Collectors.toList());
		logger.info("{} representations collected", representations.size());

		logger.info("Voting...");
		long startTime = System.nanoTime();

		// Create a list of votes which is composed of a random choice of
		// representations.
		List<Ciphertext> votes = new ArrayList<>(numVotes);
		for (int i = 0; i < numVotes; i++) {
			// Loop over all representations for this vote.
			List<ZpGroupElement> vote = new ArrayList<>();
			for (String representation : representations) {
				// Whether this representation is chosen.
				if (generator.nextBoolean()) {
					vote.add(new ZpGroupElement(new BigInteger(representation), zpSubgroup));
				}
			}
			// Blank votes are not allowed. Choose the first representation if
			// that's the case.
			if (vote.isEmpty()) {
				vote = Collections.singletonList(new ZpGroupElement(new BigInteger(representations.get(0)), zpSubgroup));
			}

			unencryptedVotes.add(vote);

			// Record the vote.
			Ciphertext encryptedVote = encryptVote(vote);
			votes.add(encryptedVote);

			if (i % 100 == 0 && i > 1) {
				long elapsedTime = System.nanoTime() - startTime;
				// Extrapolate ETA from current stats.
				double secondsPerItem = (double) elapsedTime / i / NANOSECONDS_PER_SECOND;
				long remainingItems = numVotes - i;
				long eta = Math.round(secondsPerItem * remainingItems);
				long itemsPerSecond = Math.round(1 / secondsPerItem);
				logger.info("{} votes encrypted, {} to go, ~{} votes/s, (ETA ~{} seconds)", i, remainingItems, itemsPerSecond, eta);
			}
		}

		logger.info("{} votes ready", votes.size());

		return votes;
	}

	private Ciphertext encryptVote(List<ZpGroupElement> vote) throws GeneralCryptoLibException {
		GroupElementsCompressor<ZpGroupElement> compressor = new GroupElementsCompressor<>();

		// Cast vote.
		ZpGroupElement compressedVote = compressor.compress(vote);

		List<ZpGroupElement> compressedMessage = new ArrayList<>();
		compressedMessage.add(compressedVote);

		ElGamalCiphertext elGamalCiphertext = encrypter.encryptGroupElements(compressedMessage).getElGamalCiphertext();

		// Convert the ElGamal computation values to an encrypted vote.
		List<ZpGroupElement> values = elGamalCiphertext.getPhis();
		List<ZpGroupElement> elements = new ArrayList<>(elGamalCiphertext.getValues().size());
		for (ZpGroupElement zpGroupElement : values) {
			ZpGroupElement element = new ZpGroupElement(zpGroupElement.getValue(), CryptoTestData.getZpSubgroup());
			elements.add(element);
		}

		return new CiphertextImpl(elGamalCiphertext.getGamma(), elements);
	}

	public List<List<ZpGroupElement>> getUnencryptedVotes() {
		return unencryptedVotes;
	}
}
