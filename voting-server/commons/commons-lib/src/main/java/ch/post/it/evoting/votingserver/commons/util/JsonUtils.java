/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The util class to handle json.
 */
public final class JsonUtils {

	/**
	 * Non-public constructor
	 */
	private JsonUtils() {

	}

	/**
	 * Convert json string to json object.
	 *
	 * @param json - the json in string format.
	 * @return the JsonObject corresponding to json string.
	 */
	public static JsonObject getJsonObject(String json) {
		JsonReader jsonReader = Json.createReader(new StringReader(json));
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();
		return jsonObject;
	}

	/**
	 * Convert json string to json object.
	 *
	 * @param json - the json in string format.
	 * @return the JsonObject corresponding to json string.
	 */
	public static JsonArray getJsonArray(String json) {
		JsonReader jsonReader = Json.createReader(new StringReader(json));
		JsonArray jsonObject = jsonReader.readArray();
		jsonReader.close();
		return jsonObject;
	}

	/**
	 * Returns a JsonObjectBuilder with the properties of a given json object. It allows to add new properties to the original json object.
	 *
	 * @param jo - json object.
	 * @return - json object builder with the properties of the original object
	 */
	public static JsonObjectBuilder jsonObjectToBuilder(JsonObject jo) {
		JsonObjectBuilder job = Json.createObjectBuilder();
		for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
			job.add(entry.getKey(), entry.getValue());
		}
		return job;
	}

	/**
	 * This method removes any found field from the given json having the name from the given array of field names and returns it as string. If the
	 * json is null or empty, null is returned.
	 *
	 * @param json       The json from which to remove fields.
	 * @param fieldNames The name of the fields that are removed.
	 * @return a string in json format with the requested fields removed.
	 * @throws JsonParseException if there is a problem with the json parsing of the given string.
	 * @throws IOException        if there is a I/O problem with the reading of the tree from the parser.
	 */
	public static String removeFieldsFromJson(String json, String... fieldNames) throws IOException {

		if (fieldNames == null || fieldNames.length == 0) {
			return null;
		}

		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createJsonParser(json);
		ObjectMapper mapper = new ObjectMapper(factory);
		JsonNode rootNode = mapper.readTree(parser);

		remove(rootNode, fieldNames);
		return rootNode.toString();
	}

	/**
	 * This method unescapes a JSON string in order to make it more human readable.
	 *
	 * @param json The JSON string to unescape.
	 */
	public static String unescapeJsonString(String json) {
		String unescapedJson = json;

		do {
			unescapedJson = unescapedJson.replace("\\\\", "\\");
		} while (unescapedJson.contains("\\\\"));

		unescapedJson = unescapedJson.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}").replace("\"[", "[").replace("]\"", "]");

		return unescapedJson;
	}

	// This method removes the fields having the names from the given fieldNames array. It is called
	// recursively in order to
	// go deep in the structure until an object is found. The type of the given node is always checked
	// in order to treat it
	// as it should: nodes can be removed from an objectNode, and with an arrayNode we should go
	// deeper in the structure
	private static void remove(JsonNode node, String... fieldNames) {
		JsonNode nodeFieldsRemoved;

		if (node instanceof ObjectNode) {
			nodeFieldsRemoved = removeThem(node, fieldNames);
		} else if (node instanceof ArrayNode) {
			ArrayNode array = (ArrayNode) node;
			for (JsonNode jsonNodeFromArray : array) {
				remove(jsonNodeFromArray, fieldNames);
			}
			nodeFieldsRemoved = array;
		} else {
			nodeFieldsRemoved = node;
		}

		for (JsonNode jsonNode : nodeFieldsRemoved) {
			if (jsonNode instanceof ObjectNode) {
				removeThem(jsonNode, fieldNames);
			} else if (jsonNode instanceof ArrayNode) {
				ArrayNode array = (ArrayNode) jsonNode;
				for (JsonNode jsonNodeFromArray : array) {
					remove(jsonNodeFromArray, fieldNames);
				}
			} else {
				remove(jsonNode, fieldNames);
			}
		}
	}

	// This method does the actual removing of the fields with the given names. Tries the removal with
	// all the field names
	// without checking first if exists.
	private static JsonNode removeThem(JsonNode jsonNode, String... fieldNames) {
		ObjectNode object = (ObjectNode) jsonNode;
		for (int index = 0; index < fieldNames.length; index++) {
			object.remove(fieldNames[index]);
		}
		return jsonNode;
	}
}
