/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.mixnet.HadamardArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.MultiExponentiationArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.ProductArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.ShuffleArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.SingleValueProductArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.mixnet.ZeroArgument;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;

/**
 * Global configuration of the {@link ObjectMapper} needed to serialize/deserialize the mixnet domain objects. This configuration adds all necessary
 * mixIns and serializers.
 */
public class ObjectMapperMixnetConfig {

	private ObjectMapperMixnetConfig() {
		// Intentionally left blank.
	}

	/**
	 * @return a new instance of an already configured {@link ObjectMapper}.
	 */
	public static ObjectMapper getNewInstance() {
		return new ObjectMapper()
				// Primitive elements.
				.addMixIn(GqElement.class, GqElementMixIn.class).addMixIn(ZqElement.class, ZqElementMixIn.class)
				.addMixIn(GqGroup.class, GqGroupMixIn.class)
				// ElGamal objects.
				.addMixIn(ElGamalMultiRecipientCiphertext.class, ElGamalMultiRecipientCiphertextMixIn.class)
				.addMixIn(ElGamalMultiRecipientMessage.class, ElGamalMultiRecipientMessageMixIn.class)
				.addMixIn(ElGamalMultiRecipientPublicKey.class, ElGamalMultiRecipientPublicKeyMixIn.class)
				.addMixIn(ElGamalMultiRecipientPrivateKey.class, ElGamalMultiRecipientPrivateKeyMixIn.class)
				// Verifiable.
				.addMixIn(VerifiableShuffle.class, VerifiableShuffleMixIn.class)
				.addMixIn(VerifiableDecryptions.class, VerifiableDecryptionsMixIn.class)
				// Proofs.
				.addMixIn(DecryptionProof.class, DecryptionProofMixIn.class).addMixIn(ExponentiationProof.class, ExponentiationProofMixIn.class)
				// Arguments.
				.addMixIn(ShuffleArgument.class, ShuffleArgumentMixIn.class)
				.addMixIn(MultiExponentiationArgument.class, MultiExponentiationArgumentMixIn.class)
				.addMixIn(ProductArgument.class, ProductArgumentMixIn.class)
				.addMixIn(SingleValueProductArgument.class, SingleValueProductArgumentMixIn.class)
				.addMixIn(HadamardArgument.class, HadamardArgumentMixIn.class).addMixIn(ZeroArgument.class, ZeroArgumentMixIn.class)
				// Arguments Builders.
				.addMixIn(ShuffleArgument.Builder.class, ShuffleArgumentMixIn.ShuffleArgumentBuilderMixin.class)
				.addMixIn(MultiExponentiationArgument.Builder.class, MultiExponentiationArgumentMixIn.MultiExponentiationArgumentBuilderMixIn.class)
				.addMixIn(SingleValueProductArgument.Builder.class, SingleValueProductArgumentMixIn.SingleValueProductArgumentBuilderMixIn.class)
				.addMixIn(ZeroArgument.Builder.class, ZeroArgumentMixIn.ZeroArgumentBuilderMixIn.class);
	}

}
