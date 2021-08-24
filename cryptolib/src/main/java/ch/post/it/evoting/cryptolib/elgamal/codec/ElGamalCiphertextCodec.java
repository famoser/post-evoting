/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.elgamal.codec;

import java.util.List;
import java.util.Map;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;

/**
 * <p>
 * Codec to encode and to decode lists and maps of {@link ElGamalCiphertext}.
 * <p>
 * Implementation must be thread safe.
 */
public interface ElGamalCiphertextCodec {
	/**
	 * Decodes the list from a given encoding.
	 *
	 * @param encoding the encoding
	 * @return the list
	 * @throws GeneralCryptoLibException the encoding is malformed.
	 */
	List<ElGamalCiphertext> decodeList(String encoding) throws GeneralCryptoLibException;

	/**
	 * Decodes the map from a given encoding.
	 *
	 * @param encoding the encoding
	 * @return the map
	 * @throws GeneralCryptoLibException the encoding is malformed.
	 */
	Map<ElGamalCiphertext, ElGamalCiphertext> decodeMap(String encoding) throws GeneralCryptoLibException;

	/**
	 * Decodes the instance from a given encoding.
	 *
	 * @param encoding the encoding
	 * @return the instance
	 * @throws GeneralCryptoLibException the encoding is malformed.
	 */
	ElGamalCiphertext decodeSingle(String encoding) throws GeneralCryptoLibException;

	/**
	 * Encodes a given list.
	 *
	 * @param list the list
	 * @return the encoding.
	 */
	String encodeList(List<ElGamalCiphertext> list);

	/**
	 * Encodes a given map.
	 *
	 * @param map the map
	 * @return the encoding.
	 */
	String encodeMap(Map<ElGamalCiphertext, ElGamalCiphertext> map);

	/**
	 * Encodes a given instance
	 *
	 * @param values the instance
	 * @return the encoding.
	 */
	String encodeSingle(ElGamalCiphertext values);
}
