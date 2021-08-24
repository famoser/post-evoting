/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Tests of {@link ObjectMappers}.
 */
class ObjectMappersTest {
	private static final String CSV = "A,1";
	private Foo expected;

	@BeforeEach
	void setUp() {
		expected = new Foo();
		expected.setName("name");
		expected.setValue(1);
		expected.setIgnored(true);
	}

	@Test
	void testFromJsonInputStreamClassOfT() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			ObjectMappers.toJson(bytes, expected);
		} finally {
			bytes.close();
		}
		Foo actual;
		try (InputStream stream = new ByteArrayInputStream(bytes.toByteArray())) {
			actual = ObjectMappers.fromJson(stream, Foo.class);
		}
		assertEquals(expected.getName(), actual.getName());
		assertEquals(expected.getValue(), actual.getValue());
		assertFalse(actual.isIgnored());
	}

	@Test
	void testFromJsonReaderClassOfT() throws IOException {
		StringWriter writer = new StringWriter();
		try {
			ObjectMappers.toJson(writer, expected);
		} finally {
			writer.close();
		}
		Foo actual;
		try (Reader reader = new StringReader(writer.toString())) {
			actual = ObjectMappers.fromJson(reader, Foo.class);
		}
		assertEquals(expected.getName(), actual.getName());
		assertEquals(expected.getValue(), actual.getValue());
		assertFalse(actual.isIgnored());
	}

	@Test
	void testFromJsonStringClassOfT() throws IOException {
		String string = ObjectMappers.toJson(expected);
		Foo actual = ObjectMappers.fromJson(string, Foo.class);
		assertEquals(expected.getName(), actual.getName());
		assertEquals(expected.getValue(), actual.getValue());
		assertFalse(actual.isIgnored());
	}

	@Test
	void testIntegerToJsonAndBackToInteger() throws IOException {

		Integer originalValue = 10;

		String stringRepresentation = ObjectMappers.toJson(originalValue);

		TypeReference<Integer> typeReference = new TypeReference<Integer>() {
		};
		Integer recovered = ObjectMappers.fromJson(stringRepresentation, typeReference);

		String errorMsg = "The recovered list is not equal to the original list";
		assertEquals(originalValue, recovered, errorMsg);
	}

	@Test
	void testListToJsonAndBackToList() throws IOException, GeneralCryptoLibException {

		List<ZpGroupElement> originalList = getListOfGroupElements();

		String stringRepresentation = ObjectMappers.toJson(originalList);

		TypeReference<List<ZpGroupElement>> typeReference = new TypeReference<List<ZpGroupElement>>() {
		};
		List<ZpGroupElement> recovered = ObjectMappers.fromJson(stringRepresentation, typeReference);

		int expectedSize = 2;
		String errorMsg = "The recovered list does not have the expected size";
		assertEquals(expectedSize, recovered.size(), errorMsg);

		errorMsg = "The recovered list is not equal to the original list";
		assertEquals(originalList, recovered, errorMsg);
	}

	@Test
	void givenBadJsonStringWhenMapBackToListThenException() {

		String badJsonString = "XXXXXX";

		TypeReference<List<ZpGroupElement>> typeReference = new TypeReference<List<ZpGroupElement>>() {
		};
		assertThrows(JsonParseException.class, () -> ObjectMappers.fromJson(badJsonString, typeReference));
	}

	@Test
	void whenMismatchBetweenDataAndTypeRefThenException() throws IOException, GeneralCryptoLibException {

		List<ZpGroupElement> originalList = getListOfGroupElements();
		String stringRepresentation = ObjectMappers.toJson(originalList);

		TypeReference<List<Integer>> differentType = new TypeReference<List<Integer>>() {
		};
		assertThrows(MismatchedInputException.class, () -> ObjectMappers.fromJson(stringRepresentation, differentType));
	}

	@Test
	void testReadCsvInputStreamClassOfTStrings() throws IOException {
		try (InputStream stream = new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8));
				MappingIterator<Foo> iterator = ObjectMappers.readCsv(stream, Foo.class, "name", "value")) {
			assertTrue(iterator.hasNext());
			Foo foo = iterator.next();
			assertEquals("A", foo.getName());
			assertEquals(1, foo.getValue());
			assertFalse(foo.isIgnored());
			assertFalse(iterator.hasNext());
		}
	}

	@Test
	void testReadCsvPathClassOfTStrings() throws IOException {
		Path file = createTempFile("test", ".csv");
		try {
			write(file, CSV.getBytes(StandardCharsets.UTF_8));
			try (MappingIterator<Foo> iterator = ObjectMappers.readCsv(file, Foo.class, "name", "value")) {
				assertTrue(iterator.hasNext());
				Foo foo = iterator.next();
				assertEquals("A", foo.getName());
				assertEquals(1, foo.getValue());
				assertFalse(foo.isIgnored());
				assertFalse(iterator.hasNext());
			}
		} finally {
			delete(file);
		}
	}

	@Test
	void testReadCsvWithSemicolonSeparator() throws IOException {
		File testCsvFile = new File(
				URLDecoder.decode(this.getClass().getClassLoader().getResource("csvDataTest.csv").getPath(), StandardCharsets.UTF_8.toString()));
		Path path = Paths.get(testCsvFile.toURI());
		try (Reader reader = newBufferedReader(path, StandardCharsets.UTF_8);
				MappingIterator<FooSecond> iterator = ObjectMappers.readCsv(reader, FooSecond.class, ';', "name", "value")) {
			assertTrue(iterator.hasNext());
			FooSecond fooSecond = iterator.next();
			assertEquals("21b0ed864457423da108cba4483cc469", fooSecond.getName());
			assertEquals("[\"eyJ6cEdyb3VwRWxl\",\"eyJ6cEdyb3VwRWxlbWV\"]", fooSecond.getValue());
			assertTrue(iterator.hasNext());
		}
	}

	@Test
	void testReadCsvReaderClassOfTStrings() throws IOException {
		try (Reader stream = new StringReader(CSV); MappingIterator<Foo> iterator = ObjectMappers.readCsv(stream, Foo.class, "name", "value")) {
			assertTrue(iterator.hasNext());
			Foo foo = iterator.next();
			assertEquals("A", foo.getName());
			assertEquals(1, foo.getValue());
			assertFalse(foo.isIgnored());
			assertFalse(iterator.hasNext());
		}
	}

	@Test
	public void testReadCsvStringClassOfTStrings() throws IOException {
		try (MappingIterator<Foo> iterator = ObjectMappers.readCsv(CSV, Foo.class, "name", "value")) {
			assertTrue(iterator.hasNext());
			Foo foo = iterator.next();
			assertEquals("A", foo.getName());
			assertEquals(1, foo.getValue());
			assertFalse(foo.isIgnored());
			assertFalse(iterator.hasNext());
		}
	}

	private List<ZpGroupElement> getListOfGroupElements() throws GeneralCryptoLibException {

		BigInteger p = new BigInteger("23");
		BigInteger q = new BigInteger("11");
		BigInteger g = new BigInteger("2");
		ZpSubgroup group = new ZpSubgroup(g, p, q);
		List<ZpGroupElement> list = new ArrayList<>();
		list.add(new ZpGroupElement(new BigInteger("4"), group));
		list.add(new ZpGroupElement(new BigInteger("5"), group));
		return list;
	}

	public static final class Foo {
		private String name;

		private int value;

		private boolean ignored;

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public int getValue() {
			return value;
		}

		public void setValue(final int value) {
			this.value = value;
		}

		@JsonIgnore
		public boolean isIgnored() {
			return ignored;
		}

		public void setIgnored(final boolean ignored) {
			this.ignored = ignored;
		}
	}

	private static final class FooSecond {
		private String name;

		private String value;

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(final String value) {
			this.value = value;
		}
	}
}
