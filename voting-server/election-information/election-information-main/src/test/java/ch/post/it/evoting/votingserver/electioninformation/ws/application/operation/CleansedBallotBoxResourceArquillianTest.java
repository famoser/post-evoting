/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.ws.application.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedBallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKey;

@RunWith(Arquillian.class)
@RunAsClient
public class CleansedBallotBoxResourceArquillianTest {

	private static final String TEST_BB_ID = "7f59b097dcc6407e933fdfa94b9812b7";
	private static final String TEST_VC_ID = "votingcardid";
	private static final String TEST_ELECTORAL_AUTHORITY_ID = "31a32fbcac864b8ea96c48a505437721";
	private static final String TRACK_ID_HEADER = "X-Request-ID";
	private static final String TEST_TRACK_ID = "TestTrackingId";
	private static final String TEST_SIGNATURE = "signature";
	private static final String JSON_PARAMETER_DATE_FROM = "startDate";
	private static final String JSON_PARAMETER_DATE_TO = "endDate";
	private static final String JSON_PARAMETER_GRACE_PERIOD = "gracePeriod";
	private static final String JSON_PARAMETER_TEST = "test";
	private static final String JSON_PARAMETER_ELECTION_EVENT = "electionEvent";
	private static final String JSON_PARAMETER_ID = "id";
	private static final String JSON_PARAMETER_PASSWORD_KEYSTORE = "passwordKeystore";
	private static final String GET_CLEANSED_BALLOT_BOX_PATH = "cleansedballotboxes/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}";
	private static final String GET_BALLOT_BOX_INFORMATION_PATH = "ballotboxes/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}";
	private static final String BALLOT_BOX_INFORMATION_JSON = "{\"test\":true,\"encryptionParameters\":{\"p\":\"16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759\",\"q\":\"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379\",\"g\":\"2\"},\"eeid\":\"4bbd2887e9a848928487b99490c2296e\",\"bid\":\"a50df3e34ae34731933353a75ac455c6\",\"ballotBoxCert\":\"-----BEGIN CERTIFICATE-----\\r\\nMIIDqDCCApCgAwIBAgIUceWtPU1ZOPrit4TFGkAxhXTlYx4wDQYJKoZIhvcNAQEL\\r\\nBQAwfjE1MDMGA1UEAwwsU2VydmljZXMgQ0EgNGJiZDI4ODdlOWE4NDg5Mjg0ODdi\\r\\nOTk0OTBjMjI5NmUxFjAUBgNVBAsMDU9ubGluZSBWb3RpbmcxFTATBgNVBAoMDE9y\\r\\nZ2FuaXphdGlvbjEJMAcGA1UEBwwAMQswCQYDVQQGEwJFUzAeFw0xNzEyMjAxNjA3\\r\\nMDZaFw0xODEyMzEyMTU5MDBaMHwxMzAxBgNVBAMMKkJhbGxvdEJveCA3ZjU5YjA5\\r\\nN2RjYzY0MDdlOTMzZmRmYTk0Yjk4MTJiNzEWMBQGA1UECwwNT25saW5lIFZvdGlu\\r\\nZzEVMBMGA1UECgwMT3JnYW5pemF0aW9uMQkwBwYDVQQHDAAxCzAJBgNVBAYTAkVT\\r\\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsRPotg1ogwt38Tj7bjWb\\r\\nijCk4NPAB+VUj7cDG+zIQwbzoi8chjfAys6M68YlKhpcGGjR0u1g2Jp/8Pm35PNM\\r\\nBkgSoy4iIa+aomk/nO5y6Y/+qLal/oKQ1R0vs0fbsW39tsSdgSUzmSZe9RXPIXEk\\r\\nrCAUb0gtXs7//nB8sP2l6iPaYYTs+Q5OWk8KeimRoDWRZB9SvwvLoHtGcLJfNCOW\\r\\niDKZYBTGO3q04xImrBJaNRKH+dVauuktsrCSodd28VMNtsHaITVfRfZkc749NQiY\\r\\ndzFI487aIN6xp/e8kpV8JJQviwCXcBp9C4Lsdgcf1q9d3njCYKMsmEV2MauPyIQh\\r\\nVwIDAQABoyAwHjAOBgNVHQ8BAf8EBAMCBsAwDAYDVR0TAQH/BAIwADANBgkqhkiG\\r\\n9w0BAQsFAAOCAQEAQX97ieaKcKbUyHY6hSR1oduFviaMri/3CzTvb9czGCIp/0wF\\r\\ni2myTrF1vzVzTo+u4GiJMOGqVti6bhs8Jdqn4sanXD/AmpxkXioYK3/2NNXLBwQJ\\r\\nuxXq2PNoJqsHeSl8Y3mZ6Sawxxgi+DoVjq6kL7Tk7yRBxdVHhOctEV9DOhPeujjo\\r\\nH24ULUt2knPz1tRGppAQGvy2uFlZxuvQ/tLvpwhL4GrfwRIyAoDc9LIDAjz5pKyU\\r\\nVOVfIKcaPWSFZ6bOBCwRKfalUXvIKAbDTFc3qutX9CtIe6qorxNqt2Jsw6wLpcFi\\r\\njG5gouHH+6oC3RgVQZc5zwYPaNuuqJrGbvUVmw==\\r\\n-----END CERTIFICATE-----\\r\\n\",\"startDate\":\"2016-12-31T22:00Z\",\"endDate\":\"2017-12-31T21:59Z\",\"gracePeriod\":\"900\",\"writeInAlphabet\":\"IyAnKCksLS4vMDEyMzQ1Njc4OUFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXrCoMKixaDFocW9xb7FksWTxbjDgMOBw4LDg8OEw4XDhsOHw4jDicOKw4vDjMONw47Dj8OQw5HDksOTw5TDlcOWw5jDmcOaw5vDnMOdw57Dn8Ogw6HDosOjw6TDpcOmw6fDqMOpw6rDq8Osw63DrsOvw7DDscOyw7PDtMO1w7bDuMO5w7rDu8O8w73DvsO/\",\"electoralAuthorityId\":\"31a32fbcac864b8ea96c48a505437721\",\"alias\":\"FR-CT-1|FR-MU-9999\",\"id\":\"7f59b097dcc6407e933fdfa94b9812b7\"}";
	private static final String BALLOT_BOX_INFORMATION_SIGNATURE = "eyJhbGciOiJQUzI1NiJ9.eyJvYmplY3RUb1NpZ24iOnsiaWQiOiI3ZjU5YjA5N2RjYzY0MDdlOTMzZmRmYTk0Yjk4MTJiNyIsImJhbGxvdEJveENlcnQiOiItLS0tLUJFR0lOIENFUlRJRklDQVRFLS0tLS1cclxuTUlJRHFEQ0NBcENnQXdJQkFnSVVjZVd0UFUxWk9Qcml0NFRGR2tBeGhYVGxZeDR3RFFZSktvWklodmNOQVFFTFxyXG5CUUF3ZmpFMU1ETUdBMVVFQXd3c1UyVnlkbWxqWlhNZ1EwRWdOR0ppWkRJNE9EZGxPV0U0TkRnNU1qZzBPRGRpXHJcbk9UazBPVEJqTWpJNU5tVXhGakFVQmdOVkJBc01EVTl1YkdsdVpTQldiM1JwYm1jeEZUQVRCZ05WQkFvTURFOXlcclxuWjJGdWFYcGhkR2x2YmpFSk1BY0dBMVVFQnd3QU1Rc3dDUVlEVlFRR0V3SkZVekFlRncweE56RXlNakF4TmpBM1xyXG5NRFphRncweE9ERXlNekV5TVRVNU1EQmFNSHd4TXpBeEJnTlZCQU1NS2tKaGJHeHZkRUp2ZUNBM1pqVTVZakE1XHJcbk4yUmpZelkwTURkbE9UTXpabVJtWVRrMFlqazRNVEppTnpFV01CUUdBMVVFQ3d3TlQyNXNhVzVsSUZadmRHbHVcclxuWnpFVk1CTUdBMVVFQ2d3TVQzSm5ZVzVwZW1GMGFXOXVNUWt3QndZRFZRUUhEQUF4Q3pBSkJnTlZCQVlUQWtWVFxyXG5NSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDQVFFQXNSUG90ZzFvZ3d0MzhUajdialdiXHJcbmlqQ2s0TlBBQitWVWo3Y0RHK3pJUXdiem9pOGNoamZBeXM2TTY4WWxLaHBjR0dqUjB1MWcySnAvOFBtMzVQTk1cclxuQmtnU295NGlJYSthb21rL25PNXk2WS8rcUxhbC9vS1ExUjB2czBmYnNXMzl0c1NkZ1NVem1TWmU5UlhQSVhFa1xyXG5yQ0FVYjBndFhzNy8vbkI4c1AybDZpUGFZWVRzK1E1T1drOEtlaW1Sb0RXUlpCOVN2d3ZMb0h0R2NMSmZOQ09XXHJcbmlES1pZQlRHTzNxMDR4SW1yQkphTlJLSCtkVmF1dWt0c3JDU29kZDI4Vk1OdHNIYUlUVmZSZlprYzc0OU5RaVlcclxuZHpGSTQ4N2FJTjZ4cC9lOGtwVjhKSlF2aXdDWGNCcDlDNExzZGdjZjFxOWQzbmpDWUtNc21FVjJNYXVQeUlRaFxyXG5Wd0lEQVFBQm95QXdIakFPQmdOVkhROEJBZjhFQkFNQ0JzQXdEQVlEVlIwVEFRSC9CQUl3QURBTkJna3Foa2lHXHJcbjl3MEJBUXNGQUFPQ0FRRUFRWDk3aWVhS2NLYlV5SFk2aFNSMW9kdUZ2aWFNcmkvM0N6VHZiOWN6R0NJcC8wd0ZcclxuaTJteVRyRjF2elZ6VG8rdTRHaUpNT0dxVnRpNmJoczhKZHFuNHNhblhEL0FtcHhrWGlvWUszLzJOTlhMQndRSlxyXG51eFhxMlBOb0pxc0hlU2w4WTNtWjZTYXd4eGdpK0RvVmpxNmtMN1RrN3lSQnhkVkhoT2N0RVY5RE9oUGV1ampvXHJcbkgyNFVMVXQya25QejF0UkdwcEFRR3Z5MnVGbFp4dXZRL3RMdnB3aEw0R3Jmd1JJeUFvRGM5TElEQWp6NXBLeVVcclxuVk9WZklLY2FQV1NGWjZiT0JDd1JLZmFsVVh2SUtBYkRURmMzcXV0WDlDdEllNnFvcnhOcXQySnN3NndMcGNGaVxyXG5qRzVnb3VISCs2b0MzUmdWUVpjNXp3WVBhTnV1cUpyR2J2VVZtdz09XHJcbi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS1cclxuIiwiZ3JhY2VQZXJpb2QiOiI5MDAiLCJlZWlkIjoiNGJiZDI4ODdlOWE4NDg5Mjg0ODdiOTk0OTBjMjI5NmUiLCJ0ZXN0Ijp0cnVlLCJhbGlhcyI6IkZSLUNULTF8RlItTVUtOTk5OSIsImVuY3J5cHRpb25QYXJhbWV0ZXJzIjp7InEiOiI4MTg1MjU5NDk3MTU5NzkzMzgwMTU5ODk1NzYzMTQ2NzY3NjYzNzg4MjE5MzIzMzkxMDY5NzA5OTIzMDAyMDkwNDE4NTUxNzYzNTY0NTE3OTc3MzcxMDIxNzk1MzA0NzEwNjg0ODMyOTcyMzczMjkzOTQyOTA3NDYwNDI1ODQ3MjczMjI4NDQ1ODgzODIyNDcyNzI5NTYyMjExMjc2ODgxNzA4MjkzMjU3NjY5OTg5MDA3MDc3MjI2MDc5ODQzNTU0NTgwNTQ1MzE3NjgzODAwMTc0NjMyNDY3NDYyMDcwODczMDQxMDMwMTc2NzQxNjUzNDI3Njc2MDk2MTc5MzY2MjI1OTc3NjE2MDAwMjk2ODg4Nzc3MjE1ODk5NDkwNzg3MjY0OTI3MTU3MzI1NTQ2MDQzMjQ0MjEzMTk1Mzg4NDA1NjgzNTYyNTA0Nzc1NjczMDQ0NjU5NjU3NTU1NzU0NjM4NjczNTU4NzMzNTUzOTU3MDM2ODE5NzI4NDAyNTc5NTQ3MjgxMjk2OTc3MDk3OTgwMjY1NTY4MDI2MTA0MDA5NjcxNjk2NDUzNDA4MDAwNTA4NzQ0MDI1NjgzMjU5MDYxMjAyNDA5OTgzNjAyMzAwNzEzNjUyMTMzNjkwMTE5MTMxOTU2OTQ2MzI5NDc1MTQwNzk2ODc3OTQ3MzczNjY5NTYzMjY1NTA5MDEzMzk5NDkxMzkyNjY1NTM5NTMyNTYzMTg3NzI3NjQ2NzA0NTMyNzcwMzY1ODIzNDY5OTA0MzIwMTM2Njk2OTI3NjI4MTE1NDEwMjU0NjA4NzA1NzU1MDI5Mzc5IiwiZyI6IjIiLCJwIjoiMTYzNzA1MTg5OTQzMTk1ODY3NjAzMTk3OTE1MjYyOTM1MzUzMjc1NzY0Mzg2NDY3ODIxMzk0MTk4NDYwMDQxODA4MzcxMDM1MjcxMjkwMzU5NTQ3NDIwNDM1OTA2MDk0MjEzNjk2NjU5NDQ3NDY1ODc4ODU4MTQ5MjA4NTE2OTQ1NDY0NTY4OTE3Njc2NDQ5NDU0NTkxMjQ0MjI1NTM3NjM0MTY1ODY1MTUzMzk5NzgwMTQxNTQ0NTIxNTk2ODcxMDkxNjEwOTA2MzUzNjc2MDAzNDkyNjQ5MzQ5MjQxNDE3NDYwODIwNjAzNTM0ODMzMDY4NTUzNTIxOTIzNTg3MzI0NTE5NTUyMzIwMDA1OTM3Nzc1NTQ0MzE3OTg5ODE1NzQ1Mjk4NTQzMTQ2NTEwOTIwODY0ODg0MjYzOTA3NzY4MTEzNjcxMjUwMDk1NTEzNDYwODkzMTkzMTUxMTE1MDkyNzczNDcxMTc0NjcxMDc5MTQwNzM2Mzk0NTY4MDUxNTkwOTQ1NjI1OTM5NTQxOTU5NjA1MzExMzYwNTIyMDgwMTkzNDMzOTI5MDY4MTYwMDEwMTc0ODgwNTEzNjY1MTgxMjI0MDQ4MTk5NjcyMDQ2MDE0MjczMDQyNjczODAyMzgyNjM5MTM4OTI2NTg5NTAyODE1OTM3NTU4OTQ3NDczMzkxMjY1MzEwMTgwMjY3OTg5ODI3ODUzMzEwNzkwNjUxMjYzNzU0NTUyOTM0MDkwNjU1NDA3MzE2NDY5Mzk4MDg2NDAyNzMzOTM4NTUyNTYyMzA4MjA1MDkyMTc0MTE1MTAwNTg3NTkifSwid3JpdGVJbkFscGhhYmV0IjoiSXlBbktDa3NMUzR2TURFeU16UTFOamM0T1VGQ1EwUkZSa2RJU1VwTFRFMU9UMUJSVWxOVVZWWlhXRmxhWVdKalpHVm1aMmhwYW10c2JXNXZjSEZ5YzNSMWRuZDRlWHJDb01LaXhhREZvY1c5eGI3RmtzV1R4YmpEZ01PQnc0TERnOE9FdzRYRGhzT0h3NGpEaWNPS3c0dkRqTU9OdzQ3RGo4T1F3NUhEa3NPVHc1VERsY09XdzVqRG1jT2F3NXZEbk1PZHc1N0RuOE9ndzZIRG9zT2p3NlREcGNPbXc2ZkRxTU9wdzZyRHE4T3N3NjNEcnNPdnc3RERzY095dzdQRHRNTzF3N2JEdU1PNXc3ckR1OE84dzczRHZzTy8iLCJjb25maXJtYXRpb25SZXF1aXJlZCI6dHJ1ZSwiZW5kRGF0ZSI6IjIwMTctMTItMzFUMjE6NTlaIiwiYmlkIjoiYTUwZGYzZTM0YWUzNDczMTkzMzM1M2E3NWFjNDU1YzYiLCJzdGFydERhdGUiOiIyMDE2LTEyLTMxVDIyOjAwWiIsImVsZWN0b3JhbEF1dGhvcml0eUlkIjoiMzFhMzJmYmNhYzg2NGI4ZWE5NmM0OGE1MDU0Mzc3MjEifX0.pkLzWYTKJwy3DdF1n-ChEeAeuc_ppOFjk7UTIrWvmLR7oOVUNX0mxMQTnTzaz6jKnbhfPsqNB_XIjCuMOD_ZiHD0motwIsNkCE6I90LWvsIy5Q_gbgB2IevdrEva5QDRgMopPipHRAy07GCx63cd0vVs5c8IJ7Lzn1VTlT2lTyb9tFmrrMWLejYTtqzY0-ANeFL1LHNtBoE9za6GoTcJW_qM0ZRddVU0Kqc6ltRg5L4pMNdKZSBVlICVAnJYh9Z0I3b7Xr5gz2mPhceiXOsWiQTjZwfaeg81JhN4JleUW_jOm7JGLBiZTlP_CyQ2fM-GRcNkHWJVBCkIm249je9B6A";
	private static final String ELECTION_PUBLIC_KEY_JSON = "{\"publicKey\":\"eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJnIjoiQWc9PSIsInAiOiJBSUd0OHd6aXp5SjgraWE3MTZZZ1k0RmpHNWdKUmpmeitpQjNPZVdFTUxDT0xub1dHMHdId0ZReW8yVk54NGxnQlBEbVNJYjVOdEovMTJVM2RmZTdKZGdZakVNU2F6OUhVcmZvd0hOd0N1RDFWVm1EWHhVdnl4UFNWNS9pc3M3YTg1c29wdlI5bjA5aW12dnA4WERmUE51Y2NkMTBUUXdHU0tWdDJ2Tjd2U01yZ1FPc0dFdUJCc0Vtc2RsUmlsNCt4OU5UWXBoc2MvN3ZUVjlwUk40VStaSTV5VGJCb2Y2U0ZjSDVtd0FhdGxaQi9SenR3QTF3aDNxVVVneUx6L2tUS3FUUmJhUGM1VzErTERQQlpuTWlhVFdxNzg2NDE4cGlvNG9NODZtVkl0aGZFLzM0UzBrbjhHcEpHek5GMTJOemJWUXpicHY2WXBtclBLRzFGZmFRUHdjPSIsInEiOiJRTmI1aG5GbmtUNTlFMTNyMHhBeHdMR056QVNqRy9uOUVEdWM4c0lZV0VjWFBRc05wZ1BnS2hsUnNxYmp4TEFDZUhNa1EzeWJhVC9yc3B1Nis5MlM3QXhHSVlrMW42T3BXL1JnT2JnRmNIcXFyTUd2aXBmbGlla3J6L0ZaWjIxNXpaUlRlajdQcDdGTmZmVDR1RytlYmM0NDdyb21oZ01rVXJidGViM2VrWlhBZ2RZTUpjQ0RZSk5ZN0tqRkx4OWo2YW14VERZNS8zZW1yN1NpYndwOHlSemttMkRRLzBrSzRQek5nQTFiS3lEK2puYmdCcmhEdlVvcEJrWG4vSW1WVW1pMjBlNXl0cjhXR2VDek9aRTBtdFYzNTF4cjVURlJ4UVo1MU1xUmJDK0ovdndscEpQNE5TU05tYUxyc2JtMnFobTNUZjB4VE5XZVVOcUsrMGdmZ3c9PSJ9LCJlbGVtZW50cyI6WyJhVnRJZ3NiaTJqS1JGVVJDK0dIM2pXVlJ1cnVsVWtVUHBDb0NzRmJkeXJ0TU12ZlZXc0VhQjExZTFPTSszNjFmWXRvR29HMGdmM1d5QnJhR2NIcTRNdjhmSmxzYmpldTdmRkgxejI1NCs4V2FVd0U4b2ZBMXluOXlTbDh4bkRvTnQxejhZT0lzNHdYY3NZL0oySFI2bXdqZCt2aE9hai9jRW4wOHQ4OHo3TWsvalpmb2xxNUIyNTBCUlp4RythcjZrVXNFRHlnUkI4Z2E4blJkNnhYWFl0QnRtandwWkQzQWZwM0xtekFLa1ZTWVJlTXRCOXh5ZlhIUjhPNHF2MWs4bEtVNHJnTUhwMGhmUkhVL1NXWkVJcXZXMi9oMTdnS3Z4amk2bXc3bVJUQVZEdk1Ra1NaYndTUlBkMXRmbmxqOWZTOHRmS2FJZy93SEdmYlppbXVzQlE9PSJdfX0=\",\"id\":\"31a32fbcac864b8ea96c48a505437721\"}";
	private static final String PERSISTENCE_CONTEXT_UNIT_NAME = "persistenceUnitJdbc";
	private static final String VOTING_CARD_ID_TEMPLATE = "votingCardId";
	private static final String ELECTION_EVENT_TEMPLATE = "electionEventId";
	private static final String TENANT_TEMPLATE = "tenantId";
	private static final String BALLOT_BOX_TEMPLATE = "ballotBoxId";
	private static final String TEST_TENANT_ID = "100";
	private static final String TEST_VOTING_CARD_ID = "31a32fbcac864b8ea96c48a505437721";
	private static final String TEST_BALLOT_BOX_ID = "7f59b097dcc6407e933fdfa94b9812b7";
	private static final String TEST_BALLOT_ID = "ballotid";
	private static final String TEST_ELECTION_EVENT_ID = "4bbd2887e9a848928487b99490c2296e";
	private static final String TEST_ENCRYPTED_VOTE = "TESTa390x283480ebdhfwifyqsdjugsd027tgrd";

	private static final Map<String, Object> templates;

	static {
		templates = new HashMap<>();
		templates.put(TENANT_TEMPLATE, TEST_TENANT_ID);
		templates.put(ELECTION_EVENT_TEMPLATE, TEST_ELECTION_EVENT_ID);
		templates.put(VOTING_CARD_ID_TEMPLATE, TEST_VOTING_CARD_ID);
		templates.put(BALLOT_BOX_TEMPLATE, TEST_BALLOT_BOX_ID);
	}

	@PersistenceContext(unitName = PERSISTENCE_CONTEXT_UNIT_NAME)
	EntityManager entityManager;
	@Resource
	UserTransaction userTransaction;

	@Before
	public void cleanup()
			throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		userTransaction.begin();
		entityManager.createQuery("DELETE FROM CleansedBallotBox").executeUpdate();
		entityManager.createQuery("DELETE FROM BallotBoxInformation").executeUpdate();
		entityManager.createQuery("DELETE FROM BallotBoxContent").executeUpdate();
		entityManager.createQuery("DELETE FROM ElectionPublicKey").executeUpdate();
		userTransaction.commit();
	}

	@Test
	public void testGetCleansedBallotBoxSuccessful(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget) throws Exception {

		storeTestData(true);

		ClientResponse response = (ClientResponse) webTarget.path(GET_CLEANSED_BALLOT_BOX_PATH).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).header(TRACK_ID_HEADER, TEST_TRACK_ID).get();

		assertThat(response.getStatus(), is(200));

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode rootNode = mapper.createObjectNode();
		rootNode.put("ballotBoxId", TEST_BB_ID);
		rootNode.put("empty", false);

		assertThat(response.readEntity(String.class), is(rootNode.toString()));
	}

	@Test
	public void testGetBallotBoxInformation(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget) throws Exception {

		storeBallotBoxInformation();
		storeElectionPublicKey();

		ClientResponse response = (ClientResponse) webTarget.path(GET_BALLOT_BOX_INFORMATION_PATH).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(TRACK_ID_HEADER, TEST_TRACK_ID).get();

		assertThat(response.getStatus(), is(200));

	}

	private void storeBallotBoxInformation() throws Exception {

		BallotBoxInformation ballotBoxInformation = new BallotBoxInformation();
		ballotBoxInformation.setTenantId(TEST_TENANT_ID);
		ballotBoxInformation.setElectionEventId(TEST_ELECTION_EVENT_ID);
		ballotBoxInformation.setBallotBoxId(TEST_BB_ID);
		ballotBoxInformation.setJson(BALLOT_BOX_INFORMATION_JSON);
		ballotBoxInformation.setSignature(BALLOT_BOX_INFORMATION_SIGNATURE);

		saveEntity(ballotBoxInformation);
	}

	private void storeElectionPublicKey() throws Exception {
		ElectionPublicKey electionPublicKey = new ElectionPublicKey();
		electionPublicKey.setElectionEventId(TEST_ELECTION_EVENT_ID);
		electionPublicKey.setTenantId(TEST_TENANT_ID);
		electionPublicKey.setElectoralAuthorityId(TEST_ELECTORAL_AUTHORITY_ID);
		electionPublicKey.setJson(ELECTION_PUBLIC_KEY_JSON);

		saveEntity(electionPublicKey);
	}

	private void storeTestData(boolean closedBallotBox) throws Exception {

		CleansedBallotBox cleansedBallotBox = new CleansedBallotBox();
		cleansedBallotBox.setBallotBoxId(TEST_BB_ID);
		cleansedBallotBox.setBallotId(TEST_BALLOT_ID);
		cleansedBallotBox.setElectionEventId(TEST_ELECTION_EVENT_ID);
		cleansedBallotBox.setEncryptedVote(TEST_ENCRYPTED_VOTE);
		cleansedBallotBox.setTenantId(TEST_TENANT_ID);
		cleansedBallotBox.setVotingCardId(TEST_VC_ID);

		saveEntity(cleansedBallotBox);

		JsonObjectBuilder ballotBoxInfoJson = Json.createObjectBuilder();
		ballotBoxInfoJson.add(JSON_PARAMETER_DATE_FROM,
				ZonedDateTime.now(ZoneOffset.UTC).minusDays(-1).format(DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)));

		if (closedBallotBox) {
			ballotBoxInfoJson
					.add(JSON_PARAMETER_DATE_TO, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)));
		} else {
			ballotBoxInfoJson.add(JSON_PARAMETER_DATE_TO,
					ZonedDateTime.now(ZoneOffset.UTC).plusDays(7).format(DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)));
		}

		ballotBoxInfoJson.add(JSON_PARAMETER_GRACE_PERIOD, "0");
		ballotBoxInfoJson.add(JSON_PARAMETER_TEST, false);

		BallotBoxInformation ballotBoxInformation = new BallotBoxInformation();
		ballotBoxInformation.setBallotBoxId(TEST_BB_ID);
		ballotBoxInformation.setElectionEventId(TEST_ELECTION_EVENT_ID);
		ballotBoxInformation.setJson(ballotBoxInfoJson.build().toString());
		ballotBoxInformation.setSignature(TEST_SIGNATURE);
		ballotBoxInformation.setTenantId(TEST_TENANT_ID);

		saveEntity(ballotBoxInformation);

		JsonObjectBuilder ballotBoxContentJson = Json.createObjectBuilder();
		ballotBoxContentJson.add(JSON_PARAMETER_ELECTION_EVENT, Json.createObjectBuilder().add(JSON_PARAMETER_ID, TEST_ELECTION_EVENT_ID));
		ballotBoxContentJson.add(JSON_PARAMETER_PASSWORD_KEYSTORE, "???");
		ballotBoxContentJson.add(JSON_PARAMETER_ID, TEST_BB_ID);

		BallotBoxContent ballotBoxContent = new BallotBoxContent();
		ballotBoxContent.setBallotBoxId(TEST_BB_ID);
		ballotBoxContent.setElectionEventId(TEST_ELECTION_EVENT_ID);
		ballotBoxContent.setJson(ballotBoxContentJson.build().toString());
		ballotBoxContent.setTenantId(TEST_TENANT_ID);

		saveEntity(ballotBoxContent);

		storeElectionPublicKey();
	}

	private <T> void saveEntity(T entityToSave) throws Exception {
		userTransaction.begin();
		entityManager.persist(entityToSave);
		userTransaction.commit();
	}
}
