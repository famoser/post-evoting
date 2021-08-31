/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.sign;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonWriter;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;

public class SignerMetadata {

	private static final String COMPONENT = "component";
	private static final String FILENAME = "filename";
	private static final String METADATA_SUFFIX = ".metadata";

	/**
	 * Create a file signature and write the result to a metadata file.
	 *
	 * @param signingKey   the {@link java.security.PrivateKey} signing key. Must be non-null.
	 * @param originalPath the path of the file to sign. Must be non-null.
	 * @throws IOException               if an I/O error occurs when opening an FileInputStream from the originalPath.
	 * @throws GeneralCryptoLibException if the signing process does not successfully complete.
	 */
	public void sign(PrivateKey signingKey, final Path originalPath) throws IOException, GeneralCryptoLibException {
		checkNotNull(signingKey);
		checkNotNull(originalPath);

		try (FileInputStream fileInput = new FileInputStream(originalPath.toFile())) {
			Map<String, String> map = new LinkedHashMap<>();
			map.put(COMPONENT, "eCH-Tools");
			map.put(FILENAME, originalPath.toString());

			MetadataFileSigner mdfs = new MetadataFileSigner(new AsymmetricService());
			SignatureMetadata signature = mdfs.createSignature(signingKey, fileInput, map);

			OutputStream os = new FileOutputStream(originalPath + METADATA_SUFFIX);
			try (JsonWriter json = Json.createWriter(os)) {
				json.writeObject(signature.toJsonObject());
			}

		}
	}
}
