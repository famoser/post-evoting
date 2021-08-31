/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.crypto;

import java.security.PrivateKey;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;

/**
 * Repository for handling Private Keys entities for specific object like ballot box.
 */
public class PrivateKeyForObjectRepository {

	private final KeystoreForObjectOpener keystoreOpener;

	private final PasswordForObjectRepository passwordRepository;

	/**
	 * Constructor for a keystore based private key repository.
	 *
	 * @param keystoreOpener     the keystore opener that will be used to locate and retrieve the keystore instance.
	 * @param passwordRepository the repository that will be used to retrieve the private key password.
	 */
	public PrivateKeyForObjectRepository(KeystoreForObjectOpener keystoreOpener, PasswordForObjectRepository passwordRepository) {
		this.keystoreOpener = keystoreOpener;
		this.passwordRepository = passwordRepository;
	}

	/**
	 * Searches for an private key with the given tenant, election event, object identifier and alias.
	 *
	 * @param tenantId - the identifier of the tenant.
	 * @param eeId     - the identifier of the electionEvent.
	 * @param objectId - the identifier of the object.
	 * @param alias    - the private key alias.
	 * @return a entity representing the private key.
	 * @throws CryptographicOperationException if there is an error recovering the private key.
	 * @throws ResourceNotFoundException       if the keystore containing the private key can not be found.
	 */
	public PrivateKey findByTenantEEIDObjectIdAlias(String tenantId, String eeId, String objectId, String alias)
			throws CryptographicOperationException, ResourceNotFoundException {

		String password = passwordRepository.getByTenantEEIDObjectId(tenantId, eeId, objectId);
		PrivateKey privateKey;
		try {
			CryptoAPIExtendedKeyStore keystore = keystoreOpener.openForTenantEEIDObjectId(tenantId, eeId, objectId);
			privateKey = keystore.getPrivateKeyEntry(alias, password.toCharArray());
		} catch (GeneralCryptoLibException e) {
			throw new CryptographicOperationException("Can not recover private key from keystore: ", e);
		}
		return privateKey;
	}
}
