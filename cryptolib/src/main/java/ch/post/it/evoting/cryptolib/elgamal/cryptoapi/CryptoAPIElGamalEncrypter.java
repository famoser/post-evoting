/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.cryptoapi;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Defines the methods supported by a CryptoAPIElGamalEncrypter.
 *
 * <p>The methods specified in this interface allow data to be encrypted using an implementation of
 * the ElGamal cryptosystem.
 *
 * <p>As well as permitting the encryption of data, this interface also specifies methods for
 * performing pre-computations. Pre-computations is an optional process that may be performed (at any time) before encrypting data, that allows part
 * of the numerical computations to be performed "ahead of time". In fact, pre-computations may be performed before the data that will be encrypted is
 * known.
 *
 * <p>The output from the pre-computations process may then be passed as an input to an encryption
 * process, along with the actual data to encrypt.
 *
 * <p>Due to the fact that pre-computation is optional, methods which don't require pre-computation
 * values, and methods which do require pre-computation values are provided.
 */
public interface CryptoAPIElGamalEncrypter {

	/**
	 * Encrypt the received list of messages (which are represented as {@link ZpGroupElement}).
	 *
	 * <p>The length of the received list of messages must be equal to, or less than, the length of
	 * the public key of this encrypter. If this condition is not met, then an exception will be thrown.
	 *
	 * @param messages the messages to encrypt, represented as a list of {@link ZpGroupElement} objects.
	 * @return the encrypted messages and the random exponent that was generated during the encryption process, encapsulated within an {@link
	 * ElGamalEncrypterValues} object.
	 * @throws GeneralCryptoLibException if the messages to encrypt are invalid.
	 */
	ElGamalEncrypterValues encryptGroupElements(List<ZpGroupElement> messages) throws GeneralCryptoLibException;

	/**
	 * Encrypt the received list of messages (which are represented as {@link ZpGroupElement}) with a random short Exponent. This method can only be
	 * used for Quadratic Residue groups.
	 *
	 * <p>The length of the received list of messages must be equal to, or less than, the length of
	 * the public key of this encrypter. If this condition is not met, then an exception will be thrown.
	 *
	 * @param messages the messages to encrypt, represented as a list of {@link ZpGroupElement} objects.
	 * @return the encrypted messages and the random exponent that was generated during the encryption process, encapsulated within an {@link
	 * ElGamalEncrypterValues} object.
	 * @throws GeneralCryptoLibException if the messages to encrypt are invalid.
	 * @throws CryptoLibException        if Zp Group defined in policy is not Quadratic Residue Group.
	 */
	ElGamalEncrypterValues encryptGroupElementsWithShortExponent(List<ZpGroupElement> messages) throws GeneralCryptoLibException;

	/**
	 * Encrypt the received list of messages (which are represented as Strings).
	 *
	 * <p>The length of the received list of messages must be equal to, or less than, the length of
	 * the public key of this encrypter. If this condition is not met, then an exception will be thrown.
	 *
	 * <p>If any of the elements in the received list of messages cannot be parsed as an integer, then
	 * an exception will be thrown.
	 *
	 * @param messages the messages to encrypt, represented as a list of Strings, where each String specifies an integer value that should belong to
	 *                 the same mathematical group as the ElGamal public key associated with the encrypter.
	 * @return the encrypted messages and the random exponent that was generated during the encryption process, encapsulated within an {@link
	 * ElGamalEncrypterValues} object.
	 * @throws GeneralCryptoLibException if the messages to encrypt are invalid.
	 */
	ElGamalEncrypterValues encryptStrings(List<String> messages) throws GeneralCryptoLibException;

	/**
	 * Encrypt the received list of messages (which are represented as {@link ZpGroupElement}), and use the received pre-computed values.
	 *
	 * <p>The length of the received list of messages must be equal to, or less than, the length of
	 * the public key of this encrypter. If this condition is not met, then an exception will be thrown.
	 *
	 * <p>The {@code preComputationValues} object expected by this method is the result of the method
	 * {@link #preCompute} having being previously executed. Therefore, this method should only be called if the {@link #preCompute} method has
	 * already been executed.
	 *
	 * @param messages             the messages to encrypt, represented as a list of {@link ZpGroupElement} objects.
	 * @param preComputationValues the result of performing pre-computation. This object encapsulates a random exponent, a gamma value and a set of
	 *                             phi values (sometimes known as 'prePhi values' and they are the result of pre-computation).
	 * @return the encrypted messages and the random exponent that was generated during the encryption process, encapsulated within an {@link
	 * ElGamalEncrypterValues} object.
	 * @throws GeneralCryptoLibException if the messages to encrypt or the pre-computed values are invalid.
	 */
	ElGamalEncrypterValues encryptGroupElements(List<ZpGroupElement> messages, ElGamalEncrypterValues preComputationValues)
			throws GeneralCryptoLibException;

	/**
	 * Encrypt the received list of messages (which are represented as Strings), and use the received pre-computed values.
	 *
	 * <p>The length of the received list of messages must be equal to, or less than, the length of
	 * the public key of this encrypter. If this condition is not met, then an exception will be thrown.
	 *
	 * <p>The {@code preComputationValues} object expected by this method is the result of the method
	 * {@link #preCompute} having being previously executed. Therefore, this method should only be called if the {@link #preCompute} method has
	 * already been executed.
	 *
	 * <p>If any of the elements in the received list of messages cannot be parsed as an integer, then
	 * an exception will be thrown.
	 *
	 * @param messages             the messages to encrypt, represented as a list of Strings, where each String specifies an integer value that should
	 *                             belong to the same mathematical group as the ElGamal key associated with the encrypter.
	 * @param preComputationValues the result of performing pre-computation. This object encapsulates a random exponent, a gamma value and a set of
	 *                             phi values (which could be called 'prePhi values' are they are the result of pre-computation).
	 * @return the encrypted messages and the random exponent that was generated during the encryption process, encapsulated within an {@link
	 * ElGamalEncrypterValues} object.
	 * @throws GeneralCryptoLibException if the messages to encrypt or the pre-computed values are invalid.
	 */
	ElGamalEncrypterValues encryptStrings(List<String> messages, ElGamalEncrypterValues preComputationValues) throws GeneralCryptoLibException;

	/**
	 * Returns output of performing pre-computation calculations.
	 *
	 * <p>The output of this method, which is an {@link ElGamalEncrypterValues}, can later be passed
	 * along with some plaintext messages to one of the 'encrypt' methods of this interface, in order to obtain the ciphertext of the messages.
	 *
	 * @return the result of pre-computation, encapsulated in an {@link ElGamalEncrypterValues} object.
	 */
	ElGamalEncrypterValues preCompute();

}
