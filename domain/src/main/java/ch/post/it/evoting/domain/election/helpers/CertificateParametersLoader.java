/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.helpers;

import java.util.Properties;

import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;

/**
 * Retrieves certificate properties from a given properties input.
 */
public class CertificateParametersLoader {

	private static final String ISSUER_COMMON_NAME_PROPERTY_NAME = "issuer.common.name";

	private static final String ISSUER_ORGANIZATIONAL_UNIT_PROPERTY_NAME = "issuer.organizational.unit";

	private static final String ISSUER_ORGANIZATION_PROPERTY_NAME = "issuer.organization";

	private static final String ISSUER_COUNTRY_PROPERTY_NAME = "issuer.country";

	private static final String SUBJECT_COMMON_NAME_PROPERTY_NAME = "subject.common.name";

	private static final String SUBJECT_ORGANIZATIONAL_UNIT_PROPERTY_NAME = "subject.organizational.unit";

	private static final String SUBJECT_ORGANIZATION_PROPERTY_NAME = "subject.organization";

	private static final String SUBJECT_COUNTRY_PROPERTY_NAME = "subject.country";

	private final ReplacementsHolder replacements;

	public CertificateParametersLoader(final ReplacementsHolder replacements) {
		this.replacements = replacements;
	}

	public CertificateParameters load(final Properties properties, final CertificateParameters.Type type) {

		CertificateParameters certificateParameters = new CertificateParameters();

		certificateParameters.setType(type);
		certificateParameters.setUserSubjectCn(getProperty(properties, SUBJECT_COMMON_NAME_PROPERTY_NAME));
		certificateParameters.setUserSubjectOrgUnit(getProperty(properties, SUBJECT_ORGANIZATIONAL_UNIT_PROPERTY_NAME));
		certificateParameters.setUserSubjectOrg(getProperty(properties, SUBJECT_ORGANIZATION_PROPERTY_NAME));
		certificateParameters.setUserSubjectCountry(getProperty(properties, SUBJECT_COUNTRY_PROPERTY_NAME));
		if (certificateParameters.getType() != CertificateParameters.Type.ROOT) {
			certificateParameters.setUserIssuerCn(getProperty(properties, ISSUER_COMMON_NAME_PROPERTY_NAME));
			certificateParameters.setUserIssuerOrgUnit(getProperty(properties, ISSUER_ORGANIZATIONAL_UNIT_PROPERTY_NAME));
			certificateParameters.setUserIssuerOrg(getProperty(properties, ISSUER_ORGANIZATION_PROPERTY_NAME));
			certificateParameters.setUserIssuerCountry(getProperty(properties, ISSUER_COUNTRY_PROPERTY_NAME));
		}
		return certificateParameters;
	}

	private String getProperty(final Properties properties, final String name) {
		String value = properties.getProperty(name);
		return replacements.applyReplacements(value);
	}
}
