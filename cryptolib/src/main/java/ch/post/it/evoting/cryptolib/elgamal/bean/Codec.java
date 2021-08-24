/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Utility class for binary serialization of {@link ElGamalPrivateKey} and {@link ElGamalPublicKey}.
 *
 * <p>This class is thread-safe.
 */
class Codec {
	private Codec() {
	}

	/**
	 * Decodes {@link ElGamalPrivateKey} from given bytes.
	 *
	 * @param bytes the bytes
	 * @return the key
	 * @throws GeneralCryptoLibException failed to decode the key.
	 */
	public static ElGamalPrivateKey decodePrivateKey(byte[] bytes) throws GeneralCryptoLibException {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		ZpSubgroup group = decodeGroup(buffer);
		int size = decodeInt(buffer);
		List<Exponent> exponents = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			BigInteger value = decodeBigInteger(buffer);
			exponents.add(new Exponent(group.getQ(), value));
		}
		return new ElGamalPrivateKey(exponents, group);
	}

	/**
	 * Decodes {@link ElGamalPublicKey} from given bytes.
	 *
	 * @param bytes the bytes
	 * @return the key
	 * @throws GeneralCryptoLibException failed to decode the key.
	 */
	public static ElGamalPublicKey decodePublicKey(byte[] bytes) throws GeneralCryptoLibException {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		ZpSubgroup group = decodeGroup(buffer);
		int size = decodeInt(buffer);
		List<ZpGroupElement> elements = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			BigInteger value = decodeBigInteger(buffer);
			elements.add(new ZpGroupElement(value, group));
		}
		return new ElGamalPublicKey(elements, group);
	}

	/**
	 * Encodes a given {@link ElGamalPrivateKey}.
	 *
	 * @param key the key
	 * @return the bytes.
	 */
	public static byte[] encode(ElGamalPrivateKey key) {
		List<ByteBuffer> buffers = new LinkedList<>();
		encode(buffers, key);
		return concatenate(buffers).array();
	}

	/**
	 * Encodes a given {@link ElGamalPublicKey}.
	 *
	 * @param key the key
	 * @return the bytes.
	 */
	public static byte[] encode(ElGamalPublicKey key) {
		List<ByteBuffer> buffers = new LinkedList<>();
		encode(buffers, key);
		return concatenate(buffers).array();
	}

	private static void checkRemaining(ByteBuffer buffer, int length) throws GeneralCryptoLibException {
		if (buffer.remaining() < length) {
			throw new GeneralCryptoLibException("Not enough data.");
		}
	}

	private static ByteBuffer concatenate(List<ByteBuffer> buffers) {
		int length = 0;
		for (ByteBuffer buffer : buffers) {
			buffer.rewind();
			length += buffer.remaining();
		}
		ByteBuffer bytes = ByteBuffer.allocate(length);
		buffers.forEach(bytes::put);
		return bytes;
	}

	private static BigInteger decodeBigInteger(ByteBuffer buffer) throws GeneralCryptoLibException {
		int length = decodeInt(buffer);
		checkRemaining(buffer, length);
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return new BigInteger(bytes);
	}

	private static ZpSubgroup decodeGroup(ByteBuffer buffer) throws GeneralCryptoLibException {
		BigInteger p = decodeBigInteger(buffer);
		BigInteger q = decodeBigInteger(buffer);
		BigInteger g = decodeBigInteger(buffer);
		return new ZpSubgroup(g, p, q);
	}

	private static int decodeInt(ByteBuffer buffer) throws GeneralCryptoLibException {
		checkRemaining(buffer, Integer.BYTES);
		return buffer.getInt();
	}

	private static void encode(List<ByteBuffer> buffers, BigInteger value) {
		ByteBuffer buffer = ByteBuffer.wrap(value.toByteArray());
		encode(buffers, buffer.limit());
		buffers.add(buffer);
	}

	private static void encode(List<ByteBuffer> buffers, ElGamalPrivateKey key) {
		encode(buffers, key.getGroup());
		encode(buffers, key.getKeys().size());
		for (Exponent exponent : key.getKeys()) {
			encode(buffers, exponent.getValue());
		}
	}

	private static void encode(List<ByteBuffer> buffers, ElGamalPublicKey key) {
		encode(buffers, key.getGroup());
		encode(buffers, key.getKeys().size());
		for (ZpGroupElement element : key.getKeys()) {
			encode(buffers, element.getValue());
		}
	}

	private static void encode(List<ByteBuffer> buffers, int value) {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(value);
		buffers.add(buffer);
	}

	private static void encode(List<ByteBuffer> buffers, ZpSubgroup group) {
		encode(buffers, group.getP());
		encode(buffers, group.getQ());
		encode(buffers, group.getG());
	}
}
