/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.elgamal.codec;

import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Implementation of {@link ElGamalCiphertextCodec}.
 */
public final class ElGamalCiphertextCodecImpl implements ElGamalCiphertextCodec {
	private static final ElGamalCiphertextCodecImpl INSTANCE = new ElGamalCiphertextCodecImpl();

	private ElGamalCiphertextCodecImpl() {
	}

	/**
	 * Returns the instance.
	 *
	 * @return the instance.
	 */
	public static ElGamalCiphertextCodecImpl getInstance() {
		return INSTANCE;
	}

	@Override
	public List<ElGamalCiphertext> decodeList(String encoding) throws GeneralCryptoLibException {
		return new Decoder().decodeList(encoding);
	}

	@Override
	public Map<ElGamalCiphertext, ElGamalCiphertext> decodeMap(String encoding) throws GeneralCryptoLibException {
		return new Decoder().decodeMap(encoding);
	}

	@Override
	public ElGamalCiphertext decodeSingle(String encoding) throws GeneralCryptoLibException {
		return new Decoder().decodeSingle(encoding);
	}

	@Override
	public String encodeList(List<ElGamalCiphertext> list) {
		return new Encoder().encodeList(list);
	}

	@Override
	public String encodeMap(Map<ElGamalCiphertext, ElGamalCiphertext> map) {
		return new Encoder().encodeMap(map);
	}

	@Override
	public String encodeSingle(ElGamalCiphertext values) {
		return new Encoder().encodeSingle(values);
	}

	private static class Decoder {
		private BigInteger[] bigIntegers;

		public List<ElGamalCiphertext> decodeList(String encoding) throws GeneralCryptoLibException {
			byte[] bytes = Base64.getDecoder().decode(encoding);
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			decodeBigIntegers(buffer);
			int size = decodeSize(buffer);
			List<ElGamalCiphertext> list = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				list.add(decodeElgamalComputationsValues(buffer));
			}
			return list;
		}

		public Map<ElGamalCiphertext, ElGamalCiphertext> decodeMap(String encoding) throws GeneralCryptoLibException {
			byte[] bytes = Base64.getDecoder().decode(encoding);
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			decodeBigIntegers(buffer);
			int size = decodeSize(buffer);
			Map<ElGamalCiphertext, ElGamalCiphertext> map = new HashMap<>(size);
			for (int i = 0; i < size; i++) {
				ElGamalCiphertext key = decodeElgamalComputationsValues(buffer);
				ElGamalCiphertext value = decodeElgamalComputationsValues(buffer);
				map.put(key, value);
			}
			return map;
		}

		public ElGamalCiphertext decodeSingle(String encoding) throws GeneralCryptoLibException {
			byte[] bytes = Base64.getDecoder().decode(encoding);
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			decodeBigIntegers(buffer);
			return decodeElgamalComputationsValues(buffer);
		}

		private void checkRemaining(ByteBuffer buffer, int required) throws GeneralCryptoLibException {
			if (buffer.remaining() < required) {
				throw new GeneralCryptoLibException("Not enough data to decode.");
			}
		}

		private BigInteger decodeBigInteger(ByteBuffer buffer) throws GeneralCryptoLibException {
			int size = decodeSize(buffer);
			checkRemaining(buffer, size);
			byte[] bytes = new byte[size];
			buffer.get(bytes);
			return new BigInteger(bytes);
		}

		private int decodeBigIntegerIndex(ByteBuffer buffer) throws GeneralCryptoLibException {
			return decodeInt(buffer);
		}

		private void decodeBigIntegers(ByteBuffer buffer) throws GeneralCryptoLibException {
			int size = decodeSize(buffer);
			bigIntegers = new BigInteger[size];
			for (int i = 0; i < size; i++) {
				bigIntegers[i] = decodeBigInteger(buffer);
			}
		}

		private ElGamalCiphertext decodeElgamalComputationsValues(ByteBuffer buffer) throws GeneralCryptoLibException {
			int size = decodeSize(buffer);
			List<ZpGroupElement> elements = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				elements.add(decodeZpGroupElement(buffer));
			}
			return new ElGamalCiphertext(elements);
		}

		private int decodeInt(ByteBuffer buffer) throws GeneralCryptoLibException {
			checkRemaining(buffer, Integer.BYTES);
			return buffer.getInt();
		}

		private int decodeSize(ByteBuffer buffer) throws GeneralCryptoLibException {
			return decodeInt(buffer);
		}

		private ZpGroupElement decodeZpGroupElement(ByteBuffer buffer) throws GeneralCryptoLibException {
			int p = decodeBigIntegerIndex(buffer);
			int q = decodeBigIntegerIndex(buffer);
			int value = decodeBigIntegerIndex(buffer);
			return new ZpGroupElement(bigIntegers[value], bigIntegers[p], bigIntegers[q]);
		}

	}

	private static class Encoder {
		private final Map<BigInteger, Integer> bigIntegers = new LinkedHashMap<>();

		private final List<ByteBuffer> bigIntegerBuffers = new LinkedList<>();

		private final List<ByteBuffer> valuesBuffers = new LinkedList<>();

		public String encodeList(List<ElGamalCiphertext> list) {
			encodeSize(list.size());
			list.forEach(this::encodeElGamalComputationsValues);
			encodeBigIntegers();
			return Base64.getEncoder().encodeToString(getBytes());
		}

		public String encodeMap(Map<ElGamalCiphertext, ElGamalCiphertext> map) {
			encodeSize(map.size());
			map.forEach((key, value) -> {
				encodeElGamalComputationsValues(key);
				encodeElGamalComputationsValues(value);
			});
			encodeBigIntegers();
			return Base64.getEncoder().encodeToString(getBytes());
		}

		public String encodeSingle(ElGamalCiphertext values) {
			encodeElGamalComputationsValues(values);
			encodeBigIntegers();
			return Base64.getEncoder().encodeToString(getBytes());
		}

		private void encodeBigInteger(BigInteger value) {
			byte[] bytes = value.toByteArray();
			bigIntegerBuffers.add(encodeInt(bytes.length));
			bigIntegerBuffers.add(ByteBuffer.wrap(bytes));
		}

		private void encodeBigIntegerIndex(BigInteger value) {
			Integer index = bigIntegers.computeIfAbsent(value, v -> bigIntegers.size());
			valuesBuffers.add(encodeInt(index));
		}

		private void encodeBigIntegers() {
			bigIntegerBuffers.add(encodeInt(bigIntegers.size()));
			bigIntegers.keySet().forEach(this::encodeBigInteger);
		}

		private void encodeElGamalComputationsValues(ElGamalCiphertext values) {
			ZpGroupElement gamma = values.getGamma();
			List<ZpGroupElement> phis = values.getPhis();
			encodeSize(phis.size() + 1);
			encodeZpGroupElement(gamma);
			phis.forEach(this::encodeZpGroupElement);
		}

		private ByteBuffer encodeInt(int value) {
			return (ByteBuffer) ByteBuffer.allocate(Integer.BYTES).putInt(value).flip();
		}

		private void encodeSize(int size) {
			valuesBuffers.add(encodeInt(size));
		}

		private void encodeZpGroupElement(ZpGroupElement element) {
			encodeBigIntegerIndex(element.getP());
			encodeBigIntegerIndex(element.getQ());
			encodeBigIntegerIndex(element.getValue());
		}

		private Stream<ByteBuffer> getBufferStream() {
			return Stream.concat(bigIntegerBuffers.stream(), valuesBuffers.stream());
		}

		private byte[] getBytes() {
			int length = getBufferStream().mapToInt(Buffer::remaining).sum();
			ByteBuffer bytes = ByteBuffer.allocate(length);
			getBufferStream().forEach(bytes::put);
			return bytes.array();
		}
	}
}
