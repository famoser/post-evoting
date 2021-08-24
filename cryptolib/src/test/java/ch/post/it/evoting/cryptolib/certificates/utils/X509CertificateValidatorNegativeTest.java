/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.certificates.utils;

import java.security.KeyPair;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.utils.AsymmetricTestDataGenerator;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.configuration.X509CertificateValidationData;
import ch.post.it.evoting.cryptolib.certificates.configuration.X509CertificateValidationResult;
import ch.post.it.evoting.cryptolib.certificates.configuration.X509CertificateValidationType;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;

/**
 * Tests of X509 certificate validator for negative cases.
 */
class X509CertificateValidatorNegativeTest {

	private static KeyPair rootKeyPair;

	private static CryptoX509Certificate rootCertificate;

	private static X509DistinguishedName rootSubjectDn;

	private static Date validDate;

	private static KeyPair keyPair;

	private static X509DistinguishedName subjectDn;

	private static Date invalidDate;

	private static X509CertificateValidationType[] validationTypes;

	@BeforeAll
	public static void setUp() throws GeneralCryptoLibException {

		rootKeyPair = AsymmetricTestDataGenerator.getKeyPairForSigning();
		rootCertificate = (CryptoX509Certificate) X509CertificateTestDataGenerator.getRootAuthorityX509Certificate(rootKeyPair);
		rootSubjectDn = X509CertificateTestDataGenerator.getRootDistinguishedName();
		validDate = X509CertificateTestDataGenerator.getDateWithinValidityPeriod();

		keyPair = AsymmetricTestDataGenerator.getUniqueKeyPairForSigning(rootKeyPair);
		subjectDn = X509CertificateTestDataGenerator.getDistinguishedName();
		invalidDate = X509CertificateTestDataGenerator.getDateOutsideValidityPeriod();

		validationTypes = new X509CertificateValidationType[5];
		validationTypes[0] = X509CertificateValidationType.DATE;
		validationTypes[1] = X509CertificateValidationType.SUBJECT;
		validationTypes[2] = X509CertificateValidationType.ISSUER;
		validationTypes[3] = X509CertificateValidationType.SIGNATURE;
		validationTypes[4] = X509CertificateValidationType.KEY_TYPE;

		X509CertificateTestDataGenerator.getDateWithinValidityPeriod();
	}

	@Test
	void checkWhenDateInvalidThenCertificateValidationFails() throws GeneralCryptoLibException {

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addDate(invalidDate).addSubjectDn(rootSubjectDn)
				.addIssuerDn(rootSubjectDn).addKeyType(X509CertificateType.CERTIFICATE_AUTHORITY).addCaPublicKey(rootKeyPair.getPublic()).build();

		X509CertificateValidator validator = new X509CertificateValidator(rootCertificate, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		int numFailedValidations = validationResult.getFailedValidationTypes().size();
		X509CertificateValidationType failedValidationType = validationResult.getFailedValidationTypes().get(0);

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(1, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertEquals(X509CertificateValidationType.DATE, failedValidationType, "Unexpected failed validation type");
	}

	@Test
	void checkWhenSubjectDnInvalidThenCertificateValidationFails() throws GeneralCryptoLibException {

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addDate(validDate).addSubjectDn(subjectDn)
				.addIssuerDn(rootSubjectDn).addKeyType(X509CertificateType.CERTIFICATE_AUTHORITY).addCaPublicKey(rootKeyPair.getPublic()).build();

		X509CertificateValidator validator = new X509CertificateValidator(rootCertificate, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		int numFailedValidations = validationResult.getFailedValidationTypes().size();
		X509CertificateValidationType failedValidationType = validationResult.getFailedValidationTypes().get(0);

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(1, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertEquals(X509CertificateValidationType.SUBJECT, failedValidationType, "Unexpected failed validation type");
	}

	@Test
	void checkWhenIssuerDnInvalidThenCertificateValidationFails() throws GeneralCryptoLibException {

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addDate(validDate).addSubjectDn(rootSubjectDn)
				.addIssuerDn(subjectDn).addKeyType(X509CertificateType.CERTIFICATE_AUTHORITY).addCaPublicKey(rootKeyPair.getPublic()).build();

		X509CertificateValidator validator = new X509CertificateValidator(rootCertificate, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		int numFailedValidations = validationResult.getFailedValidationTypes().size();
		X509CertificateValidationType failedValidationType = validationResult.getFailedValidationTypes().get(0);

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(1, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertEquals(X509CertificateValidationType.ISSUER, failedValidationType, "Unexpected failed validation type");
	}

	@Test
	void checkWhenCaPublicKeyInvalidThenCertificateValidationFails() throws GeneralCryptoLibException {

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addDate(validDate).addSubjectDn(rootSubjectDn)
				.addIssuerDn(rootSubjectDn).addKeyType(X509CertificateType.CERTIFICATE_AUTHORITY).addCaPublicKey(keyPair.getPublic()).build();

		X509CertificateValidator validator = new X509CertificateValidator(rootCertificate, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		int numFailedValidations = validationResult.getFailedValidationTypes().size();
		X509CertificateValidationType failedValidationType = validationResult.getFailedValidationTypes().get(0);

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(1, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertEquals(X509CertificateValidationType.SIGNATURE, failedValidationType, "Unexpected failed validation type");
	}

	@Test
	void checkWhenKeyUsageInvalidThenCertificateValidationFails() throws GeneralCryptoLibException {

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addDate(validDate).addSubjectDn(rootSubjectDn)
				.addIssuerDn(rootSubjectDn).addKeyType(X509CertificateType.SIGN).addCaPublicKey(rootKeyPair.getPublic()).build();

		X509CertificateValidator validator = new X509CertificateValidator(rootCertificate, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		int numFailedValidations = validationResult.getFailedValidationTypes().size();
		X509CertificateValidationType failedValidationType = validationResult.getFailedValidationTypes().get(0);

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(1, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertEquals(X509CertificateValidationType.KEY_TYPE, failedValidationType, "Unexpected failed validation type");
	}

	@Test
	void checkWhenKeyUsageNotSetThenCertificateValidationFails() throws GeneralCryptoLibException {

		CertificateData certificateData = X509CertificateTestDataGenerator.getCertificateData(keyPair);

		CryptoX509Certificate certificateWithNoKeyUsage = (CryptoX509Certificate) X509CertificateTestDataGenerator
				.getX509CertificateWithNoKeyUsage(certificateData, rootKeyPair);

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addKeyType(X509CertificateType.SIGN).build();

		X509CertificateValidationType[] validationTypes = new X509CertificateValidationType[1];
		validationTypes[0] = X509CertificateValidationType.KEY_TYPE;

		X509CertificateValidator validator = new X509CertificateValidator(certificateWithNoKeyUsage, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		int numFailedValidations = validationResult.getFailedValidationTypes().size();
		X509CertificateValidationType failedValidationType = validationResult.getFailedValidationTypes().get(0);

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(1, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertEquals(X509CertificateValidationType.KEY_TYPE, failedValidationType, "Unexpected failed validation type");
	}

	@Test
	void checkWhenKeyUsageNotSetThenCertificateAuthorityCertificateValidationFails() throws GeneralCryptoLibException {

		CertificateData certificateData = X509CertificateTestDataGenerator.getCertificateData(rootKeyPair);

		CryptoX509Certificate certificateWithNoKeyUsage = (CryptoX509Certificate) X509CertificateTestDataGenerator
				.getCertificateAuthorityX509CertificateWithNoKeyUsage(certificateData, rootKeyPair);

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addKeyType(X509CertificateType.SIGN).build();

		X509CertificateValidationType[] validationTypes = new X509CertificateValidationType[1];
		validationTypes[0] = X509CertificateValidationType.KEY_TYPE;

		X509CertificateValidator validator = new X509CertificateValidator(certificateWithNoKeyUsage, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		int numFailedValidations = validationResult.getFailedValidationTypes().size();
		X509CertificateValidationType failedValidationType = validationResult.getFailedValidationTypes().get(0);

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(1, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertEquals(X509CertificateValidationType.KEY_TYPE, failedValidationType, "Unexpected failed validation type");
	}

	@Test
	void checkWhenAllDataIsInvalidThenAllCertificateValidationFails() throws GeneralCryptoLibException {

		X509CertificateValidationData validationData = new X509CertificateValidationData.Builder().addDate(invalidDate).addSubjectDn(subjectDn)
				.addIssuerDn(subjectDn).addKeyType(X509CertificateType.SIGN).addCaPublicKey(keyPair.getPublic()).build();

		X509CertificateValidator validator = new X509CertificateValidator(rootCertificate, validationData, validationTypes);

		X509CertificateValidationResult validationResult = validator.validate();

		boolean isValidated = validationResult.isValidated();
		List<X509CertificateValidationType> failedValidationTypes = validationResult.getFailedValidationTypes();
		int numFailedValidations = failedValidationTypes.size();

		Assertions.assertFalse(isValidated, "Unexpected validation result. expected:<failure> but was:<success>");
		Assertions.assertEquals(validationTypes.length, numFailedValidations, "Unexpected number of failed validations.");
		Assertions.assertTrue(failedValidationTypes.contains(X509CertificateValidationType.DATE),
				"Could not find expected failed validation type <DATE>");
		Assertions.assertTrue(failedValidationTypes.contains(X509CertificateValidationType.SUBJECT),
				"Could not find expected failed validation type <SUBJECT>");
		Assertions.assertTrue(failedValidationTypes.contains(X509CertificateValidationType.ISSUER),
				"Could not find expected failed validation type <ISSUER>");
		Assertions.assertTrue(failedValidationTypes.contains(X509CertificateValidationType.SIGNATURE),
				"Could not find expected failed validation type <SIGNATURE>");
		Assertions.assertTrue(failedValidationTypes.contains(X509CertificateValidationType.KEY_TYPE),
				"Could not find expected failed validation type <KEY_TYPE>");
	}
}
