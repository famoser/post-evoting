/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class VerifyVerificationCardPKSignatureRuleTest {

	@InjectMocks
	private final VerifyVerificationCardPKSignatureRule verifyVerificationCardPKSignatureRule = new VerifyVerificationCardPKSignatureRule();

	private final String verificationCardPublicKey = "eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJwIjoiMTYzNzA1MTg5OTQzMTk1ODY3NjAzMTk3OTE1MjYyOTM1MzUzMjc1NzY0Mzg2NDY3ODIxMzk0MTk4NDYwMDQxODA4MzcxMDM1MjcxMjkwMzU5NTQ3NDIwNDM1OTA2MDk0MjEzNjk2NjU5NDQ3NDY1ODc4ODU4MTQ5MjA4NTE2OTQ1NDY0NTY4OTE3Njc2NDQ5NDU0NTkxMjQ0MjI1NTM3NjM0MTY1ODY1MTUzMzk5NzgwMTQxNTQ0NTIxNTk2ODcxMDkxNjEwOTA2MzUzNjc2MDAzNDkyNjQ5MzQ5MjQxNDE3NDYwODIwNjAzNTM0ODMzMDY4NTUzNTIxOTIzNTg3MzI0NTE5NTUyMzIwMDA1OTM3Nzc1NTQ0MzE3OTg5ODE1NzQ1Mjk4NTQzMTQ2NTEwOTIwODY0ODg0MjYzOTA3NzY4MTEzNjcxMjUwMDk1NTEzNDYwODkzMTkzMTUxMTE1MDkyNzczNDcxMTc0NjcxMDc5MTQwNzM2Mzk0NTY4MDUxNTkwOTQ1NjI1OTM5NTQxOTU5NjA1MzExMzYwNTIyMDgwMTkzNDMzOTI5MDY4MTYwMDEwMTc0ODgwNTEzNjY1MTgxMjI0MDQ4MTk5NjcyMDQ2MDE0MjczMDQyNjczODAyMzgyNjM5MTM4OTI2NTg5NTAyODE1OTM3NTU4OTQ3NDczMzkxMjY1MzEwMTgwMjY3OTg5ODI3ODUzMzEwNzkwNjUxMjYzNzU0NTUyOTM0MDkwNjU1NDA3MzE2NDY5Mzk4MDg2NDAyNzMzOTM4NTUyNTYyMzA4MjA1MDkyMTc0MTE1MTAwNTg3NTkiLCJxIjoiODE4NTI1OTQ5NzE1OTc5MzM4MDE1OTg5NTc2MzE0Njc2NzY2Mzc4ODIxOTMyMzM5MTA2OTcwOTkyMzAwMjA5MDQxODU1MTc2MzU2NDUxNzk3NzM3MTAyMTc5NTMwNDcxMDY4NDgzMjk3MjM3MzI5Mzk0MjkwNzQ2MDQyNTg0NzI3MzIyODQ0NTg4MzgyMjQ3MjcyOTU2MjIxMTI3Njg4MTcwODI5MzI1NzY2OTk4OTAwNzA3NzIyNjA3OTg0MzU1NDU4MDU0NTMxNzY4MzgwMDE3NDYzMjQ2NzQ2MjA3MDg3MzA0MTAzMDE3Njc0MTY1MzQyNzY3NjA5NjE3OTM2NjIyNTk3NzYxNjAwMDI5Njg4ODc3NzIxNTg5OTQ5MDc4NzI2NDkyNzE1NzMyNTU0NjA0MzI0NDIxMzE5NTM4ODQwNTY4MzU2MjUwNDc3NTY3MzA0NDY1OTY1NzU1NTc1NDYzODY3MzU1ODczMzU1Mzk1NzAzNjgxOTcyODQwMjU3OTU0NzI4MTI5Njk3NzA5Nzk4MDI2NTU2ODAyNjEwNDAwOTY3MTY5NjQ1MzQwODAwMDUwODc0NDAyNTY4MzI1OTA2MTIwMjQwOTk4MzYwMjMwMDcxMzY1MjEzMzY5MDExOTEzMTk1Njk0NjMyOTQ3NTE0MDc5Njg3Nzk0NzM3MzY2OTU2MzI2NTUwOTAxMzM5OTQ5MTM5MjY2NTUzOTUzMjU2MzE4NzcyNzY0NjcwNDUzMjc3MDM2NTgyMzQ2OTkwNDMyMDEzNjY5NjkyNzYyODExNTQxMDI1NDYwODcwNTc1NTAyOTM3OSIsImciOiIyIn0sImVsZW1lbnRzIjpbIjg3NTc1MTg4NjUxODMyMzk0NzA3MDk1NDI0MDEwNDcyODU2MzIyODYwMDYyNTM0Njc3ODI4ODAyMjA2NzQ0NTIzNDg0MDA2MzgzNTUwMDE4NzIyNjQzNzkzOTY2MzYyNzkwNDk0NzI5MjkyMTU1Njc4NTIzNDM4NDkwNTAzOTI5NTcyODY5OTA3ODY5NzM4MzMwODM3MDM5NTM3MjgwNTg3ODc0MDA3Mzc3OTgxMTIzMDk3OTE2NjMxOTU0ODMyNTI1MjQ4NjY5OTAzMjQwODQzMzA1Mzg1OTE1OTg3NDU4OTEzMTkyNjUyODEzNTM5MDQxMzM2ODkzMzE2ODM0ODEyNTAyMjg1MTgwMTMyNzQ5Nzc4MTc2Mzk2NDU5Mjg3Mjg0MTU4NTAwMjUxNzIyMjkxNzY0MDQzNTg3NzcyMDUxNDQ2NjU0MTM2ODE1MDI5NDc2MDk1MDAwMzMzODk3OTg2NTUyOTgzNzI4OTM5ODA3MjMwOTk5NDE2MzI1ODkxMTA3MDkzNjc5ODk5MjgyOTk3Nzc3ODEzNjUyNzcyMDY1OTg5ODU5OTQ3ODMzNDQwMjY0OTk0MTA4ODcxMTk2NDI0NjQ0MzY3OTY4OTkxOTEyOTU1MTM3OTM5NzA0ODA1NzM2NjgxMzY1MTgyNzUyMzAwOTE3OTM2NDI5MzA0MDA4OTY1NTIzOTE5NzI1MTY5MTQ0OTkxMjEzMTkzMzE3Mjc4OTQ5MTQ5OTc3MTI3MzE3MDEwNjgyOTQxMjE5ODQ1ODQ0MTk4MjM5ODA5NzI2MjE0MTcxNTgxMzE0NDQ2MjYzMjg4NDY0MjEiXX19";
	private final String verificationCardPKSignature = "OZY7wYTLmeWNhp/xAwTZPO8HKu4giRxB5y1Q9jlVvZNH2BmjQOQbz38D5bMUZbj8ILE04tKzNelEMa5vJdDTaQAzsnRxc295HTvWGTH0sPd6brXK2QPVoJeoXQBz0b2RETgFsC/Goya5qMwZ3aRfH7FSKghnXlFHEM6EY+F3GacvftppQfgHMTcC7ABGAeE5IuikL3SlqsKvhnF7UTtwc8YSnuHpQGSNhfEed9G+fn4Tws6x/AQHjcwsT3Ryq02L0b598g7XJjyXdlbvLQ7OY1M1x1iRRzIkrC4yn6dqTfzc1bw1beDxfhHKqDGb3MKsR8O90PAAHsaVCJFWKJOJGg==";
	private final String verificationDataSetInvalidJson = "{\"electionEventId\":\"1d942d2f7ee24150af6ab979d3c43c59\",\"verificationCardSetIssuerCert\":\"-----BEGIN CERTIFICATE-----\nMIIDsTCCApmgAwIBAgIVAPc+RhmguzzCd2/+axuAjpGAWpbJMA0GCSqGSIb3DQEB\nCwUAMHsxNTAzBgNVBAMMLFNlcnZpY2VzIENBIDFkOTQyZDJmN2VlMjQxNTBhZjZh\nYjk3OWQzYzQzYzU5MRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlT\nd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcNMTUxMjIyMTQwNzM0\nWhcNMTYxMjEwMTAwMDAwWjCBhjFAMD4GA1UEAww3VmVyaWZpY2F0aW9uQ2FyZElz\nc3VlciAxZDk0MmQyZjdlZTI0MTUwYWY2YWI5NzlkM2M0M2M1OTEWMBQGA1UECwwN\nT25saW5lIFZvdGluZzESMBAGA1UECgwJU3dpc3NQb3N0MQkwBwYDVQQHDAAxCzAJ\nBgNVBAYTAkNIMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhQj73Ozc\nC9YlUVElc9YnowOpTr7ekHNeWNZQIhWIQojfJTMuhGl9ZZx4rFk7LPVMMdt0Nt/6\nQ70RIhKb3XPpe4j6pO+rLS1xQsdRvFwydZivgM2A3qKNIiRjtJqRE6jyTHasaQ2G\nX5RVcjFGJ+Rt2shvMP+bUqmbege6ukGXRbDKFwm7v6IBTqnd06o2WwsbMB+jGPW1\nCJP6bwQv3pPwGiZbrYE9Onft5XbsqYqkf1r1PQnPzQmIsRg1Q7GFSgGy0g/F9FkH\ndZbN5tBeT3DFywg1BRUNNYcx07nXJ3kJSB6UUzA5+x1lQRTp8gnEbxPd1iH4hWns\nQda6TrvnqD9WAQIDAQABoyAwHjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIG\nwDANBgkqhkiG9w0BAQsFAAOCAQEAbhyR3xy7ZRE20y4u60XbJVDET4NbEcLQejkn\nzqdZ5ebUELvwDgs+JwaHJQfQ5yjxVb4dilR3Eq//nWCRQ30iLFvFSzZ94xtFRVFA\nH9hw2MDGFVLUprP6wFEpptQoYy8/sVRWxQcOjWmM7TSyg53ljzTZVceEp09++mf3\n1NsAveU/UquAIUvwDm+Qw+U64ClGMpJqFqMebJ+Fomry3yaIxCI4ToGgcw22c8E3\nrJ9nowpJzK4BahRfIOxHe/YtF6/97HUXbQ3z+JztvNT0nrz/wsPrgTQ4eblPQjH1\n9QJxxKQoPJWUqvnSbaH26vb4ctms4Ihh8quh39TaiWjp6LHymw==\n-----END CERTIFICATE-----\n\",\"verificationCardSetId\":\"4e84749ff7a64f1cbb97a4ca26fb9298\",\"choicesCodesEncryptionPublicKey\":\"eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJwIjoiMTYzNzA1MTg5OTQzMTk1ODY3NjAzMTk3OTE1MjYyOTM1MzUzMjc1NzY0Mzg2NDY3ODIxMzk0MTk4NDYwMDQxODA4MzcxMDM1MjcxMjkwMzU5NTQ3NDIwNDM1OTA2MDk0MjEzNjk2NjU5NDQ3NDY1ODc4ODU4MTQ5MjA4NTE2OTQ1NDY0NTY4OTE3Njc2NDQ5NDU0NTkxMjQ0MjI1NTM3NjM0MTY1ODY1MTUzMzk5NzgwMTQxNTQ0NTIxNTk2ODcxMDkxNjEwOTA2MzUzNjc2MDAzNDkyNjQ5MzQ5MjQxNDE3NDYwODIwNjAzNTM0ODMzMDY4NTUzNTIxOTIzNTg3MzI0NTE5NTUyMzIwMDA1OTM3Nzc1NTQ0MzE3OTg5ODE1NzQ1Mjk4NTQzMTQ2NTEwOTIwODY0ODg0MjYzOTA3NzY4MTEzNjcxMjUwMDk1NTEzNDYwODkzMTkzMTUxMTE1MDkyNzczNDcxMTc0NjcxMDc5MTQwNzM2Mzk0NTY4MDUxNTkwOTQ1NjI1OTM5NTQxOTU5NjA1MzExMzYwNTIyMDgwMTkzNDMzOTI5MDY4MTYwMDEwMTc0ODgwNTEzNjY1MTgxMjI0MDQ4MTk5NjcyMDQ2MDE0MjczMDQyNjczODAyMzgyNjM5MTM4OTI2NTg5NTAyODE1OTM3NTU4OTQ3NDczMzkxMjY1MzEwMTgwMjY3OTg5ODI3ODUzMzEwNzkwNjUxMjYzNzU0NTUyOTM0MDkwNjU1NDA3MzE2NDY5Mzk4MDg2NDAyNzMzOTM4NTUyNTYyMzA4MjA1MDkyMTc0MTE1MTAwNTg3NTkiLCJxIjoiODE4NTI1OTQ5NzE1OTc5MzM4MDE1OTg5NTc2MzE0Njc2NzY2Mzc4ODIxOTMyMzM5MTA2OTcwOTkyMzAwMjA5MDQxODU1MTc2MzU2NDUxNzk3NzM3MTAyMTc5NTMwNDcxMDY4NDgzMjk3MjM3MzI5Mzk0MjkwNzQ2MDQyNTg0NzI3MzIyODQ0NTg4MzgyMjQ3MjcyOTU2MjIxMTI3Njg4MTcwODI5MzI1NzY2OTk4OTAwNzA3NzIyNjA3OTg0MzU1NDU4MDU0NTMxNzY4MzgwMDE3NDYzMjQ2NzQ2MjA3MDg3MzA0MTAzMDE3Njc0MTY1MzQyNzY3NjA5NjE3OTM2NjIyNTk3NzYxNjAwMDI5Njg4ODc3NzIxNTg5OTQ5MDc4NzI2NDkyNzE1NzMyNTU0NjA0MzI0NDIxMzE5NTM4ODQwNTY4MzU2MjUwNDc3NTY3MzA0NDY1OTY1NzU1NTc1NDYzODY3MzU1ODczMzU1Mzk1NzAzNjgxOTcyODQwMjU3OTU0NzI4MTI5Njk3NzA5Nzk4MDI2NTU2ODAyNjEwNDAwOTY3MTY5NjQ1MzQwODAwMDUwODc0NDAyNTY4MzI1OTA2MTIwMjQwOTk4MzYwMjMwMDcxMzY1MjEzMzY5MDExOTEzMTk1Njk0NjMyOTQ3NTE0MDc5Njg3Nzk0NzM3MzY2OTU2MzI2NTUwOTAxMzM5OTQ5MTM5MjY2NTUzOTUzMjU2MzE4NzcyNzY0NjcwNDUzMjc3MDM2NTgyMzQ2OTkwNDMyMDEzNjY5NjkyNzYyODExNTQxMDI1NDYwODcwNTc1NTAyOTM3OSIsImciOiIyIn0sImVsZW1lbnRzIjpbIjE2MjA0MzExNzM1NDE0NTczMzI3MzUyOTg0ODU1MjcyNjA5NjY3MzExMjMwMzkzMzAzMDg5ODYyOTcyMTg2NzMzMTcwNjU5MzQyNjMxNzYwNTkyNTQxNjcyOTgxNjQzNjAwNDI3NDg0MjU5MzMyNzI2MTgzMzIwODk5NjE4MjYyMTc0NzIyMDUwMzg4NDc3NTA5OTEzOTU5MzQyNDk0NTE1ODYxMzE4OTMzODg3OTUwMzY5MTg2ODkzMjQ1NDk4MzY5MTg0NjI0Njc4MTk1MDY2NTAzODUxMzU4NDUzMzA3ODEzNzAyNzMzNTUzNTA1MTU0NDA2MzM5NDQ1MzUzNDMzOTE4NTk0NDA5ODE1OTMzNzg2Mzg5MjY3MjgxOTc2NTI1MTU5MTE5NzQzODM0NDQ0Njg1NjA2Mzg2NDg2MjM3MDI4Nzk0NjQxMDM5MDgxMDQyNTUxMDcyMDY1NDY2Nzk0OTEzMTEyNDAzNTcxNDYwMDgwODQwNzE5MTY3NzkzMTI4NTkxMDkxMDAxMTc0MDA1MzE1MzA3Njc5MTY4MDMzMTkwNjk4MDIyMzA4ODg3NDI2ODg4MDQ3NTc4OTUxNjI4Mzk3OTIwMzUwMzY3MTYyNzk0MTA4MTgyMTE1MjMwOTE1NDgxMTQ2MDM0NjA3MjQ5NTM0MDk1OTkxNjU5NTkyNDY5NDk2MjY1MDI2MTQ5MTE4MTg3OTU1NDI0MTM5MjcxNDc4NTYyNDk2ODMxOTAyNDU5OTM4NDYxODIxMTE0NzExOTE2MjI4NTU0ODI5MTk2NjU5NjkyMjU1OTQyMjU1NDM2NzM5ODQ0IiwiMTIyODA0NjQ5Mzg1MTEzMjY0ODEyODU1NTE4NTcyMTU5OTI3MjM2ODg2MzcwNTU2NTM4OTczMTEzMzU3MTkyNTE1NDM3MTc1OTYwNDkyMTg2MTQzMzYwNzE3MjY1NTI3NzE4OTkzMjI2ODU3MzE0MTA4MDE1Mjc3MDk2MzA4Mzg1ODE1MTU5ODA3MzQ5ODY1NjQ0MDg2MzE3MjE4MTkwNzE3NDA4MTQ1NTM2NDA3NTAzNTMxODE5MTE4OTM0OTM0NjY2Mjg5ODQ5NTE4OTU3NjY4MDkwNzA1MDc4NjQ4OTk5Mjg1NDQ3NzM3MTY0Mzc4MTI5NjY3MjY0MzA0MjkyNTk1MDM1NDU5MDgxMjEyNjgxNzU0MzQ5OTk1NzAyNDg4NjE0NTQ4NDkzMjkyNTA1ODQxOTAyNDQ1MDQ1MjEwNjM1Mzg1MjM0MTE3OTIzNTE3NzgxNzM2NDI1NDg3NjcxOTkxNzU4NDcwNzA5MzA0ODYwMjA1Njk3NzUyMTUxOTgxNzkwOTg3ODcyMDYyMTc1NTg2MDE1MzIxMzc4NDAzMDM5NDAxMjU1OTYwMTAzMzY3NDA1MzU3MzEwOTI0MzIxNzc3MzM1MDM3MjYyMjY4MTM3OTU4Mzc5Mjc0ODQ4MDcyMDY1Nzc4MjIyMDYxMzE0ODUyMDk2MjA1NDExODU4MDkyMDMyMjAzNTU2MzU2NTQ2NzQzODg3NTUyMDYxMTc4Nzg2NjEzNDIwNjI2MjIxMzgxNjIyNjQ5MTA1Njc2NTczNzk5MDM5NzMwMjUzNTQ5Mzk5ODAxNDU5NTU0ODI1NTI3NDAwMjU4NTYiLCIxNTQ3NTMwNzM5OTQ0MTUyMjUyNzk4MDQ3NjM5OTQzOTM1NTkzMTA0MTcyMzA3Nzg1NDU3MjUzMjU2Mjg4MTY5MDA0NzQ1MjE0MjI2MzQwNTc1MTczNDA3NjE1ODc3MDU2NjQ4MDYzOTI1NjUwMDIxNjUyMTIwMjQzODg5NTQzNDAxMDM5MDkxMDY5MDA1Njc3MTQ5NjE5NTg4Nzk2MzMzODc0MTc3MDE1Nzg4NDY2ODk4MTExNTYwOTM1MDA3OTA2NTM3OTA4MjI3NTA3MzE3MTM1MzYzNjg4NDM4ODA4MDk0OTQwMzMwNDQ3MjU1ODcxOTY3MTQyMTE2MzA3MDMzNjExMjU5ODE4OTM2MTIyMzc4MzY5NTM4MzkxMTEwMjUzNzU0Njg3MzYwMzI2MzQ0MjA1NDcyMjYwNjE1MzE3NjU2MzMwNDM5NTAyMjY0MTU2NTA3MDM2NzU5NDAzNjAxMDA5NDk4NjgwNjkxNzQ0NDUzOTk0MDc4NDI3OTQ0OTA3Nzg5NzY0NDM0NjcyOTI5ODExMTY5OTU0NjM1ODYwNjUwODkxMzYwMDkwMzkxMjMyNjg2Mjk3MzI5ODUwNjcyNjgyNzMxMDU1NzQ2MDkxOTEyNzQ5NTQxNTQzODc3ODg0ODY1MzUzNDExOTUxNzE3NjYxNTQ5MTAyNTI0NjYwMzE4ODE4OTk0Mzg2OTcwOTA0MjkzNTk2NTc5OTU4NDc3MzA2MjUwOTIwOTA3NDMyMDcwMTE2MDEzNTIzMzc4Njg0NDI1NzI4OTcxODg0MDY1MDE4Mjg5Nzg5NDY2MTQ1NDk1Mzg2NjIyMCIsIjg1NDIyOTYwMDE4MjI4NDM2NDg5MzY4NTM3Mjk0Nzk3NTI4NTM2NDY3NjE3ODIwMzM4NjczNTk4ODAyOTI4OTI4NTE4OTE5MTM4ODQyMDA1NTE2OTA5NTcxMjMzNTA3MDg5NTMzNjE4NDY0MjExOTQwNjkxOTcxMTUyODEyMDkwMjE1MzgyMTgzNTYyMjIwMDA2NTg4MDgwMDM0MjQ2NjExOTgyMzIyMzgyNzcyNTIzNjU5MjEwODIxNjAzMDMyNjQ1MzgyMzA1MTk5NzA1OTE2MzEzMzg4NTY4MTkzMjAyNjI1MDQzOTE1Nzg2MDE5NjA0MDk0Mzk4MDU1MDQ2MTY3Nzg1ODY0NzE2OTAyMzU5MDAyODgzMjMwOTU0OTEzODQwNTg0MzUxMDU1MDY0MTg4NTU1MDM3MjM0NTIxOTA4NDM4NzgwNTIxMTQxNDI1MzIyNTg1MjcxMzkwNTg5MTY3NDAxMzg5MDIwNjQzMjM3NzAwODQ2NDk2ODMyNzQ3ODg1OTcxODI2MTE4ODc5MDYyNTc2NzQ4NjgxMzg0ODM2Nzk5NTc1MTg1MzY5ODM4NzA2MzE1MTExNDM2MzIyMjcxNjg1NzM3OTY5NTM3MDE4ODk2Njk0Nzk5MjA0ODUxNzk0NzY3MDkyNDUxMTIwMDA5NjI3MDg3NjMzMjcxNDAxMzkzMDEzNTE4ODczNTQ1ODk0MjY1MTc2MDMyODYwNDIzMjY3NTIyNzk5MzEwMTQ4MDczNTY5ODE0MDAwMzU4OTEyNjY2NTY4NDcyODk0NjkwMzc0NTA4MzA0ODAzNjc3NzkyOTA3NDciLCI0NTI1ODc2ODY1OTQ4NzMyNDE4MzE5ODk5MTU0MzY2MzIxOTMwMjI5NTgxOTIzMjAzMTc3MTYwNDg1ODcwMTYzOTAwNDk2NTcyOTE3NjUyNzU3MTE1MzE0OTY1NDI3MzMzMDY3NTY5NjgzMzEwNDcwOTgwNzQzNTg5MDAxODE5NTY5NTkxNTAyNzMwNjIyOTcxNDU2MDA0ODA2OTk5Mzg3MjU5MTAwOTQ4Mjc2OTQ4NDk1NDE4NTgzMjUyNzg0MzcyNzAyMjI3NjAzNjM5NDI3OTE4MDU2MzAyMDA0NjU5NzA4ODU5NDYwNjAxNDE0MDc3MDI1MjA3NjUyMTA3ODAwNDM5NjI4NDI0OTQzMDIwMDE4NDEwMTI3Mzc5NTI0MTc2NTk5NTk5MzYyMDUxMDM2NTM1MDUwMzQ5NTA5MjAxODk2MTA4Mjk0NTQxNjczNDA5MjA5NTUxOTU1OTg2NTMxOTk2MTUyNzMzMTg3MDIzNjk2MjUzMTQ4MzU0NTQwNzgzOTc0Nzc4NDg3NzkwMzEyNzAxNTkyMzYxNjEyNzUwOTEwMjY0NDk2NjcwMzE3OTEzMzk5NDgyNzc0ODQzMTk5MTgwNTI4MDg4MDA2ODMxMDEyOTU4MDY2MjMxODY1ODczMTMwOTcwNjUxODIzMDg3MTEzNTQ2ODczMTgwODYyNTc0NzA1NTQ1ODUzNDIxMDgwNjY2MzgzMjk0NjA1NDU3OTg4NzY2NjU0MjU3ODY5MzA3NzIyNDk1NzkzNDA2OTQ0NTY0OTAxMDEwOTcyOTAyNTE1NzUzNTc1ODUwMjg3ODY2MDIzMjMxIiwiMTA4NzE3NjEyODY2NTg5NTcwNjkyMjk5MjUxMTc2NTk4NjIxMjkxNDgxNzExMzQ2NzAxNjgwMjM2MTMyMzY5NzEyNjczODY1OTM5NDA0MTI0NzAxMzM3NTYzMjYxNDAzMTY3OTE1Mjk2OTkwMTc2NTE5MzgxODI3MTg5Nzk5MTM4MTUxNTQ4Mzc4MTM0ODc0Nzc3NjMxNDM4NzE5Njg5NDU2MTU2NTcyMzI1NDc2ODYxOTc2NzE5NzkyNzU4MzI5NjQyMjI2MjM4NTg0MzUyNzA2ODkzNDk1Njg3NDA3OTYzNTA0MDg5NTg3NTMxNjExNTA1NzUxMzU1MTEwMDExOTMzMzU1ODA1MTk3MzYxMDQwOTkxNzUzNjMwMzgzMTM5NTA5MjY1MTQ0ODE5MDQxMTI4MjA2NDYwMTEzNDY4NzY2NjI5NDAyNTMyNzEzODg0NDE0OTg3MDk4NTc1MTcwNjQxOTU1OTU5MDM3NDk1MzE3NDk0MTgwNDI4MzYyNDI3MzM1OTE4MzIyODA0MDA2OTg4MTMxODI2MjEzNTY0NDI4NTk0OTMwMDQ4MzAwNTAwOTQ2NjQ4NDgwNjAyNjQwMzkxMTk1NzQ5OTY4NDQyOTUyMTQyNjQ4Mzc0ODA1OTM0NTY4MDIwMDczNDU1OTYzMTU3OTYzNDA4OTg4Njc1NTc3NjYwMzA2MTM2MDcxNDg0Mjg0NDcyODQxNjU4NzgxNzU5MDQ2MDQ5MzQ1NDcxOTU5MjI2NDc3NDE3NDI0MTM2NDgxMTYxODM0OTg2MzA5MTgyNTg5NDQyMTg3MTk5MjY3NzU2Mjc1MjQiLCI1NjE0MDM4NTU2MTIwODA5OTM4NzY2NzA4OTYxNzc2NTcxNDI3MjQ2NDc2Mzk2MTQzODA2NTQwNzk1OTUzNjc4MzMxNDg2NjgyMTI2NjQyMDA3MDkxNTA4NTY1MDMxNzU2NzIwNTQ3OTcyOTg5MDI4ODU4MDExMTI1MTk0OTk5MjA2NzA5MTc3NTkxMTExMjQ4ODcxMDg5OTQ1NzkxMTM0NTE1NTY0MTI4MzIxMDI4NTUyNjQzNDY0NjI0NTI2Mjk3ODcxNzgxMjE4MjMxMjEyNjYwMDQ1Mzg5MjI3NDcxMDQxNjAyNDE5MDcyOTE0OTQzMDk4MDYwMjkwOTg0NzIxMzIyNjA5ODk2MzI0NTI1Njg1MjU5OTUxNTg2NzI0NDU5ODY1OTczNTQ3Mjk2OTUxMzkxMTk0NTYxNDg0MjEzOTQ1MTcwNjEyNzY3MDUyMTEzODcwMjE4NjA1NzI3MDc4NTE4MjA4MTExNzM3MTA0MjQ4NTYwNTE4OTA1NTE2MDc0MDIwNDQ1MTgzMDY4OTE4Nzc5MzQ0Nzg0MTE2NTY1NzA1NjIzOTQ3MjMxNDc3NzE1Mzk0NDg1MDI3NjcxOTk5NzcxNTI1Mjk1Nzk1NTY1MjM1ODQxNTQyNTcyMzI0NDY1NTcxNzQxOTk3NjczMTUwNTk1MTYzNzY1MjIwNTY5NzM5NzEzNzk4MDkyMTQ1MDQ0MjQwMzU2NjI4NTUzMTcwMjg5MDY4MjcxMTc1ODAyNTM4OTIxNDgxNjY3MTMzNjUzMjI2MTYxMjc2ODE2ODExOTIzOTQwMzgwMzAwMDc3MjkwNzk3MTYwIiwiMzc5OTY3NjY4MDkzODU4MzY2MTQ3NTQ1OTA1NDk3MjIxNzE3MjY5ODE1MjY0Mzg5NjM2NDExODQ5MTU3ODI5ODM1NzQ5NDU3Njc5NjYzMDAyNTY5NjM0Njg3NDc5ODA2MDY1NjIzNDQ1MjE1OTAwMzc3Mjc5OTU2MzcwNzgxNzA1NDcwMzU1NTcyMTc5MTM3MzA5NjA5OTU5MzA1NDQ0OTg0MjAxOTU3MTAxNjk5OTkxMTkwMjg5NDg5MzMzNzkwMzc5MDE0OTU2NTk3MzU2MDYwMTQwMTc1NjUxMDIzNzg0ODA2ODY5OTUyNjgxODc1OTI1NjYwNTY3MjU0ODE3MjczMDA2NTUzNDU4MzE2MTAxNTE2MjMxMzM1MTcxNDIwNDcxOTY2NTI0NDE5OTI1ODgyMTQ1Mjc0NzU4MDk0NDAzNzI2NjY3NjI3NDIzNjIzNzE3MDgzNTE3NDgzMzQ1OTk0ODUwNTAzMDE1OTY4NzM2OTA1NzE0MzAyOTM2MTc3MzEzNjYwNDcyODUzODI2MjAzNzU2NDgwOTA5MDI4MjI3NjIzNTY0MzcyMjQzOTE2MDcyNTY2NTAzNzE0MjY2NDQ3Mjc2Mjc5NzE5MjE1Njk4NjIxMTY1MTA2OTgyNDQzNjAzMDQ2MTkwNzkyMDE2MjI1Mjk3NTc3MzYwMDQ3MTE0MTAyNDg0ODQ2NDY2ODc2OTM3NTY4NjUxNDg0NzEyNjQ2Mzk5NzY0Njg4MzYyMTkyMDgwNjA5MzY3NzEzOTA1NjEzNzg0NjQxNjIzMzk3NTgyMTQzNjM0ODQ4NzUzODI0MTI1ODMyOSIsIjEyOTUzNDkwNTQ4MjQ1OTgzMDI0MjczNTk2NzA2NzUyODYzMDQxMjg4NjAzMzYwMjk4MzM1NjAzNTU1NTU3MDc2NDU0OTEwODk0MDc5ODcxOTYwMjM4Mjg3NTQ5OTUxOTgyMzIwMDc2MTAwNDI1MTM4MjUyMTM5MTg2MDQyMjE1NDMzNjc2NjI5MDk1NjQ4MTMzMTUxNDI4NzU0NTkyMzM4MTI3MDk2NzIyMjQ2MTAzMDg1Mjk3MDcyMDY3NzcxNzcyNTE2ODgyMzYzMzMyNjA4Mjc5ODMxODM3NjcxNzgzOTg4MzgyMTcwMjU4Mjg4MDc1ODM5MzQ2NjUyMTE4MDEwODY5ODQ0MDUzOTcwMzM5NTE3NDAxMzg1NjcwMTQ0OTA2NTg5OTg0ODYwNzU1Nzc0MjMyMjc1OTU5MTYxMzQxNzY0NTI4MjQxMTM3NzUzODU2MTE1NTgyMTA0MDk4ODU5MDQzMTY0MjQwMDY3MDg5NjkwMjI5ODc2NDYzNTA2NjkwNTc1MDc2NDMzMDE1MTI2ODA4NjE4MDI3MzgwNTc3MzYxNjEyMzE3MDMxNDExODM0NzYwODI4NDE4OTgyNTI3Mzc4NTEyMjEzMjc0ODE1ODQ5ODEzMzEyOTc2NTI5NTkxNjY3NjIzMDAwMDExNjczNTQyMzA5NDg0NDQxNjI4MDAyMTMxNDA0MzgyMzE3NTMyNTkzNDA0MTY5MDc0MDMxNDMwMTA4NjU1MDA5NTA5MDM4MjMyODAzMTU5ODY2NTEwNTQyMTk5NzQ0MDgyNTM5NjYwMjg2NDAyNDI0NjE3NjIxNDEwNTk4IiwiMTIxMzg4MTc2NzY5ODE1NjY4ODI2MjYzODA0NDczNzk2NTg4NzU2MjQ5ODY3MzIwNjQzNzAwMjM2MDQyNzQ3MTgzNzI5NTY1MzM2MzgxMzkyMzM3MzIzMDIxNDc3MjQyNzEzOTQ0NzkxOTY0OTY0NjkxNzU0ODMzNDkzODAxNzQxNzY5MDA0Mjk5OTE2MDg4MTczOTgwOTY2OTg1ODc2MDk4NzQ1MDQ3NzQwOTQ2OTEyNDYzNTI1MDM0Mjc3OTI5MDMwODAzMDcyMjkzNTcxNTgwNjY2MjkwNTU3MDU3MTQzMDY3OTMwNDM4NzE5MzMzOTQzMzk1NTIxODExMjk5NTczODk1MzYwNjUxODcxNjAyNjMwMjg2NTQ1NDE3MDEzNzE4Mjc5MDcxNTI1MDYwOTk1MzMzODczODE2NDQ1MTc2NDU5NzEwNDUzODI3ODY1NzkyODQyNDY3NzgwODEwMjE5NTk2NDA0NDMzODMyNDkzODg4MjY2MTYyMzM1OTM1OTQ4MTgxNDMxMzE3NTk1ODE5ODE4NTcxMzA3MDIxODQxMTM2OTI0ODc5NjM4NTMyODg4MDMzNzE3MjA1MTM1MTAyMzI3NjY4NTQ0NjY2MjE5NDgyNDg4MjM3MzU5MDk0MjQ2MzUwMzMzNzU2NTIxMTcxODU5NjAxMjQyNDExNTc4NDU5NTE0Mzk2NTY0MzkxNTQ4Njc4NDE0NDcxMTU2MTgwODY4OTQyNDcyNDk1NzY4OTc5NjExMjgyMjk2MzE1MTk4NzI0MzA2ODM0Njg4NzgwNzk1NjIxNjI2MDQzNjQ0MTM4ODI0MDIiLCIxNDg3MTM1NjI3MzI2OTAyMzAwNjMxMTA1OTczMjQxNDQ1MTAwNjQwNTU5MjMxNTQ5NDY5MTczODE4NzkyMDkwMTk4MDM5MjEzMDgyNDYyOTgxMjc2NzM3MTEwNjQ2Mjk5NDAyNzIzOTkzOTc5MjMzNjYyNzI1NTU1MTU2NTc1MjI5MjkwODgwMTI5OTAxNDk5OTY2OTM5MDUxODk5MjgzOTg4Mzk0OTcyNDExMTY1MjIxODc5MDU4ODY0NDIxOTQ0MDA5ODc1Njc0NDMxMTc0MDk3NzgyMjU5MDI0NDAxMzAzNzE0MTMwMzE0MDM3Nzk1MTk4MzI2MDU1NzA4ODg0MDY1OTc1ODcxNTE1MTExNTYxODA3NTYzODIwMzMwNTE2NzcyMDg3NDY0MDAwNzc0NzgyNjY1NzE1NTE3MjIzMzE4MDQwMTgxMjA1Mzc0MTkxMjM5Mjg4NjA5MDAzMTQ3NzY2MjI1NzQ1NTU3NDY3MTc4NDI4ODk2NzM5MjQxOTIxODc3MjYzNzk2NTY5NTU1ODQxODI5MjU3NTgzMDE0NjEzMjc4NzkyNzI1MjYxMjAxODQ4MDc5NDg4Mjk2NTEzOTY1MjE0NDUyNDA0MzM5OTM5MTQ5Nzg0NDEzODUxNjAyODM0NjA2MzA0NDk1NDE3MDMxNTIyOTUzMTA2MDg5NjMwNjU1NTg4NTAwMjI3NzIzOTc4NTY2OTE4MTEwNDUwNzc2NDc4NDk1ODM2NzA0MTM2OTcwMjExMzU5NTEyMjkzOTkwNzU0NTc0MDE2MzkwMTkzMTE1NjQ3ODg5MTg2NzI4NDMxMDUwOSIsIjgxNTgxMTc0MzM0NDE5MzQzNjU3MDUyODEzNDIxMDEzNjA3ODE3MzM3MTE3NDE2MTA2NTU0NjUwNDIzNjcwMjE1Mzg2MTI0MjkwMDMyMDk1MTA3Mzg2NTk1MzMzMjQ5MTQ0NTA1Mjk3MTk1ODY1NjYwMTMyMjM4NzUwMzMwOTU1NjM5OTEwMDUzMTM1NDQwOTY2MTE5NzY3OTQ0ODgxMzgxMTY2NDgwODc2MjI2Njk4OTA3MTkxMTk5Mjc1MDMyODgyODMxNTU3MDk4NjAzMDg0NDcyNTEwNTY5OTgzODIzMzc1NjEyNDIyMzY0MDI5NTgyODU5MDM5OTcwODczODA5NzQ0MjA3MzM1MjYxMjc3OTAyMTMzNzM0ODQ5MTgzNDU0MTg1NTY3NzYyNjgzMzE1NDA4ODc3MTg1NzE3MjM3OTMxMTUwOTk1NzM5NDUwMDI3NDE3NDMzNjU0MzQ2MTgyODgwMjU3NjY4Mzc0NTI1Njc3NTM0Mjg0MDQwNzMxOTgwNjI1NjQzNTY0MDc4OTAyMTk1NjUzMTM1NjAwMzE2OTA4NzY1MDE3MDI2MTgwMTUyOTAzMjE0NjA2NDc4NTU5OTM2MjIzMzUwMzA0Mzc4NTAxNDI5NTYyMTU0NjU3Mzg4Nzg5NDM1Njk5MjE0OTYzOTYyMzA3MDY5NzMwMzc3NDYzMDA3Mjg3ODEwNTY2NTU1NjIwMTY4MjUxNTgyNTQ3OTk0MDYxNzAyMDg4NzI3NzAzMzUwMDQ2MDA4ODYwNzUyNDg1OTQwMTg4MjExODA3ODQ0NjQzODYyNTQzMDYxMTIxMDAzODciLCIyNjczMjU0NTkzMTk3Njk3Mjc1NzQwMDkwNTE4MzEyNjQzMTU1ODc5NTk4MzMyMTEzNTc5MTc5NzkwOTk4MTM1MTE0NjU5MTUzMzM1OTI1MTExMzY0MTk2ODU4NTc4NzQwMDIzNzk3OTc3ODI2NDY1MzQzNTQ0ODY5MDMwODU5MTU0ODgxNDA1MDA2MzQwMDQzMDE2MzY1NDMwMTU4MjczNzQwMzU0MjcxNjQzMjE0MTY1NDA0NTg5MDQzNjk5MTE2MTYyMDI2MDU1NjI0NjI2MzUxOTM3ODEzOTI2MzA5NzY2NTAzMTIwNDc4MTMxMTk0NjU4NTk0NzU1NDQzNjQ0NjkwNzkzMjE4NTk3MzQ5ODM0NjE1MDgxNDk4OTI1OTAzOTIzMjQ4NzY5NDEyNDczMjcxMTMxOTQ0OTgzMjAzMTAyMTY1NjMzNDA1OTE0NTg4MTE5MjkwMzg3MTY3Mjg1NzEyNDY2NjUwMDUyNzc1MzUxNDM3MjEyODg0ODUyMjI2OTUwNDk5NjQ3MDc5MDk2MjI1OTI0NDMyMzcyNTE0MzUwODcyNTEwNzQ3NTc5ODg1OTM1OTQwOTc2NzY1ODAwODc4NDc1NTg1NjM3Mjk5NzA3NTMzNzc1MjYyMTYxNjU5NDY0NjY5ODQyNjkwNjMzNjE5NzE4MDI3NjI3NTM1NzY4OTY2ODU4NDU2NTA0ODQyNDk5NDA3NDc4NjYxMTQzOTk5NzU0MDUzNDMyMTI0NjE4NzUwMDMyMDExODQ3Njc5NDk5NjM5Mzc5OTU2MjQ5MzMyMzAxNTczODQzNDE1ODU3Njc1MzA5IiwiNDc2MTM5ODUxNjQzMDAwNDMxMzc0MzM2OTA0MDEzMTQ2MjM1MzgyMjI2Mjk4MDUwNTAwNjc1MTE0NTk2MTgyOTE1MjA2Nzc3MzM1MDAyOTM2NjI1NzM4MTQ1OTAzNjc3NjEzMDI5MDgyNjkxNzU0ODkyMzg5MTUwNDU0MDU5NzYzMDMwNTc1ODkyMTU5MjkzMDUxNjA1MzQ4MzYxNTIwMzI5ODk3MzY1NjU3NDA3MjM2MzcxMjE1MzkwMTc2MzA4OTQ0MTM3MDMwOTI5NDM5Nzc3MDU4NjUwNDA1NTg4NDQ2MzUyNTE1NDI3NjY4NTgzMTIyMDIzNzY0ODg0OTY4MDI5Mzc5NTEwNTgyODg2NzI4NzE2NzY5Mjg4NTQxOTQ1ODkxMTIzOTQxNTk4NTE4MDQ1MjUxOTE2NzQxODEyODQ3MTIzMjI3NDI1NzQ4MTAwNTcyNzY2MzY5NTU3ODMyNjc2NDEyNzg1MjMwOTE4NDY4NzIwMDk2OTM3NjE5NjUzMTUxODk4NDExODQ1MDI0MTQ2ODMyMzY0MTA4MzQyMTM2OTg4OTM5MjY4MjQxNjM5NjIzODMwMTg2NTU5OTc2ODcxNDI1NzcxMDg1NDI5NDc1OTg1NjE3MTYzMTMzMzkzMDQ5Nzg1NDUxOTMyOTIzMzIyNjg5MDE4OTgyMDUyNDA5ODk5ODE5MjcyNDE3ODUxODU5MDc3NTgxOTM3NDEzMzI2MTA5NDk5Mzk5NTM2NTMwMDA3NzQ0OTI2MjQ2ODk4MTczMDgxMTI0MzQyMzEwODI3ODcwNzMzMzA5MDY3OTk5MzU4NjUyNiIsIjEyMDY2MDk2NjAyNDczMjAwNDk1ODkxODgzNjU2OTM5NzI3NDk5NTkzNjgwNjExNzI1ODI1NTU5MDA4NTIwNDk1NzY2OTM0NDA5MzI5ODA1MjQ2NTgzNDE1Njk1ODgzNTczNTIzNTg0MzYxNjIxODU0NDY1OTk5NjA0MTMwNDExNzczODY5NjIxNzg0Mjg4ODMxNjQ3MDU0MTg1NjAxODc3Njg0NjA0NzIwOTk4MDEzMTg3Nzc2NTQ4MTEzMTM1NzY0NDgxOTc4NTM4Njk5MzUyNzQ0OTY5NDUyMjI4NDEwNzg5ODk4MjU4MjY0Nzc1NzIyODk5MTcwMjE1NjkzNzk5NDAzNzM3NTQzODY0ODg1NzEzMTQwODM4ODQyMDgwODkzMzI5MDI1MzA3ODU2MTY1MTQ1ODE4ODY1NDA2NDg2MzA3ODY5NTMxMDMxMjU1MjQwNTE3ODk3MjcyNjk0MDg0NDE2MTExODc2MzExMTc2OTkxNzM3NjY0ODA0NzYxODcyMTg0MzE5Mjk4ODc4NDMzMzIzNDM3OTQzMjY0NDgwMTMzNjM0NDUzNjI1MDUzODk0OTAxOTk1NzExMzIyNTc2MTAyODUxMDY5MDExOTM1OTI1OTMxNTI3MzE4ODQ5Mzg0NjY5NTg2MTE5NzU5MDc0OTIwNjM3NzcyNzA3NDEyMDE3OTIwOTI3MDE1MjEwMDgxNjcwNjQ3Mzc3MDM5MTczMTE5ODY4NTQ2NTI5ODA2ODY3NDU4NjQzNDg5NzcxNjU3NTQyMjgxNTE1MzU3MDMzODA5NTU5Mjg1MTEzNjE0MTQ3MDExMjYzIl19fQ==\"}";
	private final String verificationDataSetJson = "{\"electionEventId\":\"1d942d2f7ee24150af6ab979d3c43c59\",\"verificationCardSetIssuerCert\":\"-----BEGIN CERTIFICATE-----\\r\\nMIIDsTCCApmgAwIBAgIVAPc+RhmguzzCd2/+axuAjpGAWpbJMA0GCSqGSIb3DQEB\\r\\nCwUAMHsxNTAzBgNVBAMMLFNlcnZpY2VzIENBIDFkOTQyZDJmN2VlMjQxNTBhZjZh\\r\\nYjk3OWQzYzQzYzU5MRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlT\\r\\nd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcNMTUxMjIyMTQwNzM0\\r\\nWhcNMTYxMjEwMTAwMDAwWjCBhjFAMD4GA1UEAww3VmVyaWZpY2F0aW9uQ2FyZElz\\r\\nc3VlciAxZDk0MmQyZjdlZTI0MTUwYWY2YWI5NzlkM2M0M2M1OTEWMBQGA1UECwwN\\r\\nT25saW5lIFZvdGluZzESMBAGA1UECgwJU3dpc3NQb3N0MQkwBwYDVQQHDAAxCzAJ\\r\\nBgNVBAYTAkNIMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhQj73Ozc\\r\\nC9YlUVElc9YnowOpTr7ekHNeWNZQIhWIQojfJTMuhGl9ZZx4rFk7LPVMMdt0Nt/6\\r\\nQ70RIhKb3XPpe4j6pO+rLS1xQsdRvFwydZivgM2A3qKNIiRjtJqRE6jyTHasaQ2G\\r\\nX5RVcjFGJ+Rt2shvMP+bUqmbege6ukGXRbDKFwm7v6IBTqnd06o2WwsbMB+jGPW1\\r\\nCJP6bwQv3pPwGiZbrYE9Onft5XbsqYqkf1r1PQnPzQmIsRg1Q7GFSgGy0g/F9FkH\\r\\ndZbN5tBeT3DFywg1BRUNNYcx07nXJ3kJSB6UUzA5+x1lQRTp8gnEbxPd1iH4hWns\\r\\nQda6TrvnqD9WAQIDAQABoyAwHjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIG\\r\\nwDANBgkqhkiG9w0BAQsFAAOCAQEAbhyR3xy7ZRE20y4u60XbJVDET4NbEcLQejkn\\r\\nzqdZ5ebUELvwDgs+JwaHJQfQ5yjxVb4dilR3Eq//nWCRQ30iLFvFSzZ94xtFRVFA\\r\\nH9hw2MDGFVLUprP6wFEpptQoYy8/sVRWxQcOjWmM7TSyg53ljzTZVceEp09++mf3\\r\\n1NsAveU/UquAIUvwDm+Qw+U64ClGMpJqFqMebJ+Fomry3yaIxCI4ToGgcw22c8E3\\r\\nrJ9nowpJzK4BahRfIOxHe/YtF6/97HUXbQ3z+JztvNT0nrz/wsPrgTQ4eblPQjH1\\r\\n9QJxxKQoPJWUqvnSbaH26vb4ctms4Ihh8quh39TaiWjp6LHymw==\\r\\n-----END CERTIFICATE-----\\r\\n\",\"verificationCardSetId\":\"4e84749ff7a64f1cbb97a4ca26fb9298\"}";

	@Mock
	private VerificationSetRepository verificationSetRepository;

	@Mock
	private AsymmetricServiceAPI asymmetricService;

	@Mock
	private Logger LOGGER;

	@Test
	public void verificationJsonValid() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = new Vote();
		vote.setTenantId("100");
		vote.setElectionEventId("100");
		vote.setVotingCardId("100");
		vote.setVerificationCardId("100");
		vote.setVerificationCardSetId("100");
		vote.setVerificationCardPublicKey(verificationCardPublicKey);
		vote.setVerificationCardPKSignature(verificationCardPKSignature);

		VerificationSetEntity verificationDataSet = new VerificationSetEntity();
		verificationDataSet.setJson(verificationDataSetJson);
		when(verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(anyString(), anyString(), anyString()))
				.thenReturn(verificationDataSet);

		when(asymmetricService.verifySignature(any(), any(), any(), any(), any())).thenReturn(true);

		ValidationError result = verifyVerificationCardPKSignatureRule.execute(vote);

		assertEquals(result.getValidationErrorType(), ValidationErrorType.SUCCESS);
	}

	@Test
	public void verificationJsonParsingError() throws ResourceNotFoundException {
		Vote vote = new Vote();
		vote.setTenantId("100");
		vote.setElectionEventId("100");
		vote.setVotingCardId("100");
		vote.setVerificationCardId("100");
		vote.setVerificationCardSetId("100");
		vote.setVerificationCardPublicKey(verificationCardPublicKey);
		vote.setVerificationCardPKSignature(verificationCardPKSignature);

		VerificationSetEntity verificationDataSet = new VerificationSetEntity();
		verificationDataSet.setJson(verificationDataSetInvalidJson);
		when(verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(anyString(), anyString(), anyString()))
				.thenReturn(verificationDataSet);

		ValidationError result = verifyVerificationCardPKSignatureRule.execute(vote);

		assertEquals(result.getValidationErrorType(), ValidationErrorType.FAILED);
	}

	@Test
	public void verificationJsonNotValid() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = new Vote();
		vote.setTenantId("100");
		vote.setElectionEventId("100");
		vote.setVotingCardId("100");
		vote.setVerificationCardId("100");
		vote.setVerificationCardSetId("100");
		vote.setVerificationCardPublicKey(verificationCardPublicKey);
		vote.setVerificationCardPKSignature(verificationCardPKSignature);

		VerificationSetEntity verificationDataSet = new VerificationSetEntity();
		verificationDataSet.setJson(verificationDataSetJson);
		when(verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(anyString(), anyString(), anyString()))
				.thenReturn(verificationDataSet);

		when(asymmetricService.verifySignature(any(), any(), any(), any(), any())).thenReturn(false);

		ValidationError result = verifyVerificationCardPKSignatureRule.execute(vote);

		assertEquals(result.getValidationErrorType(), ValidationErrorType.FAILED);
	}

}
