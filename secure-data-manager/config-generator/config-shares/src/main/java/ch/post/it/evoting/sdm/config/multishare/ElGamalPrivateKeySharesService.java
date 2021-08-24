/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.multishare;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingServiceAPI;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.secretsharing.service.ThresholdSecretSharingService;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;

/**
 * Allows for an ElGamal privatekey to be split into a number of shares, and the privatekey to be recovered again later.
 * <p>
 * Internally, it uses the splitting algorithm defined by the policy of {@link ThresholdSecretSharingService}.
 */
public class ElGamalPrivateKeySharesService {

	private ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI;

	public ElGamalPrivateKeySharesService() {
		initSecretSharingService();
	}

	/**
	 * Split an ElGamal privatekey into a number of shares.
	 * <p>
	 * It is important to realize that an ElGamal key is composed of a number of "subkeys" (these subkeys can also be thought of as keys in their own
	 * right).
	 * <p>
	 * This method performs a split on EACH of the subkeys separately, it then groups together the corresponding shares from each of these split
	 * operations. For example, if the received ElGamal privatekey contains 5 subkeys, and if the specified number of shares is 2, then each of the
	 * subkeys will be split into two shares, and then all of the shares with index '0' will be grouped into one container, and all of the shares with
	 * index '1' will be grouped into a container.
	 *
	 * @param privateKey   the key to be split into shares.
	 * @param numberShares the number of shares to be generated. The list returned by this method will contain this number of elements.
	 * @param threshold    the minimum number of shares required to be present to recover the key.
	 * @return a List of containers (containing the same number of shares as subkeys in the key).
	 */
	public List<MultipleSharesContainer> split(final ElGamalPrivateKey privateKey, final int numberShares, final int threshold)
			throws SharesException {

		validateSplitInputs(privateKey, numberShares, threshold);

		List<Exponent> subkeys = privateKey.getKeys();

		// split each subkey into shares
		List<List<Share>> allShares = new ArrayList<>();

		for (Exponent subkey : subkeys) {
			byte[] secret = subkey.getValue().toByteArray();
			Set<Share> shareForSubkey;
			try {
				shareForSubkey = thresholdSecretSharingServiceAPI.split(secret, numberShares, threshold, privateKey.getGroup().getQ());
			} catch (GeneralCryptoLibException e) {
				throw new SharesException("An error occured while splitting the shares.", e);
			}
			allShares.add(new ArrayList<>(shareForSubkey));
		}

		// now arrange data into the containers that will be serialized
		int numSubkeys = subkeys.size();
		List<MultipleSharesContainer> containers = new ArrayList<>();

		for (int i = 0; i < numberShares; i++) {

			List<byte[]> sharesForCard = new ArrayList<>();
			for (int j = 0; j < numSubkeys; j++) {
				sharesForCard.add(thresholdSecretSharingServiceAPI.serialize(allShares.get(j).get(i)));
			}
			containers.add(new MultipleSharesContainer(numberShares, threshold, sharesForCard, privateKey.getGroup().getQ()));
		}

		return containers;
	}

	/**
	 * Recover an ElGamal privatekey.
	 *
	 * @param containers       the list of containers (containing one or more shares).
	 * @param elGamalPublicKey the publickey that corresponds to the private key.
	 * @return the recovered ElGamal privatekey.
	 */
	public ElGamalPrivateKey recover(List<MultipleSharesContainer> containers, ElGamalPublicKey elGamalPublicKey) throws SharesException {

		validateRecoverInputs(containers, elGamalPublicKey);

		int numSubkeys = containers.get(0).getShares().size();

		List<Exponent> recoveredListOfSubkeys = new ArrayList<>();

		for (int i = 0; i < numSubkeys; i++) {

			Set<Share> sharesForRecoveringParticularSubkey = new HashSet<>();
			for (MultipleSharesContainer container : containers) {

				List<byte[]> allSharesFromContainer = container.getShares();
				try {
					sharesForRecoveringParticularSubkey.add(thresholdSecretSharingServiceAPI.deserialize(allSharesFromContainer.get(i)));
				} catch (GeneralCryptoLibException e) {
					throw new SharesException("Exception while deserializing the shares.", e);
				}
			}

			Exponent recoveredSubkey;
			try {
				byte[] recoveredData = thresholdSecretSharingServiceAPI.recover(sharesForRecoveringParticularSubkey);
				recoveredSubkey = new Exponent(elGamalPublicKey.getGroup().getQ(), new BigInteger(1, recoveredData));
			} catch (GeneralCryptoLibException e) {
				throw new SharesException("Exception while trying to reconstruct Privatekey Exponent", e);
			}
			recoveredListOfSubkeys.add(recoveredSubkey);
		}

		try {
			return new ElGamalPrivateKey(recoveredListOfSubkeys, elGamalPublicKey.getGroup());
		} catch (GeneralCryptoLibException e) {
			throw new SharesException("Exception while trying to create new ElGamalPrivateKey", e);
		}
	}

	private void validateSplitInputs(ElGamalPrivateKey privateKey, int numberShares, int threshold) throws SharesException {

		if (privateKey == null) {
			throw new SharesException("The received ElGamal private key was null");
		}

		if (numberShares < 1) {
			throw new SharesException("The number of shares should be at least 1, but it was: " + numberShares);
		}

		if (threshold < 1) {
			throw new SharesException("The threshold should be at least 1, but it was: " + threshold);
		}
	}

	private void validateRecoverInputs(List<MultipleSharesContainer> containers, ElGamalPublicKey elGamalPublicKey) throws SharesException {

		if (containers == null) {
			throw new SharesException("The received list of containers was null");
		} else if (containers.isEmpty()) {
			throw new SharesException("The received list of containers was empty");
		}

		if (elGamalPublicKey == null) {
			throw new SharesException("The received ElGamal public key was null");
		}
	}

	private void initSecretSharingService() {
		this.thresholdSecretSharingServiceAPI = new ThresholdSecretSharingService();
	}

}
