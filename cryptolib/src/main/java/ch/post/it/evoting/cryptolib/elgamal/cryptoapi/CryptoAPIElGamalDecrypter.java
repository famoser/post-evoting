/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.cryptoapi;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Defines the methods supported by a CryptoAPIElGamalDecrypter.
 */
public interface CryptoAPIElGamalDecrypter {

	/**
	 * Decrypt a ciphertext.
	 *
	 * <p>The encrypted message parameter will be a list of group elements, encapsulated within an
	 * {@link ElGamalCiphertext} object.
	 *
	 * <p>The length of the received ciphertext (number of group elements contained within it) must be
	 * equal to, or less than, the length of the private key of this decrypter. If this condition is not met, then an exception will be thrown.
	 *
	 * @param ciphertext              the encrypted message to be decrypted.
	 * @param validateGroupMembership if true, validate that each element in {@code ciphertext} is a member of the mathematical group of the
	 *                                decrypter's ElGamal private key.
	 * @return the decrypted message.
	 * @throws GeneralCryptoLibException if the ciphetext is invalid.
	 */
	List<ZpGroupElement> decrypt(ElGamalCiphertext ciphertext, boolean validateGroupMembership) throws GeneralCryptoLibException;
}
