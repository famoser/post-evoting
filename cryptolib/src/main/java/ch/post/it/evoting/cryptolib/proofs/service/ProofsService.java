/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import java.util.Properties;

import ch.post.it.evoting.cryptolib.CryptolibService;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofPreComputerAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.MaurerProofPolicyFromProperties;
import ch.post.it.evoting.cryptolib.proofs.maurer.factory.MaurerUnifiedProofFactory;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Implementation of the {@link ProofsService}.
 */
public final class ProofsService extends CryptolibService implements ProofsServiceAPI {

	private static final String ZP_SUBGROUP_LABEL = "Zp subgroup";
	private final MaurerUnifiedProofFactory factory;
	private final ElGamalServiceAPI elGamalService;

	/**
	 * Default Constructor that uses a default path location.
	 */
	public ProofsService() {
		final MaurerProofPolicyFromProperties maurerProofPolicy = new MaurerProofPolicyFromProperties();

		PrimitivesServiceAPI primitivesService = new PrimitivesService();
		HashBuilder hashBuilder = new HashBuilder(primitivesService, maurerProofPolicy.getCharset().getCharset());

		factory = new MaurerUnifiedProofFactory(maurerProofPolicy, hashBuilder);

		// Initialise an ElGamal service to be able to check group policies.
		elGamalService = new ElGamalService();
	}

	/**
	 * Constructor which initializes its state using the properties file located at the specified path.
	 *
	 * @param properties the properties to be used to configure the service.
	 */
	public ProofsService(Properties properties) {
		final MaurerProofPolicyFromProperties maurerProofPolicy = new MaurerProofPolicyFromProperties(properties);

		PrimitivesServiceAPI primitivesService = new PrimitivesService(properties);
		HashBuilder hashBuilder = new HashBuilder(primitivesService, maurerProofPolicy.getCharset().getCharset());

		factory = new MaurerUnifiedProofFactory(maurerProofPolicy, hashBuilder);

		// Initialise an ElGamal service to be able to check group policies.
		elGamalService = new ElGamalService(properties);
	}

	@Override
	public ProofProverAPI createProofProverAPI(final MathematicalGroup<?> group) throws GeneralCryptoLibException {

		Validate.notNull(group, ZP_SUBGROUP_LABEL);
		elGamalService.checkGroupPolicy(group);

		return factory.createProofCreationAPI(group);
	}

	@Override
	public ProofVerifierAPI createProofVerifierAPI(final MathematicalGroup<?> group) throws GeneralCryptoLibException {

		Validate.notNull(group, ZP_SUBGROUP_LABEL);
		elGamalService.checkGroupPolicy(group);

		return factory.createProofVerificationAPI(group);
	}

	@Override
	public ProofPreComputerAPI createProofPreComputerAPI(final MathematicalGroup<?> group) throws GeneralCryptoLibException {

		Validate.notNull(group, ZP_SUBGROUP_LABEL);
		elGamalService.checkGroupPolicy(group);

		return factory.createProofPreComputationAPI(group);
	}
}
