/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;

/**
 * Configuration which publish mock implementation of {@link KeysManager}.
 */
@Configuration
public class KeyManagerMockConfig {

	private final KeyPair KEY_PAIR = new AsymmetricService().getKeyPairForSigning();

	@Bean
	public KeysManager keyManager() throws KeyManagementException, GeneralCryptoLibException {
		final String publicKey = "{\"publicKey\":{\"zpSubgroup\":{\"g\":\"Ag==\",\"p\":\"AIGt8wzizyJ8+ia716YgY4FjG5gJRjfz+iB3OeWEMLCOLnoWG0wHwFQyo2VNx4lgBPDmSIb5NtJ/12U3dfe7JdgYjEMSaz9HUrfowHNwCuD1VVmDXxUvyxPSV5/iss7a85sopvR9n09imvvp8XDfPNuccd10TQwGSKVt2vN7vSMrgQOsGEuBBsEmsdlRil4+x9NTYphsc/7vTV9pRN4U+ZI5yTbBof6SFcH5mwAatlZB/RztwA1wh3qUUgyLz/kTKqTRbaPc5W1+LDPBZnMiaTWq786418pio4oM86mVIthfE/34S0kn8GpJGzNF12NzbVQzbpv6YpmrPKG1FfaQPwc=\",\"q\":\"QNb5hnFnkT59E13r0xAxwLGNzASjG/n9EDuc8sIYWEcXPQsNpgPgKhlRsqbjxLACeHMkQ3ybaT/rspu6+92S7AxGIYk1n6OpW/RgObgFcHqqrMGvipfliekrz/FZZ215zZRTej7Pp7FNffT4uG+ebc447romhgMkUrbteb3ekZXAgdYMJcCDYJNY7KjFLx9j6amxTDY5/3emr7Sibwp8yRzkm2DQ/0kK4PzNgA1bKyD+jnbgBrhDvUopBkXn/ImVUmi20e5ytr8WGeCzOZE0mtV351xr5TFRxQZ51MqRbC+J/vwlpJP4NSSNmaLrsbm2qhm3Tf0xTNWeUNqK+0gfgw==\"},\"elements\":[\"cDEirCGORSF6n3eWy0zxrjASTNBes9xCXLEJL9AigcUtAEauqgzQEOE+r2SE1eIXHbf43Iv4RT1ffKD6msPbKOppmB6bdxOtlzHK/6HJxJ9z6zNa75OTB2NbRCxGLhuN92joFFN8uQqiZ87PBYE4ZmA347gjVSvJsolsoFki2r4gLRITFbo/0CYFZqUXUgIzbyTX7DYIjCWY9o4sXofd1Ay3QgdHvN6HEVpdUc62bMxu1q9wZopPFdA03fadGyKeQkT93EgIAcCSuMemfgNEcONEieMSg3pLIKy68dKfxIP7y09WdY4f+to6d+xJVU/61/Gp41yPNS9n9BruQgsdAA==\", \"cDEirCGORSF6n3eWy0zxrjASTNBes9xCXLEJL9AigcUtAEauqgzQEOE+r2SE1eIXHbf43Iv4RT1ffKD6msPbKOppmB6bdxOtlzHK/6HJxJ9z6zNa75OTB2NbRCxGLhuN92joFFN8uQqiZ87PBYE4ZmA347gjVSvJsolsoFki2r4gLRITFbo/0CYFZqUXUgIzbyTX7DYIjCWY9o4sXofd1Ay3QgdHvN6HEVpdUc62bMxu1q9wZopPFdA03fadGyKeQkT93EgIAcCSuMemfgNEcONEieMSg3pLIKy68dKfxIP7y09WdY4f+to6d+xJVU/61/Gp41yPNS9n9BruQgsdAA==\"]}}";
		final String privateKey = "{\"privateKey\":{\"zpSubgroup\":{\"g\":\"Ag==\",\"p\":\"AIGt8wzizyJ8+ia716YgY4FjG5gJRjfz+iB3OeWEMLCOLnoWG0wHwFQyo2VNx4lgBPDmSIb5NtJ/12U3dfe7JdgYjEMSaz9HUrfowHNwCuD1VVmDXxUvyxPSV5/iss7a85sopvR9n09imvvp8XDfPNuccd10TQwGSKVt2vN7vSMrgQOsGEuBBsEmsdlRil4+x9NTYphsc/7vTV9pRN4U+ZI5yTbBof6SFcH5mwAatlZB/RztwA1wh3qUUgyLz/kTKqTRbaPc5W1+LDPBZnMiaTWq786418pio4oM86mVIthfE/34S0kn8GpJGzNF12NzbVQzbpv6YpmrPKG1FfaQPwc=\",\"q\":\"QNb5hnFnkT59E13r0xAxwLGNzASjG/n9EDuc8sIYWEcXPQsNpgPgKhlRsqbjxLACeHMkQ3ybaT/rspu6+92S7AxGIYk1n6OpW/RgObgFcHqqrMGvipfliekrz/FZZ215zZRTej7Pp7FNffT4uG+ebc447romhgMkUrbteb3ekZXAgdYMJcCDYJNY7KjFLx9j6amxTDY5/3emr7Sibwp8yRzkm2DQ/0kK4PzNgA1bKyD+jnbgBrhDvUopBkXn/ImVUmi20e5ytr8WGeCzOZE0mtV351xr5TFRxQZ51MqRbC+J/vwlpJP4NSSNmaLrsbm2qhm3Tf0xTNWeUNqK+0gfgw==\"},\"exponents\":[\"DWOyiVH88d/gzjBCQZenbbstssyQKlHOo5+dpbZ3OV2Wko8K39cqCTHZmn1xXmFWSm/GIN6oI0IzMFHRdECGuFgEzF6a+SbYijtJyp2IodHvLvbul7ZSEj5Z1m4giGrQ+NWppu3kHZzP6u1gu7sY4BF3sAwT9rGPZrc9HadMdXspip5dxUVv30y0jTUenbksfUh/CoIpCxQXSMhIqYYZv/bK9RAKVwo1L3QeiucpefqF/14/4Li/idsskvkrsfOHixNJ2j3UV5zvPNrfyRA3oLxi1Ci9DDeQ3S12Svg9mzrKuuU0TXGwx509YubAi3fz6uUyi24tQCVmjQZ5ZGGeNA==\", \"DWOyiVH88d/gzjBCQZenbbstssyQKlHOo5+dpbZ3OV2Wko8K39cqCTHZmn1xXmFWSm/GIN6oI0IzMFHRdECGuFgEzF6a+SbYijtJyp2IodHvLvbul7ZSEj5Z1m4giGrQ+NWppu3kHZzP6u1gu7sY4BF3sAwT9rGPZrc9HadMdXspip5dxUVv30y0jTUenbksfUh/CoIpCxQXSMhIqYYZv/bK9RAKVwo1L3QeiucpefqF/14/4Li/idsskvkrsfOHixNJ2j3UV5zvPNrfyRA3oLxi1Ci9DDeQ3S12Svg9mzrKuuU0TXGwx509YubAi3fz6uUyi24tQCVmjQZ5ZGGeNA==\"]}}";

		final KeysManager manager = mock(KeysManager.class);
		when(manager.getCcmjElectionSecretKey(any())).thenReturn(ElGamalPrivateKey.fromJson(privateKey));
		when(manager.getCcmjElectionPublicKey(any())).thenReturn(ElGamalPublicKey.fromJson(publicKey));
		when(manager.hasValidElectionSigningKeys(any(), any(Date.class), any(Date.class))).thenReturn(Boolean.TRUE);
		when(manager.getCcmjElectionPublicKeySignature(any())).thenReturn("Signature".getBytes(StandardCharsets.UTF_8));
		when(manager.getPlatformCACertificate()).thenReturn(createTestCertificate().getCertificate());
		when(manager.getElectionSigningPublicKey(any())).thenReturn(KEY_PAIR.getPublic());
		when(manager.getElectionSigningPrivateKey(any())).thenReturn(KEY_PAIR.getPrivate());
		return manager;
	}

	private CryptoAPIX509Certificate createTestCertificate() throws GeneralCryptoLibException {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime end = now.plusYears(1);
		ValidityDates validityDates = new ValidityDates(Date.from(now.toInstant()), Date.from(end.toInstant()));

		CertificateData certificateData = new CertificateData();
		certificateData.setSubjectPublicKey(KEY_PAIR.getPublic());
		certificateData.setValidityDates(validityDates);
		X509DistinguishedName distinguishedName = new X509DistinguishedName.Builder("certId", "CH").build();
		certificateData.setSubjectDn(distinguishedName);
		certificateData.setIssuerDn(distinguishedName);

		return new CertificatesService().createSignX509Certificate(certificateData, KEY_PAIR.getPrivate());
	}
}
