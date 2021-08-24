/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Path;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class which implements some popular use cases of using {@link ObjectMapper}.
 * <p>
 * This class is thread-safe.
 */
public final class ObjectMappers {
	private static final ObjectMapper JSON_PROTOTYPE = new ObjectMapper().enable(MapperFeature.USE_ANNOTATIONS)
			.disable(MapperFeature.USE_GETTERS_AS_SETTERS).setSerializationInclusion(Include.NON_EMPTY).registerModule(new JavaTimeModule());

	private ObjectMappers() {
	}

	public static JsonObject getJsonObject(String json) {
		JsonReader jsonReader = Json.createReader(new StringReader(json));
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();
		return jsonObject;
	}

	/**
	 * Reads a value from a given JSON stream. Client is responsible for closing the stream. See {@link ObjectMapper#readValue(InputStream, Class)}
	 * for the details.
	 *
	 * @param stream     the stream
	 * @param valueClass the value class
	 * @return the value
	 * @throws JsonParseException   the parsing failed
	 * @throws JsonMappingException the mapping failed
	 * @throws IOException          I/O error occurred.
	 */
	public static <T> T fromJson(final InputStream stream, final Class<T> valueClass) throws IOException {
		ObjectMapper mapper = newJsonObjectMapper();
		return valueClass.cast(mapper.readValue(stream, valueClass));
	}

	/**
	 * Reads a value from a given JSON reader. Client is responsible for closing the reader. See {@link ObjectMapper#readValue(Reader, Class)} for the
	 * details.
	 *
	 * @param reader     the reader
	 * @param valueClass the value class
	 * @return the value
	 * @throws JsonParseException   the parsing failed
	 * @throws JsonMappingException the mapping failed
	 * @throws IOException          I/O error occurred.
	 */
	public static <T> T fromJson(final Reader reader, final Class<T> valueClass) throws IOException {
		ObjectMapper mapper = newJsonObjectMapper();
		return valueClass.cast(mapper.readValue(reader, valueClass));
	}

	/**
	 * Reads a value from a given JSON string. Client is responsible for closing the stream. See {@link ObjectMapper#readValue(String, Class)} for the
	 * details.
	 *
	 * @param string     the string
	 * @param valueClass the value class
	 * @return the value
	 * @throws JsonParseException   the parsing failed
	 * @throws JsonMappingException the mapping failed
	 * @throws IOException          I/O error occurred.
	 */
	public static <T> T fromJson(final String string, final Class<T> valueClass) throws IOException {
		ObjectMapper mapper = newJsonObjectMapper();
		return valueClass.cast(mapper.readValue(string, valueClass));
	}

	/**
	 * Reads a value from a given JSON string. Assumes that the type matches the received TypeReference. See {@link ObjectMapper#readValue(String,
	 * TypeReference)}.
	 *
	 * @param string        the string
	 * @param typeReference the type reference
	 * @return the value
	 * @throws JsonParseException       the parsing failed
	 * @throws JsonMappingException     the mapping failed
	 * @throws MismatchedInputException if the type of the list does not match the specified type
	 * @throws IOException              I/O error occurred.
	 */
	public static <T> T fromJson(final String string, final TypeReference<T> typeReference) throws IOException {
		ObjectMapper mapper = newJsonObjectMapper();
		return mapper.readValue(string, typeReference);
	}

	/**
	 * Reads CSV values from a given file according the specified class and column names. Client is responsible for closing the returned iterator.
	 * This method is a shortcut for the following code:
	 *
	 * <pre>
	 * <code>
	 * CsvMapper mapper = new CsvMapper();
	 * CsvSchema schema = mapper.schemaFor(valueClass).sortedBy(columnNames);
	 * mapper.reader(valueClass).with(schema).readValues(file);
	 * </code>
	 * </pre>
	 *
	 * @param file        the file
	 * @param valueClass  the value class
	 * @param columnNames the column names
	 * @return the iterator of values
	 * @throws JsonProcessingException processing failure
	 * @throws IOException             I/O error occurred.
	 */
	public static <T> MappingIterator<T> readCsv(final File file, final Class<T> valueClass, final String... columnNames) throws IOException {
		return newCsvObjectReader(valueClass, CsvSchema.DEFAULT_COLUMN_SEPARATOR, columnNames).readValues(file);
	}

	/**
	 * Reads CSV values from a given stream according the specified class and column names. Client is responsible for closing the returned iterator.
	 * Client is responsible for closing the supplied stream. This method is a shortcut for the following code:
	 *
	 * <pre>
	 * <code>
	 * CsvMapper mapper = new CsvMapper();
	 * CsvSchema schema = mapper.schemaFor(valueClass).sortedBy(columnNames);
	 * mapper.reader(valueClass).with(schema).readValues(stream);
	 * </code>
	 * </pre>
	 *
	 * @param stream      the stream
	 * @param valueClass  the value class
	 * @param columnNames the column names
	 * @return the iterator of values
	 * @throws JsonProcessingException processing failure
	 * @throws IOException             I/O error occurred.
	 */
	public static <T> MappingIterator<T> readCsv(final InputStream stream, final Class<T> valueClass, final String... columnNames)
			throws IOException {
		return newCsvObjectReader(valueClass, CsvSchema.DEFAULT_COLUMN_SEPARATOR, columnNames).readValues(stream);
	}

	/**
	 * Reads CSV values from a given file according the specified class and column names. Client is responsible for closing the returned iterator.
	 * This method is a shortcut for the following code:
	 *
	 * <pre>
	 * <code>
	 * CsvMapper mapper = new CsvMapper();
	 * CsvSchema schema = mapper.schemaFor(valueClass).sortedBy(columnNames);
	 * mapper.reader(valueClass).with(schema).readValues(file.toFile());
	 * </code>
	 * </pre>
	 *
	 * @param file        the file
	 * @param valueClass  the value class
	 * @param columnNames the column names
	 * @return the iterator of values
	 * @throws JsonProcessingException processing failure
	 * @throws IOException             I/O error occurred.
	 */
	public static <T> MappingIterator<T> readCsv(final Path file, final Class<T> valueClass, final String... columnNames) throws IOException {
		return readCsv(file.toFile(), valueClass, columnNames);
	}

	/**
	 * Reads CSV values from a given reader according the specified class and column names. Client is responsible for closing the returned iterator.
	 * Client is responsible for closing the supplied reader. This method is a shortcut for the following code:
	 *
	 * <pre>
	 * <code>
	 * CsvMapper mapper = new CsvMapper();
	 * CsvSchema schema = mapper.schemaFor(valueClass).sortedBy(columnNames);
	 * mapper.reader(valueClass).with(schema).readValues(reader);
	 * </code>
	 * </pre>
	 *
	 * @param reader      the reader
	 * @param valueClass  the value class
	 * @param columnNames the column names
	 * @return the iterator of values
	 * @throws JsonProcessingException processing failure
	 * @throws IOException             I/O error occurred.
	 */
	public static <T> MappingIterator<T> readCsv(final Reader reader, final Class<T> valueClass, final String... columnNames) throws IOException {
		return newCsvObjectReader(valueClass, CsvSchema.DEFAULT_COLUMN_SEPARATOR, columnNames).readValues(reader);
	}

	/**
	 * Reads CSV values from a given reader according the specified class, column separator and column names. Client is responsible for closing the
	 * returned iterator. Client is responsible for closing the supplied reader. This method is a shortcut for the following code:
	 *
	 * <pre>
	 * <code>
	 * CsvMapper mapper = new CsvMapper();
	 * CsvSchema schema = mapper.schemaFor(valueClass).sortedBy(columnNames);
	 * mapper.reader(valueClass).with(schema).readValues(reader);
	 * </code>
	 * </pre>
	 *
	 * @param reader          the reader
	 * @param valueClass      the value class
	 * @param columnSeparator the column separator of the file
	 * @param columnNames     the column names
	 * @return the iterator of values
	 * @throws JsonProcessingException processing failure
	 * @throws IOException             I/O error occurred.
	 */
	public static <T> MappingIterator<T> readCsv(final Reader reader, final Class<T> valueClass, final char columnSeparator,
			final String... columnNames) throws IOException {
		return newCsvObjectReader(valueClass, columnSeparator, columnNames).readValues(reader);
	}

	/**
	 * Reads CSV values from a given string according the specified class and column names. Client is responsible for closing the returned iterator.
	 * This method is a shortcut for the following code:
	 *
	 * <pre>
	 * <code>
	 * CsvMapper mapper = new CsvMapper();
	 * CsvSchema schema = mapper.schemaFor(valueClass).sortedBy(columnNames);
	 * mapper.reader(valueClass).with(schema).readValues(stream);
	 * </code>
	 * </pre>
	 *
	 * @param string      the string
	 * @param valueClass  the value class
	 * @param columnNames the column names
	 * @return the iterator of values
	 * @throws JsonProcessingException processing failure
	 * @throws IOException             I/O error occurred.
	 */
	public static <T> MappingIterator<T> readCsv(final String string, final Class<T> valueClass, final String... columnNames) throws IOException {
		return newCsvObjectReader(valueClass, CsvSchema.DEFAULT_COLUMN_SEPARATOR, columnNames).readValues(string);
	}

	/**
	 * Writes a given object as JSON to a given file. See {@link ObjectMapper#writeValue(OutputStream, Object)} for the details.
	 *
	 * @param file  the file
	 * @param value the value
	 * @throws JsonGenerationException the generation failed
	 * @throws JsonMappingException    the mapping failed
	 * @throws IOException             I/O error occurred.
	 */
	public static void toJson(final File file, final Object value) throws JsonMappingException, IOException {
		newJsonObjectMapper().writeValue(file, value);
	}

	/**
	 * Writes a given object as a JSON string. See {@link ObjectMapper#writeValueAsString(Object)} for the details.
	 *
	 * @param value the value
	 * @return the JSON string
	 * @throws JsonProcessingException the processing failed.
	 */
	public static String toJson(final Object value) throws JsonProcessingException {
		return newJsonObjectMapper().writeValueAsString(value);
	}

	/**
	 * Writes a given object as JSON to a given stream. Client is responsible for closing the stream. See {@link ObjectMapper#writeValue(OutputStream,
	 * Object)} for the details.
	 *
	 * @param stream the stream
	 * @param value  the value
	 * @throws JsonGenerationException the generation failed
	 * @throws JsonMappingException    the mapping failed
	 * @throws IOException             I/O error occurred.
	 */
	public static void toJson(final OutputStream stream, final Object value) throws IOException {
		newJsonObjectMapper().writeValue(stream, value);
	}

	/**
	 * Writes a given object as JSON to a given file. See {@link ObjectMapper#writeValue(OutputStream, Object)} for the details.
	 *
	 * @param file  the file
	 * @param value the value
	 * @throws JsonGenerationException the generation failed
	 * @throws JsonMappingException    the mapping failed
	 * @throws IOException             I/O error occurred.
	 */
	public static void toJson(final Path file, final Object value) throws IOException {
		toJson(file.toFile(), value);
	}

	/**
	 * Writes a given object as JSON to a given writer. Client is responsible for closing the writer. See {@link ObjectMapper#writeValue(Writer,
	 * Object)} for the details.
	 *
	 * @param writer the writer
	 * @param value  the value
	 * @throws JsonGenerationException the generation failed
	 * @throws JsonMappingException    the mapping failed
	 * @throws IOException             I/O error occurred.
	 */
	public static void toJson(final Writer writer, final Object value) throws IOException {
		newJsonObjectMapper().writeValue(writer, value);
	}

	private static ObjectReader newCsvObjectReader(final Class<?> valueClass, final char columnSeparator, final String... columnNames) {
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(valueClass).withColumnSeparator(columnSeparator);
		if (columnNames.length > 0) {
			schema = schema.sortedBy(columnNames);
		}
		return mapper.readerFor(valueClass).with(schema);
	}

	private static ObjectMapper newJsonObjectMapper() {
		return JSON_PROTOTYPE.copy();
	}
}
