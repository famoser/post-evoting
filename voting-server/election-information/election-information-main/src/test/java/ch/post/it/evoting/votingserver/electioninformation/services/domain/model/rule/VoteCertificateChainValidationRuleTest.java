/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class VoteCertificateChainValidationRuleTest {

	private final String json = "{\"electionEventId\":\"1d942d2f7ee24150af6ab979d3c43c59\",\"electionRootCA\":\"-----BEGIN CERTIFICATE-----\\nMIIDwDCCAqigAwIBAgIVAKNLm2cx4bRtj2SrSRUyECgR9aiWMA0GCSqGSIb3DQEB\\nCwUAMIGGMUAwPgYDVQQDDDdFbGVjdGlvbiBFdmVudCBSb290IENBIDFkOTQyZDJm\\nN2VlMjQxNTBhZjZhYjk3OWQzYzQzYzU5MRYwFAYDVQQLDA1PbmxpbmUgVm90aW5n\\nMRIwEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcN\\nMTUxMjIyMTQwNzAxWhcNMTYxMjEwMTAwMDAwWjCBhjFAMD4GA1UEAww3RWxlY3Rp\\nb24gRXZlbnQgUm9vdCBDQSAxZDk0MmQyZjdlZTI0MTUwYWY2YWI5NzlkM2M0M2M1\\nOTEWMBQGA1UECwwNT25saW5lIFZvdGluZzESMBAGA1UECgwJU3dpc3NQb3N0MQkw\\nBwYDVQQHDAAxCzAJBgNVBAYTAkNIMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\\nCgKCAQEAgveRl0R1cT4Hm38VJOYRJ04t7CDy4GPzhr7wVCDCeo4tRnHPTqy61yzH\\nOuYpPft/Ur+HzErcx2ZHn9Z9ro8e153d62yIy62oF9dUNawXZlFX2D6KVSmuc+bg\\nw8A4D/cfPm/lPe3SNhf8Xtnycim32EWxDuSn4KGleU7PCkGLa8dIY52TFWXmf8MR\\nWFLZVQQ8/YACbX0t/GliJGaIgYK550gVBIFyebbgYMZspdfAoX99QlZqgGTAULYQ\\n5b8QnYSyXITOyckLTXAD+iWFCU9GQhBjoK3kFRe4v//kIwnYtVI6zbkrkicKktz5\\nJFy8K2v0Qg0/uGEky80gCik6tP4bkQIDAQABoyMwITAPBgNVHRMBAf8EBTADAQH/\\nMA4GA1UdDwEB/wQEAwIBBjANBgkqhkiG9w0BAQsFAAOCAQEAZO+ce1VOZuFbGBvG\\nj/23RxrYxHnldQImZmaCqIk7rlWBFP2Gs4jdsCJVZKmbpkKtWLKFo2GwCM+djc6t\\nVgis28nMd0dfSn3o2XpzA27xTfnXyua0R4xob1NysplyT3SfV6uZyEbiwJgbaAwr\\nRpmPYyWQnqzuW8LN7gu0WXOH73cz0v1j9R/+8AXKEN8x88RnlXc3wGUTILUbsIc8\\nYrldz5s1zqsV24Fvh86lpvrAjQeknSuAhZuU2JQUGMjg/wEyC7Wq8U/6Rk2t0EsO\\n9ajhXmJgs7Pc+VARE43BBcRrGeUCI1TZsG57uyLyjiqOOnFzrSGZKMNgn0x4Zcfv\\nK6MUlw==\\n-----END CERTIFICATE-----\\n\",\"servicesCA\":\"-----BEGIN CERTIFICATE-----\\nMIIDtDCCApygAwIBAgIVALIYpQ6WH9kO3H7zhcLKQjNJKg1JMA0GCSqGSIb3DQEB\\nCwUAMIGGMUAwPgYDVQQDDDdFbGVjdGlvbiBFdmVudCBSb290IENBIDFkOTQyZDJm\\nN2VlMjQxNTBhZjZhYjk3OWQzYzQzYzU5MRYwFAYDVQQLDA1PbmxpbmUgVm90aW5n\\nMRIwEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcN\\nMTUxMjIyMTQwNzAxWhcNMTYxMjEwMTAwMDAwWjB7MTUwMwYDVQQDDCxTZXJ2aWNl\\ncyBDQSAxZDk0MmQyZjdlZTI0MTUwYWY2YWI5NzlkM2M0M2M1OTEWMBQGA1UECwwN\\nT25saW5lIFZvdGluZzESMBAGA1UECgwJU3dpc3NQb3N0MQkwBwYDVQQHDAAxCzAJ\\nBgNVBAYTAkNIMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhh0b0pok\\nZQvFH4DQXuh9FxSt9Tz4+mrJdpwfKFL/4v33/A55iV5+FU7RZMh7BgEieXjyw1mo\\nVZqhad5P9dEtW66rIoyy2YM+4LwrAIpqxBUW6iy6j2dQvLo+GYYhw6TsC9BqpXoh\\n2HzrtkrPVEhhv9a+TeHsEPDN9MmTYzwAcW04JyrfLVwthF9rpHkZ6GrTnlIl3GBo\\nFPHtySSxy+Ye23QiLHDggwsX5nFkR1MmIHPY8pb9IQ4XQWEhmul83j3NmraFHr03\\nd2Do3/nHA1kbFdrWgASpfBIVNRRXypqpUX4qpNIApgUrkcbrkVGJvdY8QxvrVxV4\\ndkocVyCoW2FD6wIDAQABoyMwITAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQE\\nAwIBBjANBgkqhkiG9w0BAQsFAAOCAQEAbVE/762TLUOhNzI6Y79+E5L0U4ueP8C5\\nimq7/gvuJZK/O8ruJpi/UeQ/W+n8+59BCqXkhtfCfEzM0qtGq5CrqxFJgMzTYeLP\\nuByFv92LHLsGgYeReZdIiLvI4/8k3FwskYoEZq4slgTZMHcOfRFm2/9p3y96iVIN\\nRfPCUN+JcX7uXnw5/ptL3RrWc5aeE570ukSEetbb3I0U83j8DJqfI+KuwiN7pFpv\\naWH+JR9Z9SMoe1PU9xD4YUpAQ3Kb32Yi/2CJwCs/oypbFQIoC4oYaONA6MrTH/rm\\noRqqcttA7QpeZFA5axJVgfgwMXKUit6p8cUC9ccpY5mZnXfC6/v6bg==\\n-----END CERTIFICATE-----\\n\",\"authoritiesCA\":\"-----BEGIN CERTIFICATE-----\\nMIIDtjCCAp6gAwIBAgIUHKEJcoa7qzXHt69lxiQ3WbOtaYgwDQYJKoZIhvcNAQEL\\nBQAwgYYxQDA+BgNVBAMMN0VsZWN0aW9uIEV2ZW50IFJvb3QgQ0EgMWQ5NDJkMmY3\\nZWUyNDE1MGFmNmFiOTc5ZDNjNDNjNTkxFjAUBgNVBAsMDU9ubGluZSBWb3Rpbmcx\\nEjAQBgNVBAoMCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDAeFw0x\\nNTEyMjIxNDA3MDFaFw0xNjEyMTAxMDAwMDBaMH4xODA2BgNVBAMML0F1dGhvcml0\\naWVzIENBIDFkOTQyZDJmN2VlMjQxNTBhZjZhYjk3OWQzYzQzYzU5MRYwFAYDVQQL\\nDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADEL\\nMAkGA1UEBhMCQ0gwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCStqLl\\nwG4xsnGh1LW5+alQn/xLOqeHYrux+N62h6OkHJdGNvDj9r6Iaev1rBVP8dEjzAEn\\nTJTMWY17daMPoK1HPq9yFhNSfUdvzzaiwJZxqbDpWjVpjXqkY2oJTV6JijCkLLzz\\nC3y4Lwl77NTLO75nYMkPos/s6KtVProkA5VTPhW02ntn1J1wbu9/S0IJueGXBFjV\\nHTukea5wC3MaDpL5kBMwYi0UFynWYK5MamowPHaWZlSKoUnhuB8ndBf8qaLiG3ih\\nSsd7qEFAw6KKv7ZjYym+F6xT3zeOfh2Y5qpO+AV8G6HEBBVwcybTzY4naxosV1ft\\ny6F9p59NJ0NoZE5NAgMBAAGjIzAhMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/\\nBAQDAgEGMA0GCSqGSIb3DQEBCwUAA4IBAQAm4ZHuCORKbTAzusMCbfGDS+g0gp7c\\nh6bxXeHAoVREZXcIhMqs+g5XRg12mkEQ8tIjan1ew5aEpgvwT0hXiSmkrWbpn0MB\\n2A+hBZrPeOvQEtkhN7r/SkWFX0Chg5JBZ6Rv6KYg+L8+PCBjwkkkYqr5Wxx5vVzg\\nO826sH+trxQBNTArF8NrOJHLgLx5D0oJTrWHeCdlrdDqZSYXttkri/XZPUsqy6KK\\nWUJ6Y3YtxO3oxqRtFjJDaD5OPNUBROkkVgUWNaBvtMo5WaGHJbW6cmIzXdWFnk2e\\nGQNEs2+fYIwSyoGMRRWpSKaKrKQC0sg65KqA2+P6zCj3lacJTdavTsI7\\n-----END CERTIFICATE-----\\n\",\"credentialsCA\":\"-----BEGIN CERTIFICATE-----\\nMIIDtjCCAp6gAwIBAgIUOSB2hPh3nt5gQOBZAybeFSjriGowDQYJKoZIhvcNAQEL\\nBQAwgYYxQDA+BgNVBAMMN0VsZWN0aW9uIEV2ZW50IFJvb3QgQ0EgMWQ5NDJkMmY3\\nZWUyNDE1MGFmNmFiOTc5ZDNjNDNjNTkxFjAUBgNVBAsMDU9ubGluZSBWb3Rpbmcx\\nEjAQBgNVBAoMCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDAeFw0x\\nNTEyMjIxNDA3MDFaFw0xNjEyMTAxMDAwMDBaMH4xODA2BgNVBAMML0NyZWRlbnRp\\nYWxzIENBIDFkOTQyZDJmN2VlMjQxNTBhZjZhYjk3OWQzYzQzYzU5MRYwFAYDVQQL\\nDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADEL\\nMAkGA1UEBhMCQ0gwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCmz41k\\nkV2XnZdP6KL2vxesOGahCPg4HtJFJP1rN5ODGWYiO4jlHlPtL2OY+AStUbHh+pL0\\nweExfJZ+lSTPQ+dBRDdruiph093u8bAWKDJhJS3A7KihH7dfiWAQhbSx6tOi+p97\\nCWubALuPWFmD2jtwwJvB6hwGqtIXMS5QWVan+sW8i3zzwfCdyER+RkW2YQ0S57At\\njKSHhPYp2kfzYDJGx+bZQpvoQIW3d8FxrHfZlMu5u7f9CAR+EivPceyjA7IX8au3\\nKZ0+tSHjAYs3lmifH7QAlFYupbgF+1HfkhUkPZa4S7XyyTyt0cz8USS3WigK6Mxm\\nGxEMRmVpLEFabYsRAgMBAAGjIzAhMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/\\nBAQDAgEGMA0GCSqGSIb3DQEBCwUAA4IBAQA+ZEBdFFlkuEg0WVb4wF0wA6p/aMGy\\nxqpEGE4tUokrHy2hgkga+2KXl3EFebnTXcQHy5Z+MNHR5kqR6ixtiwE/Q2XQOz2R\\n0Tw9JMOPX6ZB2Rcaleg/LaC5UWPpFiNI69mNXbDPMawam1f10Vw+spJCW+fCKFP2\\nIp6iWOrml+TYv1qQ4rsPwUtfQWsnK3Y+19eCj560c8u95ScQKmBPEW3Q2R9x8jRa\\nN872U5m654UtUJL0A1JTYUY68kSlhNsaZlPm/1ap3c2x2lCp7ZHBfQx7ymntP2ap\\ncgEI9xQCNtBTL5QP3rQxzFKJPiSntAEGkTtT14zjRGsFnk1Ma3c6TXAU\\n-----END CERTIFICATE-----\\n\",\"adminBoard\":\"-----BEGIN CERTIFICATE-----\\nMIIDsDCCApigAwIBAgIUGqYs1LghTVtIlPNetOECOm/TSzMwDQYJKoZIhvcNAQEL\\nBQAwfjE4MDYGA1UEAwwvQXV0aG9yaXRpZXMgQ0EgMWQ5NDJkMmY3ZWUyNDE1MGFm\\nNmFiOTc5ZDNjNDNjNTkxFjAUBgNVBAsMDU9ubGluZSBWb3RpbmcxEjAQBgNVBAoM\\nCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDAeFw0xNTEyMjIxNDA3\\nMDFaFw0xNjEyMTAxMDAwMDBaMIGDMT0wOwYDVQQDDDRBZG1pbmlzdHJhdGlvbkJv\\nYXJkIDFkOTQyZDJmN2VlMjQxNTBhZjZhYjk3OWQzYzQzYzU5MRYwFAYDVQQLDA1P\\nbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkG\\nA1UEBhMCQ0gwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCb0Wy0eHZK\\nkkhqXiu7d9h8XoZY/byMjRlfOwAeT7wpbSqcY5eZjNFbL6nPqxT1isHCjx/rnJbF\\n79MDFpQ49yptSV11GeVU3toiyqsl8EwuOnOncYTzl+3ph+thpOOJ2DD0le57CdS4\\nEZ+iyMa2F8+dq3j6/Ns5jG1TwLQWwyXvD1KeFsVeMnHA29lIpO87ZLHUtF/RbYMh\\nT7iBY9egmzQzHRs8GIGS9n7R/uScDJg4vxy5zeFlYcugpzln70M/GrBGQjQgKDbm\\nwOyd9q6igTEmBUv0TGOHd583X9LgExuy63bYayTxuIP6jrXkwFsSbphtE+UzAsw8\\nz0xOgMbj2PoRAgMBAAGjIDAeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgbA\\nMA0GCSqGSIb3DQEBCwUAA4IBAQAtxM2nh6CBT7EoMXdmFFP8T4GMrAGqNKV9sGpO\\nsnmDLXm4IzHRJu6CfzwOCHuR6MHt6GF9eV7AEUbBKKXud9zcvocEyUcuaht5EzOn\\niQMMHomjoeJ3HeWASRrEzIM+uiJcmaMNiDT8jpUGAbrIXIr6versbh+UA7ku3E2G\\npt32PpNNj7egx1SNyWR6buN7Ts/TUapxTWEruwky5MfHSKqsUWYomlTcmgNyhu4E\\n2xqF/WqJeTom1aV6TpVXz4LzZjzQuSlNH8jxZS+2/V0ms4Ku4Sa3t2dGnhRTpY+s\\n6g+HstsgZmMNsvFmYvETK1vr++X6Dek7Sjxl8i5CjUJ9oDmA\\n-----END CERTIFICATE-----\\n\",\"electionInformationParams\":{\"numVotesPerVotingCard\":\"1\",\"numVotesPerAuthToken\":\"1\"},\"signature\":null}";

	private final String cert = "-----BEGIN CERTIFICATE-----\r\nMIIDoTCCAomgAwIBAgIVAMsl9BPSnbNr/aHfOEtYSN9QyBiHMA0GCSqGSIb3DQEB\r\nCwUAMH4xODA2BgNVBAMML0NyZWRlbnRpYWxzIENBIDFkOTQyZDJmN2VlMjQxNTBh\r\nZjZhYjk3OWQzYzQzYzU5MRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQK\r\nDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcNMTUxMjIyMTQw\r\nNzM0WhcNMTYxMjEwMTAwMDAwWjB0MS4wLAYDVQQDDCVTaWduIDUwMWM5NDc4YTg2\r\nNTQxYmI5OGYwZDk3YjljZTYzNmIyMRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIw\r\nEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwggEiMA0G\r\nCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCr2jXbPMBAvq01l38iqyRHC8IGihGe\r\npvsIGU5yK5KeNXv3RqyQkTt1NJjBOwItAiFXVpZmFpEPlPqATYznPUC91bMV7vax\r\n+/+ZdSFAyVoj3DYbyWNvIGW9Abja5Oz/fRG3bOC1Ok0cmJb32a9uR7NnH2M8ik2/\r\n7J4xxvjs1L9o02+U8o9JO1Idw0blz3mQj0DYaEadTPXKDUiA0fQ6tv87A728OSnN\r\ndh9mgcy62+PW/S6kX9PnlYQoJ0nguSCHotmOSmG/OVlsa6V6pg7dvf5asmNE8+mu\r\nkD3fSyVH+HQVQIRv/xF2wDEQf/vvLEcs7BaF+oeQIL7JXuCl1dQ0YLmZAgMBAAGj\r\nIDAeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgbAMA0GCSqGSIb3DQEBCwUA\r\nA4IBAQCI6QX8Zr+05ir0l+fmKcckdA7OTQsksOtnyBv1oBKR+1vclmIE3WMlGkif\r\nIXnVHGH5X8G7MLjhdQkiRFDHrjDc4l4XKdBFrEK+Cll/P7cn0x73+EF30sw4Wmii\r\nDD2jOZc8afm32AZxAT8mNESMG4opHFWRhvXHL/88r/M2weeIFxDqWf9hm8SoAFjd\r\nUhbIkdsU4YmBfaRDrxdQFUJDp+N2jfWFquP2+Dx3rW8wGFGzVe3hSfeCt339X1pT\r\nqZLH1qFP8cmFDr2nwS29g0gEgjsidzTElLQi76FOvu8ml05OFzvf8i4di/zoth58\r\nT6BNH+uog7VNkszUMjf0fpKDQg/u\r\n-----END CERTIFICATE-----\r\n";

	private final String certInvalidFormat = "-----BEGIN CERTIFICATE-----\\r\\nMIIDoTCCAomgAwIBAgIVAMsl9BPSnbNr/aHfOEtYSN9QyBiHMA0GCSqGSIb3DQEB\\r\\nCwUAMH4xODA2BgNVBAMML0NyZWRlbnRpYWxzIENBIDFkOTQyZDJmN2VlMjQxNTBh\\r\\nZjZhYjk3OWQzYzQzYzU5MRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQK\\r\\nDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwHhcNMTUxMjIyMTQw\\r\\nNzM0WhcNMTYxMjEwMTAwMDAwWjB0MS4wLAYDVQQDDCVTaWduIDUwMWM5NDc4YTg2\\r\\nNTQxYmI5OGYwZDk3YjljZTYzNmIyMRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIw\\r\\nEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwggEiMA0G\\r\\nCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCr2jXbPMBAvq01l38iqyRHC8IGihGe\\r\\npvsIGU5yK5KeNXv3RqyQkTt1NJjBOwItAiFXVpZmFpEPlPqATYznPUC91bMV7vax\\r\\n+/+ZdSFAyVoj3DYbyWNvIGW9Abja5Oz/fRG3bOC1Ok0cmJb32a9uR7NnH2M8ik2/\\r\\n7J4xxvjs1L9o02+U8o9JO1Idw0blz3mQj0DYaEadTPXKDUiA0fQ6tv87A728OSnN\\r\\ndh9mgcy62+PW/S6kX9PnlYQoJ0nguSCHotmOSmG/OVlsa6V6pg7dvf5asmNE8+mu\\r\\nkD3fSyVH+HQVQIRv/xF2wDEQf/vvLEcs7BaF+oeQIL7JXuCl1dQ0YLmZAgMBAAGj\\r\\nIDAeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgbAMA0GCSqGSIb3DQEBCwUA\\r\\nA4IBAQCI6QX8Zr+05ir0l+fmKcckdA7OTQsksOtnyBv1oBKR+1vclmIE3WMlGkif\\r\\nIXnVHGH5X8G7MLjhdQkiRFDHrjDc4l4XKdBFrEK+Cll/P7cn0x73+EF30sw4Wmii\\r\\nDD2jOZc8afm32AZxAT8mNESMG4opHFWRhvXHL/88r/M2weeIFxDqWf9hm8SoAFjd\\r\\nUhbIkdsU4YmBfaRDrxdQFUJDp+N2jfWFquP2+Dx3rW8wGFGzVe3hSfeCt339X1pT\\r\\nqZLH1qFP8cmFDr2nwS29g0gEgjsidzTElLQi76FOvu8ml05OFzvf8i4di/zoth58\\r\\nT6BNH+uog7VNkszUMjf0fpKDQg/u\\r\\n-----END CERTIFICATE-----\\r\\n";

	private final String certInvalid = "-----BEGIN CERTIFICATE-----\r\nMIIDsDCCApigAwIBAgIUJOGf5TZnCOHfS4nqRA/qgYaR4k4wDQYJKoZIhvcNAQEL\r\nBQAwezE1MDMGA1UEAwwsU2VydmljZXMgQ0EgZjg2Yjk5NjdmNWYxNGEwMWI5Zjcw\r\nYzYyNzdjMzgzMjkxFjAUBgNVBAsMDU9ubGluZSBWb3RpbmcxEjAQBgNVBAoMCVN3\r\naXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDAeFw0xNjAxMTQxNzIxMTFa\r\nFw0xNzAxMDEwMDAwMDBaMIGGMUAwPgYDVQQDDDdWZXJpZmljYXRpb25DYXJkSXNz\r\ndWVyIGY4NmI5OTY3ZjVmMTRhMDFiOWY3MGM2Mjc3YzM4MzI5MRYwFAYDVQQLDA1P\r\nbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlTd2lzc1Bvc3QxCTAHBgNVBAcMADELMAkG\r\nA1UEBhMCQ0gwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCpczcovpJ0\r\nQ4Xis3fbZ5moR6ZEOzYmLeAkRKS8ZoP4JvNHl8xSdjFQ7hL7ty15hWLFQdnBn2aD\r\nHhM1ydp2fwRelLBj7SoPpk4xco9FphpidfqsVzoLzUh3O4RsVx5GHFtwQ5+bQDhM\r\njy1/L5noiprxZzZFcTQpycYHuEJsHyn0WplYrkIbl24CmvKpmOXFhxDZVdaZdyWY\r\nYaaBWawklAKNjDE6nW2HioZ+ZuVld3Hip1mi23cDtBBuSCk6hgHQOkPx4IKpYkfU\r\nxRovXD5fthvgzgrprMLcviOuAkZJWamQnJQ+lUGVrygu8L4nwDiJUWoRlCCK+GCi\r\n/gyD+QoYjKcTAgMBAAGjIDAeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgbA\r\nMA0GCSqGSIb3DQEBCwUAA4IBAQAu4DVM0aDOT66pvYVhhImEz5n/lFShqp0y1oU9\r\nQMBd+4ZKebcp3HNDmADkKiYQp3FXNVlBOxT62vnvIihZxj+e+O3p1bK3Ji3BQVyw\r\nqdP0wEf3MvHkCY7c/ykyHZkNMYgE6cj3KMr+vr5ly0wCCDDXx0mBBIUJDXcUDjNt\r\nFHkxmENTx6sTVEaik0OGcTxZOxwkeiQgV6h7e+zEWy26tgrYUxX60XN7ZBKn3c8r\r\nMutxmFIMLqhidzZtPAJ9+qjYIpL0nvVym7/juCxJ8yxuzYVLvx2oAGTKu6BYyEcM\r\n3OKF4mKx6A/8lW4gIISUjxhy47MyEVfqhBLiVvk0WsqVeIYF\r\n-----END CERTIFICATE-----\r\n";

	@InjectMocks
	private final VoteCertificateChainValidationRule voteCertChainValidationRule = new VoteCertificateChainValidationRule();

	@Mock
	private ElectionInformationContentRepository electionInformationContentRepositoryMock;

	@Mock
	private Logger LOGGER;

	@Test
	public void validateOK() throws ResourceNotFoundException, GeneralCryptoLibException {
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setId(100);
		electionInformationContent.setTenantId("100");
		electionInformationContent.setElectionEventId("100");
		electionInformationContent.setJson(json);
		when(electionInformationContentRepositoryMock.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		Vote vote = new Vote();
		vote.setTenantId("100");
		vote.setElectionEventId("100");
		vote.setCertificate(cert);

		ValidationError result = voteCertChainValidationRule.execute(vote);

		assertEquals(result.getValidationErrorType(), ValidationErrorType.SUCCESS);
	}

	@Test
	public void validateCanNotBeParsedCert() throws ResourceNotFoundException, GeneralCryptoLibException {
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setId(100);
		electionInformationContent.setTenantId("100");
		electionInformationContent.setElectionEventId("100");
		electionInformationContent.setJson(json);
		when(electionInformationContentRepositoryMock.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		Vote vote = new Vote();
		vote.setTenantId("100");
		vote.setElectionEventId("100");
		vote.setCertificate(certInvalidFormat);

		ValidationError result = voteCertChainValidationRule.execute(vote);

		assertEquals(result.getValidationErrorType(), ValidationErrorType.FAILED);
	}

	@Test
	public void validateInvalidCert() throws ResourceNotFoundException, GeneralCryptoLibException {
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setId(100);
		electionInformationContent.setTenantId("100");
		electionInformationContent.setElectionEventId("100");
		electionInformationContent.setJson(json);
		when(electionInformationContentRepositoryMock.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		Vote vote = new Vote();
		vote.setTenantId("100");
		vote.setElectionEventId("100");
		vote.setCertificate(certInvalid);

		ValidationError result = voteCertChainValidationRule.execute(vote);

		assertEquals(result.getValidationErrorType(), ValidationErrorType.FAILED);
	}

}
