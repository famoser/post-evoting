/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.elgamal.codec;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Tests of {@link ElGamalCiphertextCodecImpl}.
 */
class ElGamalCiphertextCodecImplTest {
	private static final BigInteger P = BigInteger.valueOf(23);

	private static final BigInteger Q = BigInteger.valueOf(11);

	private static final BigInteger VALUE1 = BigInteger.valueOf(2);

	private static final BigInteger VALUE2 = BigInteger.valueOf(4);

	private static final BigInteger VALUE3 = BigInteger.valueOf(8);

	private static final BigInteger VALUE4 = BigInteger.valueOf(16);

	private static final BigInteger VALUE5 = BigInteger.valueOf(9);

	private static final BigInteger VALUE6 = BigInteger.valueOf(18);

	private ZpGroupElement element1;

	private ZpGroupElement element2;

	private ZpGroupElement element3;

	private ZpGroupElement element4;

	private ZpGroupElement element5;

	private ZpGroupElement element6;

	private ElGamalCiphertextCodec codec;

	@BeforeEach
	public void setUp() throws GeneralCryptoLibException {
		element1 = new ZpGroupElement(VALUE1, P, Q);
		element2 = new ZpGroupElement(VALUE2, P, Q);
		element3 = new ZpGroupElement(VALUE3, P, Q);
		element4 = new ZpGroupElement(VALUE4, P, Q);
		element5 = new ZpGroupElement(VALUE5, P, Q);
		element6 = new ZpGroupElement(VALUE6, P, Q);
		codec = ElGamalCiphertextCodecImpl.getInstance();
	}

	@Test
	void testDecodeListTrancated() throws GeneralCryptoLibException {
		ElGamalCiphertext values1 = new ElGamalCiphertext(asList(element1, element2));
		ElGamalCiphertext values2 = new ElGamalCiphertext(asList(element3, element4));
		ElGamalCiphertext values3 = new ElGamalCiphertext(asList(element5, element6));
		List<ElGamalCiphertext> list = asList(values1, values2, values3);

		String encoding = codec.encodeList(list);
		byte[] bytes = Base64.getDecoder().decode(encoding);
		bytes = copyOf(bytes, bytes.length - 1);
		final String encodedString = Base64.getEncoder().encodeToString(bytes);

		assertThrows(GeneralCryptoLibException.class, () -> codec.decodeList(encodedString));
	}

	@Test
	void testDecodeMapTrancated() throws GeneralCryptoLibException {
		ElGamalCiphertext values1 = new ElGamalCiphertext(asList(element1, element2));
		ElGamalCiphertext values2 = new ElGamalCiphertext(asList(element3, element4));
		ElGamalCiphertext values3 = new ElGamalCiphertext(asList(element5, element6));
		Map<ElGamalCiphertext, ElGamalCiphertext> map = new HashMap<>();
		map.put(values1, values2);
		map.put(values2, values3);
		map.put(values3, values1);

		String encoding = codec.encodeMap(map);
		byte[] bytes = Base64.getDecoder().decode(encoding);
		bytes = copyOf(bytes, bytes.length - 1);
		final String encodedString = Base64.getEncoder().encodeToString(bytes);

		assertThrows(GeneralCryptoLibException.class, () -> codec.decodeMap(encodedString));
	}

	@Test
	void testDecodeSingleTrancated() throws GeneralCryptoLibException {
		ElGamalCiphertext values = new ElGamalCiphertext(asList(element1, element2));

		String encoding = codec.encodeSingle(values);
		byte[] bytes = Base64.getDecoder().decode(encoding);
		bytes = copyOf(bytes, bytes.length - 1);
		final String encodedString = Base64.getEncoder().encodeToString(bytes);

		assertThrows(GeneralCryptoLibException.class, () -> codec.decodeSingle(encodedString));
	}

	@Test
	void testEncodeList() throws GeneralCryptoLibException {
		ElGamalCiphertext values1 = new ElGamalCiphertext(asList(element1, element2));
		ElGamalCiphertext values2 = new ElGamalCiphertext(asList(element3, element4));
		ElGamalCiphertext values3 = new ElGamalCiphertext(asList(element5, element6));
		List<ElGamalCiphertext> list = asList(values1, values2, values3);

		String encoding = codec.encodeList(list);

		List<ElGamalCiphertext> list2 = codec.decodeList(encoding);
		assertEquals(list.size(), list2.size());
		for (int i = 0; i < list.size(); i++) {
			assertEquals(list.get(i), list2.get(i));
		}
	}

	@Test
	void testEncodeMap() throws GeneralCryptoLibException {
		ElGamalCiphertext values1 = new ElGamalCiphertext(asList(element1, element2));
		ElGamalCiphertext values2 = new ElGamalCiphertext(asList(element3, element4));
		ElGamalCiphertext values3 = new ElGamalCiphertext(asList(element5, element6));
		Map<ElGamalCiphertext, ElGamalCiphertext> map = new HashMap<>();
		map.put(values1, values2);
		map.put(values2, values3);
		map.put(values3, values1);

		String encoding = codec.encodeMap(map);

		Map<ElGamalCiphertext, ElGamalCiphertext> map2 = codec.decodeMap(encoding);
		assertEquals(map.size(), map2.size());
		assertEquals(values2, map2.get(values1));
		assertEquals(values3, map2.get(values2));
		assertEquals(values1, map2.get(values3));
	}

	@Test
	void testEncodeSingle() throws GeneralCryptoLibException {
		ElGamalCiphertext values = new ElGamalCiphertext(asList(element1, element2));

		String encoding = codec.encodeSingle(values);

		assertEquals(values, codec.decodeSingle(encoding));
	}
}
