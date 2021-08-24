/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.config;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.config.commands.ChainValidator;
import ch.post.it.evoting.config.commands.PasswordReaderUtils;
import ch.post.it.evoting.config.commands.encryptionparameters.ConfigEncryptionParametersAdapter;
import ch.post.it.evoting.config.commands.encryptionparameters.ConfigEncryptionParametersGenerator;
import ch.post.it.evoting.config.commands.encryptionparameters.ConfigEncryptionParametersHolder;
import ch.post.it.evoting.config.commands.encryptionparameters.ConfigOutputSerializer;
import ch.post.it.evoting.config.commands.primes.PrimeGroupMembersProvider;
import ch.post.it.evoting.config.commands.primes.PrimesParametersAdapter;
import ch.post.it.evoting.config.commands.primes.PrimesParametersHolder;
import ch.post.it.evoting.config.commands.primes.PrimesSerializer;
import ch.post.it.evoting.config.exception.CreatePrimeGroupMembersException;
import ch.post.it.evoting.config.exception.InvalidPasswordException;
import ch.post.it.evoting.config.model.SignedObject;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.api.stores.bean.KeyStoreType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.cmssigner.CMSSigner;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;

import io.jsonwebtoken.Jwts;

/**
 * Main class in charge of execute the three configuration steps (adapt, generate and serialize) for the command ordered.
 */
@Service
public class ConfigurationCommandLine implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationCommandLine.class);

	private static final String MALFORMED_KEYSTORE = "Malformed Keystore";
	private static final Pattern pattern = Pattern.compile("[0-9]{23}");

	private final ConfigEncryptionParametersGenerator configEncryptionParametersGenerator;
	private final ConfigEncryptionParametersAdapter preConfigAdapter;
	private final ConfigOutputSerializer configOutputSerializer;
	private final PrimesParametersAdapter primesParametersAdapter;
	private final PrimeGroupMembersProvider primeGroupMembersProvider;
	private final PrimesSerializer primesSerializer;
	private final StoresServiceAPI storesService;

	private CertificateFactory cf;

	@Autowired
	public ConfigurationCommandLine(final ConfigEncryptionParametersGenerator configEncryptionParametersGenerator,
			final ConfigEncryptionParametersAdapter preConfigAdapter, final ConfigOutputSerializer configOutputSerializer,
			final PrimesParametersAdapter primesParametersAdapter, final PrimeGroupMembersProvider primeGroupMembersProvider,
			final PrimesSerializer primesSerializer, final StoresServiceAPI storesService) {

		this.configEncryptionParametersGenerator = configEncryptionParametersGenerator;
		this.preConfigAdapter = preConfigAdapter;
		this.configOutputSerializer = configOutputSerializer;
		this.primesParametersAdapter = primesParametersAdapter;
		this.primeGroupMembersProvider = primeGroupMembersProvider;
		this.primesSerializer = primesSerializer;
		this.storesService = storesService;

		Security.addProvider(new BouncyCastleProvider());
		try {
			cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
		} catch (CertificateException e) {
			LOGGER.error("Failed to instantiate Certificate Factory.", e);
		} catch (NoSuchProviderException e) {
			LOGGER.error("Failed to get BC provider.", e);
		}
	}

	private static PrivateKeyEntry decodePrivateKeyEntry(KeyStore store, String alias, PasswordProtection protection) throws KeyManagementException {
		try {
			if (!store.entryInstanceOf(alias, PrivateKeyEntry.class)) {
				throw new KeyManagementException(format("Key entry ''{0}'' is missing or invalid.", alias));
			}
			return (PrivateKeyEntry) store.getEntry(alias, protection);
		} catch (UnrecoverableKeyException e) {
			throw new InvalidPasswordException("Invalid key password.", e);
		} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
			throw new KeyManagementException("Failed to decode private key entry.", e);
		}
	}

	/**
	 * Generates Encryption Parameters data, and then serializes and signs that data.
	 *
	 * @param receivedParameters the received parameters.
	 * @throws IOException
	 */
	public void generateEncryptionParameters(final Parameters receivedParameters) throws IOException {

		LOGGER.info("Starting to generate the encryption parameters...");

		LOGGER.info("Collecting input parameters...");

		final ConfigEncryptionParametersHolder holder = preConfigAdapter.adapt(receivedParameters);

		char[] password = PasswordReaderUtils.readPasswordFromConsole();

		KeyStore store = decodeKeyStore(holder.getP12Path().toString(), password);
		PrivateKey privateKey = loadPrivateKeyFromKeyStore(store, password);

		LOGGER.info("Deleting password from memory...");

		clean(password);

		LOGGER.info("Reading CA trusted file");
		Certificate trustedCA = readTrustedCA(holder.getTrustedCAPath());

		LOGGER.info("Verifying seed's signature...");
		byte[] seedSignature = Files.readAllBytes(holder.getSeedSignaturePath());

		try (InputStream seed = Files.newInputStream(holder.getSeedPath())) {
			Certificate[][] signers = CMSSigner.verify(seedSignature, seed);

			if (signers.length != 1 && signers[0].length < 1) {
				throw new IllegalArgumentException("Seed signature signers could not be recovered.");
			}

			Certificate[] chain = new Certificate[signers[0].length - 1];
			System.arraycopy(signers[0], 1, chain, 0, signers[0].length - 1);
			ChainValidator.validateChain(trustedCA, chain, signers[0][0], X509CertificateType.SIGN);
		} catch (CMSException | GeneralCryptoLibException e) {
			LOGGER.error("Seed signature could not be verified.");
			throw new IllegalArgumentException(e);
		}

		LOGGER.info("Generating the encryption parameters...");

		final VerifiableElGamalEncryptionParameters encryptionParameters = configEncryptionParametersGenerator.generate(holder.getSeedPath());

		LOGGER.info("Processing the output...");

		final Path writtenFilePath = configOutputSerializer.serialize(encryptionParameters, privateKey, holder.getOutputPath());

		LOGGER.info("The pre-configuration was generated correctly. It can be found in: {}", writtenFilePath);
	}

	/**
	 * Generates the group prime numbers from the encryption parameters, and then serializes and signs that data.
	 *
	 * @param parameters the received parameters.
	 */
	void generatePrimeGroupMembers(final Parameters parameters) {

		try {
			LOGGER.info("Starting the generation of prime group members");

			LOGGER.info("Collecting parameters...");

			final PrimesParametersHolder holder = primesParametersAdapter.adapt(parameters);

			LOGGER.info("Reading keyStore...");
			char[] password = PasswordReaderUtils.readPasswordFromConsole();

			KeyStore store = decodeKeyStore(holder.getP12Path(), password);
			PrivateKey privateKey = loadPrivateKeyFromKeyStore(store, password);
			Certificate[] fullChain = loadCertificateChainFromKeyStore(store);
			Certificate signer = fullChain[0];
			Certificate[] innerChain = Arrays.copyOfRange(fullChain, 1, fullChain.length);

			LOGGER.info("Deleting password from memory...");

			clean(password);

			LOGGER.info("Validating chain from keyStore and trusted CA...");
			Certificate trustedCA = readTrustedCA(Paths.get(holder.getTrustedCAPath()));

			ChainValidator.validateChain(trustedCA, innerChain, signer, X509CertificateType.SIGN);

			LOGGER.info("Getting and verifying inputed parameters");
			String timeStamp = getTimeStampFromFile(holder.getEncryptionParametersPath());

			final SignedObject signedObject = readSignedObject(Paths.get(holder.getEncryptionParametersPath()));

			// Verify signature.
			final Map<String, Object> claimMapRecovered = Jwts.parser().setSigningKey(signer.getPublicKey())
					.parseClaimsJws(signedObject.getSignature()).getBody();
			final ObjectMapper mapper = new ObjectMapper();
			final Object recoveredSignedObject = claimMapRecovered.get("objectToSign");
			final VerifiableElGamalEncryptionParameters params = mapper
					.convertValue(recoveredSignedObject, VerifiableElGamalEncryptionParameters.class);

			LOGGER.info("Generating prime group members...");

			MathematicalGroup<?> group = params.getGroup();

			List<BigInteger> primesGroupMembers = primeGroupMembersProvider
					.generateVotingOptionRepresentations(group.getP(), group.getQ(), group.getG());

			LOGGER.info("Processing the output...");

			primesSerializer.serializePrimes(primesGroupMembers, timeStamp, holder.getOutputPath(), privateKey, Arrays.asList(innerChain), signer);

			LOGGER.info("The prime group members were generated. They can be found in:");
			LOGGER.info("\t{}", holder.getOutputPath());
		} catch (IOException e) {
			LOGGER.error("Could not open encryptionParameters file.");
			throw new CreatePrimeGroupMembersException(e);
		} catch (CMSException e) {
			LOGGER.error("Could not generate primes file signature.");
			throw new CreatePrimeGroupMembersException(e);
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("Error validating certificate chain.");
			throw new CreatePrimeGroupMembersException(e);
		}
	}

	private SignedObject readSignedObject(Path path) throws IOException {
		try (InputStream stream = Files.newInputStream(path)) {
			final ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(stream, SignedObject.class);
		}
	}

	/**
	 * The keyStore must contain only one {@link KeyStore.PrivateKeyEntry}. Discover what is the alias for that entry.
	 */
	private String getAlias(final KeyStore keyStore) throws KeyStoreException {
		String privateKeyEntryAlias = null;
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String currentAlias = aliases.nextElement();
			if (privateKeyEntryAlias == null) {
				if (aliasPKEntry(keyStore, currentAlias)) {
					privateKeyEntryAlias = currentAlias;
				}
			} else {
				if (aliasPKEntry(keyStore, currentAlias)) {
					throw new KeyStoreException(MALFORMED_KEYSTORE);
				}
			}
		}
		return privateKeyEntryAlias;
	}

	private boolean aliasPKEntry(final KeyStore keyStore, final String alias) throws KeyStoreException {
		return keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class);
	}

	private PrivateKey loadPrivateKeyFromKeyStore(final KeyStore keyStore, final char[] protection) {
		try {
			PrivateKeyEntry privateKeyEntry = decodePrivateKeyEntry(keyStore, getAlias(keyStore), new PasswordProtection(protection));
			return privateKeyEntry.getPrivateKey();
		} catch (KeyManagementException | KeyStoreException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private Certificate[] loadCertificateChainFromKeyStore(KeyStore keyStore) {
		try {
			return keyStore.getCertificateChain(getAlias(keyStore));
		} catch (KeyStoreException e) {
			throw new IllegalArgumentException("Failed to decode private key entry.", e);
		}
	}

	private KeyStore decodeKeyStore(final String p12Path, char[] protection) {
		try (InputStream stream = Files.newInputStream(Paths.get(p12Path))) {
			return storesService.loadKeyStore(KeyStoreType.PKCS12, stream, protection);
		} catch (GeneralCryptoLibException e) {
			if (e.getCause() instanceof IOException && e.getCause().getCause() instanceof UnrecoverableKeyException) {
				throw new IllegalArgumentException("Key store password is invalid.", e);
			} else {
				throw new IllegalArgumentException("Failed to decode the key store.", e);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to decode the key store.", e);
		}
	}

	private Certificate readTrustedCA(final Path trustedCAPath) throws IOException {
		try (InputStream s = Files.newInputStream(trustedCAPath)) {
			return cf.generateCertificate(s);
		} catch (CertificateException e) {
			throw new IllegalArgumentException("Failed reading the Trusted CA PEM.", e);
		}
	}

	private String getTimeStampFromFile(String encryptionParametersPath) {
		Matcher matcher = pattern.matcher(encryptionParametersPath);

		if (!matcher.find()) {
			throw new IllegalArgumentException("Encryption parameters file, does not contain a valid timestamp.");
		}

		return matcher.group();
	}

	@Override
	public void run(final String... args) throws Exception {
		final Command processedCommand = MainParametersProcessor.process(args);

		if (processedCommand == null) {
			return;
		}

		final MutuallyExclusiveCommand action = processedCommand.getIdentifier();

		switch (action) {
		case HELP:
			// Nothing to do, is done inside MainParametersProcessor.process
			break;
		case GEN_ENCRYPTION_PARAM:
			generateEncryptionParameters(processedCommand.getParameters());
			break;
		case GEN_PRIME_GROUP_MEMBERS:
			generatePrimeGroupMembers(processedCommand.getParameters());
			break;
		default:
			break;
		}
	}

	private void clean(final char[] password) {
		Arrays.fill(password, '\u0000');
	}
}
