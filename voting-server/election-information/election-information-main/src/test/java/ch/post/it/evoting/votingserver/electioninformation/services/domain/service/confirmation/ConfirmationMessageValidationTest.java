/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import ch.post.it.evoting.domain.election.model.Information.VoterInformation;
import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.model.confirmation.ConfirmationMessage;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformationRepository;

/**
 * Test for encrypted options rule.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfirmationMessageValidationTest {

	@InjectMocks
	private final ConfirmationMessageMathematicalGroupValidation mgv = new ConfirmationMessageMathematicalGroupValidation();
	@Mock
	BallotBoxInformationRepository repository;
	@Mock
	private Logger LOGGER;

	@Test
	public void mathematical_group_validation_fails_on_all_zeros_key() throws Exception {

		when(repository.findByTenantIdElectionEventIdBallotBoxId(any(), any(), any())).thenReturn(fakeBallotBoxInformation());

		final ValidationError result = mgv
				.execute("100", "dbe163434b654a2a9565057d5f555dfe", "3123123", fakeConfirmationInformation(toBase64("000000000")),
						fakeAuthenticationToken());

		assertNotEquals(result.getValidationErrorType(), ValidationErrorType.SUCCESS);
	}

	@Test
	public void mathematical_group_validation_fails_on_invalid_key() throws Exception {

		when(repository.findByTenantIdElectionEventIdBallotBoxId(any(), any(), any())).thenReturn(fakeBallotBoxInformation());

		final ValidationError result = mgv
				.execute("100", "dbe163434b654a2a9565057d5f555dfe", "3123123", fakeConfirmationInformation(toBase64("00000000137")),
						fakeAuthenticationToken());

		assertNotEquals(result.getValidationErrorType(), ValidationErrorType.SUCCESS);
	}

	@Test
	public void mathematical_group_validation_fails_on_invalid_number_key() throws Exception {

		when(repository.findByTenantIdElectionEventIdBallotBoxId(any(), any(), any())).thenReturn(fakeBallotBoxInformation());

		final ValidationError result = mgv.execute("100", "dbe163434b654a2a9565057d5f555dfe", "3123123", fakeConfirmationInformation(toBase64(";3")),
				fakeAuthenticationToken());

		assertNotEquals(result.getValidationErrorType(), ValidationErrorType.SUCCESS);
	}

	@Test(expected = RuntimeException.class)
	public void mathematical_group_validation_throws_on_invalid_db_data() throws Exception {

		when(repository.findByTenantIdElectionEventIdBallotBoxId(any(), any(), any()))
				.thenReturn(fakeBallotBoxInformationWithInvalidEncryptionParameters());

		final ValidationError result = mgv
				.execute("100", "dbe163434b654a2a9565057d5f555dfe", "3123123", fakeConfirmationInformation(toBase64("0000001")),
						fakeAuthenticationToken());

		assertNotEquals(result.getValidationErrorType(), ValidationErrorType.SUCCESS);
	}

	private AuthenticationToken fakeAuthenticationToken() {
		AuthenticationToken token = new AuthenticationToken();
		VoterInformation voterInfo = new VoterInformation();
		voterInfo.setBallotBoxId("e20bfdb00cc0427698a9578099e54ca3");
		token.setVoterInformation(voterInfo);
		return token;
	}

	private ConfirmationInformation fakeConfirmationInformation(String key) {
		ConfirmationInformation information = new ConfirmationInformation();
		ConfirmationMessage message = new ConfirmationMessage();
		message.setConfirmationKey(key);
		information.setConfirmationMessage(message);
		return information;

	}

	private BallotBoxInformation fakeBallotBoxInformation() {
		BallotBoxInformation ballotBoxInformation = new BallotBoxInformation();
		ballotBoxInformation.setJson(
				"{\"eeid\":\"dbe163434b654a2a9565057d5f555dfe\",\"startDate\":\"2016-10-27T08:00Z\",\"endDate\":\"2017-04-01T20:00Z\",\"encryptionParameters\":{\"p\":\"16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759\",\"q\":\"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379\",\"g\":\"2\"},\"test\":true,\"gracePeriod\":\"600\",\"bid\":\"5e4ea1721f5f4699a5373a5da017f4bf\",\"ballotBoxCert\":\"-----BEGIN CERTIFICATE-----\\nMIIDozCCAougAwIBAgIVAIwT1SJ8QzQth7ldCOejJVRgD77TMA0GCSqGSIb3DQEB\\nCwUAMHsxNTAzBgNVBAMMLFNlcnZpY2VzIENBIGRiZTE2MzQzNGI2NTRhMmE5NTY1\\nMDU3ZDVmNTU1ZGZlMRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlT\\nd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcNMTYxMTMwMTEwMDIx\\nWhcNMTgwNDAxMjAwMDAwWjB5MTMwMQYDVQQDDCpCYWxsb3RCb3ggZTIwYmZkYjAw\\nY2MwNDI3Njk4YTk1NzgwOTllNTRjYTMxFjAUBgNVBAsMDU9ubGluZSBWb3Rpbmcx\\nEjAQBgNVBAoMCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDCCASIw\\nDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIn6JkZ9g0PFU3ButD71xUS65yv1\\nXJPAlZ7RkqlJHGhdfhiSHDnL8lECI0CCmUqTINzDhGvglngtd7KIcgGRftjT0NIK\\n993JI/8jaswu77N6KX4EVk0j5zfxJNz6FPIfOgeXtHam6BJeq2ph1r29GDm6P6HS\\n0EbBWAPQhnlDsHvCl+ofz1jrwXJB01mX9PKzbCwMvgE2k+tHmjEH2ooBJdryyp/J\\nOVPgOh4353g6CxgBOIm3Z4U/aK/0f5nFbW5nMYhEK3bgjCU4PNKKBJZgJZxEfGF6\\nfTVus7bC8LOV79OnCRbPFLngZDxyfj7kUOd3rBBgpl2a3NJVvMhL+QT3slUCAwEA\\nAaMgMB4wDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBsAwDQYJKoZIhvcNAQEL\\nBQADggEBADyYjYxTrNvoFn0HwXiCoUHrnucauey5kk8wvhbeDFLq4eMaEQagPFoC\\nNwo0ERA5l4/hJzYyVlfmGwXx2n2aoe2wgcosMAcDVmMrPIaS4eFt9lN82J5/+KBK\\nh4V68tyBcqE1nUK80rvmpCRajH4x3D7GSalW0RtDC4EGiMYNkPSlZXDJyBrLdIHc\\nW9b/hgfcaQQQ11CSEC1VPt94yMKrcDOO5J21L69HSTfY4Fl1wyZez7uJKF7fPPGO\\nyKwRPJS2KVC2P7jsAmAFodHGmuYKGnEQTkYd7zrPzkbqfE5dWAnn7PnXvMKDAHqW\\nzub8xRSFdkMBIW6igbNUIIrARed7FmM=\\n-----END CERTIFICATE-----\\n\",\"electoralAuthorityId\":\"46ec4ea7c32449b58562f0e8fb7452e8\",\"id\":\"e20bfdb00cc0427698a9578099e54ca3\"}");
		return ballotBoxInformation;
	}

	private BallotBoxInformation fakeBallotBoxInformationWithInvalidEncryptionParameters() {
		BallotBoxInformation ballotBoxInformation = new BallotBoxInformation();
		ballotBoxInformation.setJson(
				"{\"eeid\":\"dbe163434b654a2a9565057d5f555dfe\",\"startDate\":\"2016-10-27T08:00Z\",\"endDate\":\"2017-04-01T20:00Z\",\"encryptionParameters\":{\"p\":\"a;123\",\"q\":\"b%123123\",\"g\":\"2\"},\"test\":true,\"gracePeriod\":\"600\",\"bid\":\"5e4ea1721f5f4699a5373a5da017f4bf\",\"ballotBoxCert\":\"-----BEGIN CERTIFICATE-----\\nMIIDozCCAougAwIBAgIVAIwT1SJ8QzQth7ldCOejJVRgD77TMA0GCSqGSIb3DQEB\\nCwUAMHsxNTAzBgNVBAMMLFNlcnZpY2VzIENBIGRiZTE2MzQzNGI2NTRhMmE5NTY1\\nMDU3ZDVmNTU1ZGZlMRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlT\\nd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcNMTYxMTMwMTEwMDIx\\nWhcNMTgwNDAxMjAwMDAwWjB5MTMwMQYDVQQDDCpCYWxsb3RCb3ggZTIwYmZkYjAw\\nY2MwNDI3Njk4YTk1NzgwOTllNTRjYTMxFjAUBgNVBAsMDU9ubGluZSBWb3Rpbmcx\\nEjAQBgNVBAoMCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDCCASIw\\nDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIn6JkZ9g0PFU3ButD71xUS65yv1\\nXJPAlZ7RkqlJHGhdfhiSHDnL8lECI0CCmUqTINzDhGvglngtd7KIcgGRftjT0NIK\\n993JI/8jaswu77N6KX4EVk0j5zfxJNz6FPIfOgeXtHam6BJeq2ph1r29GDm6P6HS\\n0EbBWAPQhnlDsHvCl+ofz1jrwXJB01mX9PKzbCwMvgE2k+tHmjEH2ooBJdryyp/J\\nOVPgOh4353g6CxgBOIm3Z4U/aK/0f5nFbW5nMYhEK3bgjCU4PNKKBJZgJZxEfGF6\\nfTVus7bC8LOV79OnCRbPFLngZDxyfj7kUOd3rBBgpl2a3NJVvMhL+QT3slUCAwEA\\nAaMgMB4wDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBsAwDQYJKoZIhvcNAQEL\\nBQADggEBADyYjYxTrNvoFn0HwXiCoUHrnucauey5kk8wvhbeDFLq4eMaEQagPFoC\\nNwo0ERA5l4/hJzYyVlfmGwXx2n2aoe2wgcosMAcDVmMrPIaS4eFt9lN82J5/+KBK\\nh4V68tyBcqE1nUK80rvmpCRajH4x3D7GSalW0RtDC4EGiMYNkPSlZXDJyBrLdIHc\\nW9b/hgfcaQQQ11CSEC1VPt94yMKrcDOO5J21L69HSTfY4Fl1wyZez7uJKF7fPPGO\\nyKwRPJS2KVC2P7jsAmAFodHGmuYKGnEQTkYd7zrPzkbqfE5dWAnn7PnXvMKDAHqW\\nzub8xRSFdkMBIW6igbNUIIrARed7FmM=\\n-----END CERTIFICATE-----\\n\",\"electoralAuthorityId\":\"46ec4ea7c32449b58562f0e8fb7452e8\",\"id\":\"e20bfdb00cc0427698a9578099e54ca3\"}");
		return ballotBoxInformation;
	}

	private String toBase64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}
}
