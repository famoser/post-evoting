/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.DynamicElectionInformationContentFactory;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MaxNumberOfAllowedVotesTest {

	private static final String tenantId = "100";
	private static final String electionEventId = "100";
	private static final String votingCardId = "100";
	private static final String ballotBoxId = "100";
	private static final String ballotId = "100";
	private final String authenticationToken = "{\"id\": \"lnniWSgf+XDd4dasaIX9rQ==\",\"voterInformation\": {\"electionEventId\": \"100\","
			+ "\"votingCardId\": \"100\",\"ballotId\": \"100\",\"verificationCardId\": \"100\",\"tenantId\":\"100\",\"ballotBoxId\": \"100\",\"votingCardSetId\": \"100\",\"credentialId\":\"100\","
			+ "\"verificationCardSetId\": \"100\"},\"timestamp\": \"1430759337499\",\"signature\": \"base64encodedSignature==\"}";
	@InjectMocks
	private final MaxNumberOfAllowedVotes rule = new MaxNumberOfAllowedVotes();
	private Vote vote;
	private String voteJson;
	@Mock
	private ElectionInformationContentRepository electionInformationContentRepository;

	@Mock
	private BallotBoxRepository ballotBoxRepository;

	@Mock
	private Logger LOGGER;

	@Spy
	private DynamicElectionInformationContentFactory dynamicElectionInformationContentFactory;

	@Before
	public void setup() throws JsonProcessingException {
		vote = new Vote();
		vote.setTenantId(tenantId);
		vote.setElectionEventId(electionEventId);
		vote.setVotingCardId(votingCardId);
		vote.setBallotBoxId(ballotBoxId);
		vote.setBallotId(ballotId);
		vote.setAuthenticationToken(authenticationToken);
		vote.setCertificate("Cert");
		vote.setCipherTextExponentiations("cipher");
		vote.setCredentialId("100");
		vote.setEncryptedOptions("opts");
		vote.setEncryptedPartialChoiceCodes("partial");
		vote.setSignature("sign");

		ObjectMapper mapper = new ObjectMapper();
		voteJson = mapper.writeValueAsString(vote);
	}

	@Test
	public void electionInformationContentNotFound() throws ResourceNotFoundException {
		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString()))
				.thenThrow(new ResourceNotFoundException("information not found"));

		assertEquals(ValidationErrorType.FAILED, rule.execute(vote).getValidationErrorType());
	}

	@Test
	public void lessThanMaxNumberOfVotesPerVotingCardId() throws ResourceNotFoundException {
		JsonObjectBuilder jsonObject = Json.createObjectBuilder();
		JsonObjectBuilder params = Json.createObjectBuilder();
		params.add("numVotesPerVotingCard", "2");
		params.add("numVotesPerAuthToken", "2");
		jsonObject.add("electionInformationParams", params);
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setJson(jsonObject.build().toString());
		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		List<BallotBox> ballotBoxList = new ArrayList<>();
		when(ballotBoxRepository
				.findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(ballotBoxList);

		assertEquals(ValidationErrorType.SUCCESS, rule.execute(vote).getValidationErrorType());
	}

	@Test
	public void moreThanMaxNumberOfVotesPerVotingCardId() throws ResourceNotFoundException {
		JsonObjectBuilder jsonObject = Json.createObjectBuilder();
		JsonObjectBuilder params = Json.createObjectBuilder();
		params.add("numVotesPerVotingCard", "2");
		params.add("numVotesPerAuthToken", "2");
		jsonObject.add("electionInformationParams", params);
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setJson(jsonObject.build().toString());
		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		List<BallotBox> ballotBoxList = new ArrayList<>();
		ballotBoxList.add(new BallotBox());
		ballotBoxList.add(new BallotBox());
		when(ballotBoxRepository
				.findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(ballotBoxList);

		assertEquals(ValidationErrorType.FAILED, rule.execute(vote).getValidationErrorType());
	}

	@Test
	public void lessThanMaxNumberOfVotesPerAuthToken() throws ResourceNotFoundException {
		JsonObjectBuilder jsonObject = Json.createObjectBuilder();
		JsonObjectBuilder params = Json.createObjectBuilder();
		params.add("numVotesPerVotingCard", "2");
		params.add("numVotesPerAuthToken", "2");
		jsonObject.add("electionInformationParams", params);
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setJson(jsonObject.build().toString());
		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		List<BallotBox> ballotBoxList = new ArrayList<>();
		BallotBox bb = new BallotBox();
		bb.setVote(voteJson);
		ballotBoxList.add(bb);
		when(ballotBoxRepository
				.findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(ballotBoxList);

		assertEquals(ValidationErrorType.SUCCESS, rule.execute(vote).getValidationErrorType());
	}

	@Test
	public void moreThanMaxNumberOfVotesPerAuthToken() throws ResourceNotFoundException {
		JsonObjectBuilder jsonObject = Json.createObjectBuilder();
		JsonObjectBuilder params = Json.createObjectBuilder();
		params.add("numVotesPerVotingCard", "2");
		params.add("numVotesPerAuthToken", "1");
		jsonObject.add("electionInformationParams", params);
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setJson(jsonObject.build().toString());
		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		List<BallotBox> ballotBoxList = new ArrayList<>();
		BallotBox bb = new BallotBox();
		bb.setVote(voteJson);
		ballotBoxList.add(bb);
		when(ballotBoxRepository
				.findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(ballotBoxList);

		assertEquals(ValidationErrorType.FAILED, rule.execute(vote).getValidationErrorType());
	}

	@Test
	public void moreThanMaxNumberOfVotesPerAuthTokenButNotEquals() throws ResourceNotFoundException, JsonProcessingException {
		String authenticationToken = "{\"id\": \"lnniWSgf+XDd4dasaIX9rM==\",\"voterInformation\": {\"electionEventId\": \"100\","
				+ "\"votingCardId\": \"100\",\"ballotId\": \"100\",\"verificationCardId\": \"100\",\"tenantId\":\"100\",\"ballotBoxId\": \"100\",\"votingCardSetId\": \"100\",\"credentialId\":\"100\","
				+ "\"verificationCardSetId\": \"100\"},\"timestamp\": \"1430759337488\",\"signature\": \"base64encodedSignature==\"}";
		Vote vote2 = new Vote();
		vote2.setTenantId(tenantId);
		vote2.setElectionEventId(electionEventId);
		vote2.setVotingCardId(votingCardId);
		vote2.setBallotBoxId(ballotBoxId);
		vote2.setBallotId(ballotId);
		vote2.setAuthenticationToken(authenticationToken);

		ObjectMapper mapper = new ObjectMapper();
		String vote2Json = mapper.writeValueAsString(vote2);

		JsonObjectBuilder jsonObject = Json.createObjectBuilder();
		JsonObjectBuilder params = Json.createObjectBuilder();
		params.add("numVotesPerVotingCard", "2");
		params.add("numVotesPerAuthToken", "1");
		jsonObject.add("electionInformationParams", params);
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setJson(jsonObject.build().toString());
		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		List<BallotBox> ballotBoxList = new ArrayList<>();
		BallotBox bb = new BallotBox();
		bb.setVote(vote2Json);
		ballotBoxList.add(bb);
		when(ballotBoxRepository
				.findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(ballotBoxList);

		assertEquals(ValidationErrorType.SUCCESS, rule.execute(vote).getValidationErrorType());
	}

	@Test
	public void moreThanMaxNumberOfVotesPerAuthTokenButNotEqualsVoteInString() throws ResourceNotFoundException {

		String vote2Json = "{\"vote\":{\"tenantId\":\"100\",\"electionEventId\":\"71f8018b6c0a4c8fada7146aa4b93b72\",\"ballotId\":\"b10190c318d6462fa50f6c46244017eb\",\"ballotBoxId\":\"035355528a5e4f98b49fd77a2573c842\",\"votingCardId\":\"ac2e53fe2513dcc8a891ee408a950815\",\"encryptedOptions\":\"4484657043701248415542756884372948477587263601556174801614631961187034825267277834049929074485597353123975122149638853106910926116936602465767755226191813051406576400209638194876242483773853518154787784014636167584770234696081719467017943258373468139105213711679593972437866701209676956987834233461356524115225362757109732082435234569024094194987043725264535809478222122060299942748874974544152572965912476450184363978576114534908408765000597141716283383761625540749115536726699660146012552478101247918840115731020615217841531076202284530114170258066720854332576879415374873354879130166359793397858787926835495483875;11692953471742570015738859431517073072331723077989255754301204258052406122267863816401300216939198893646955111374414987099344806068325286794687078407393565185121644557008088085390587480616309042232527339496058003184908127651333410276073225360146251017869392603232968378947064026986738506718072639290140556554671553754036654812842355865521780609517871793308679351272646764837649225061192108015308964835312189524446913584722498650290503367215652451886854079639713439527467440827576131040870129167935939080450710769521958391989263384750928911808481841058330649288260517124313869068890712867374636087355203700113629103094\",\"encryptedPartialChoiceCodes\":\"4007364569766278862933080827241152413280470800105047782935238670471398779709421039806634802763740705872471715808907614220655810631647073516367718061343961718195492883930696071059572493365039983873209552451054116641574023827359443591320212881206355403876462730434181388695375034838720776674650596917413017216434943177953744831707217623945613164420337066149074623422701257588654384346360067296814724072902880642587613864789255624990203548716070163445815429369623481979563781877257258592726602884775325172044687094343931516838029263332639619469350328630892205017045703361001606522888926423733652316220621979815002849459;13475724599362668162940392615556691473646099559048913884669882732794849997717962433942278524986547098508798633657677496093810437301217792431649507907559758472150619364171367750924237979099106374694389476419771724354788477082765398459240940108492954240977328628400423815229163889992119673187439464129144726485960352393331632111149408386846806524374003174791400523431549162517474262537544141131106825142964979386549185600419582765533373175430291551601022171835376031163279045054516331906520592545357664764097178683187107406957678272862223036787423213217529395394135655316468908056449415358584735924030691657146797330104\",\"verificationCardPublicKey\":\"eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJnIjoiMiIsInAiOiIxNjM3MDUxODk5NDMxOTU4Njc2MDMxOTc5MTUyNjI5MzUzNTMyNzU3NjQzODY0Njc4MjEzOTQxOTg0NjAwNDE4MDgzNzEwMzUyNzEyOTAzNTk1NDc0MjA0MzU5MDYwOTQyMTM2OTY2NTk0NDc0NjU4Nzg4NTgxNDkyMDg1MTY5NDU0NjQ1Njg5MTc2NzY0NDk0NTQ1OTEyNDQyMjU1Mzc2MzQxNjU4NjUxNTMzOTk3ODAxNDE1NDQ1MjE1OTY4NzEwOTE2MTA5MDYzNTM2NzYwMDM0OTI2NDkzNDkyNDE0MTc0NjA4MjA2MDM1MzQ4MzMwNjg1NTM1MjE5MjM1ODczMjQ1MTk1NTIzMjAwMDU5Mzc3NzU1NDQzMTc5ODk4MTU3NDUyOTg1NDMxNDY1MTA5MjA4NjQ4ODQyNjM5MDc3NjgxMTM2NzEyNTAwOTU1MTM0NjA4OTMxOTMxNTExMTUwOTI3NzM0NzExNzQ2NzEwNzkxNDA3MzYzOTQ1NjgwNTE1OTA5NDU2MjU5Mzk1NDE5NTk2MDUzMTEzNjA1MjIwODAxOTM0MzM5MjkwNjgxNjAwMTAxNzQ4ODA1MTM2NjUxODEyMjQwNDgxOTk2NzIwNDYwMTQyNzMwNDI2NzM4MDIzODI2MzkxMzg5MjY1ODk1MDI4MTU5Mzc1NTg5NDc0NzMzOTEyNjUzMTAxODAyNjc5ODk4Mjc4NTMzMTA3OTA2NTEyNjM3NTQ1NTI5MzQwOTA2NTU0MDczMTY0NjkzOTgwODY0MDI3MzM5Mzg1NTI1NjIzMDgyMDUwOTIxNzQxMTUxMDA1ODc1OSIsInEiOiI4MTg1MjU5NDk3MTU5NzkzMzgwMTU5ODk1NzYzMTQ2NzY3NjYzNzg4MjE5MzIzMzkxMDY5NzA5OTIzMDAyMDkwNDE4NTUxNzYzNTY0NTE3OTc3MzcxMDIxNzk1MzA0NzEwNjg0ODMyOTcyMzczMjkzOTQyOTA3NDYwNDI1ODQ3MjczMjI4NDQ1ODgzODIyNDcyNzI5NTYyMjExMjc2ODgxNzA4MjkzMjU3NjY5OTg5MDA3MDc3MjI2MDc5ODQzNTU0NTgwNTQ1MzE3NjgzODAwMTc0NjMyNDY3NDYyMDcwODczMDQxMDMwMTc2NzQxNjUzNDI3Njc2MDk2MTc5MzY2MjI1OTc3NjE2MDAwMjk2ODg4Nzc3MjE1ODk5NDkwNzg3MjY0OTI3MTU3MzI1NTQ2MDQzMjQ0MjEzMTk1Mzg4NDA1NjgzNTYyNTA0Nzc1NjczMDQ0NjU5NjU3NTU1NzU0NjM4NjczNTU4NzMzNTUzOTU3MDM2ODE5NzI4NDAyNTc5NTQ3MjgxMjk2OTc3MDk3OTgwMjY1NTY4MDI2MTA0MDA5NjcxNjk2NDUzNDA4MDAwNTA4NzQ0MDI1NjgzMjU5MDYxMjAyNDA5OTgzNjAyMzAwNzEzNjUyMTMzNjkwMTE5MTMxOTU2OTQ2MzI5NDc1MTQwNzk2ODc3OTQ3MzczNjY5NTYzMjY1NTA5MDEzMzk5NDkxMzkyNjY1NTM5NTMyNTYzMTg3NzI3NjQ2NzA0NTMyNzcwMzY1ODIzNDY5OTA0MzIwMTM2Njk2OTI3NjI4MTE1NDEwMjU0NjA4NzA1NzU1MDI5Mzc5In0sImVsZW1lbnRzIjpbIjQ4NDQ4Nzk4NDM3MTA5MzIwMjk5NzIwNzAzOTU3OTU4MTI2NDQzMjk2MTAxODc4NTQ2MzEzMDU1NTI3MTgxMzI1NjQ0MjQxNzIzNjA0NzEyODQ4NzYxNzc5NjY5MDI1MDk0MTEwMjkxOTc1ODU2NzcwNjY1NzM3MDczNTI5ODYzMDgxNjQ0NjczMDE2NDc0ODAyMDkwMDQ3NzI5OTg0NTAzMDM2MjkyMTI0MjkzOTgwOTEzNTEwMjE3NjU4OTMyMDk2Mzk1MzA5Njc4ODM4MTkyMjU2NjEzNzc1MDIxNDI1MzE1ODc0MTc0ODg1MDg5MjYzOTY5NjM2Mjg1ODc4NTc5NjU4NjI3NTE3MTIyMDE0MzY3MTg3MTM1MTI0ODUxNjAwMTUzOTAzNDI1MjQzMDYwMTk1Mjk4NjAyODAxOTE2MjM3MjQxODMwNTM1MzA2MzM2NDM5NDE4MjIzOTkwODI2NTAxMjM1OTY3NjcxNDQwMTkyMzMyOTcwNDQxOTI1NjYzMDM0OTM4MjcxMTQ4MzExMjE1NDU4NzIyNTU5NTkyMTYzMDc1NzQ3Njk0MDQ0MDIxNzUyMjAyNjkyODcxNTU4NTYzMzc1MDk4NDI0MDkyMzYyOTY4MjQzOTQyOTkxNzcwMTA3MTk3MDU1NDExMDY3MjgzODc4MzEwMDg2NjIxNTk0NjkyODM2MjQ3NDkxMTU1NTA5NTA3OTk2MTMzNDcyNTQyMDk0Mjc0Mjc3NDU4ODAzNzY4MDQyOTA2MjgyMDUzNjQ2NjA2ODgzMDg5Mzk0NjM3Mzk5MDA4NDQ5MzEzODQxODAzMDMiXX19\",\"verificationCardPKSignature\":\"OWAoUpAQ5vyaNQUYD1r4zX72sKtw7sva1BDHxusfmx3HKV7mIbbUefyEeeKIUBmSIgKRLFnNnNSjEBx5ouvMzmeeCuPDWmFYwoDQNzWuESnoJRBji+FOBVFKWhLAdjFmLxvv84GKHWkCIHR7MwzSHPG1/0rFPfTyg+cJ65t36G6xb184JM2Vk2qQCv/XeulvQN3UWqWPCkAAjFC1XhzHrkE0aFwJQS7tto3PDZc0v8+Wl49KCXTEunBVEgpp5NgD2752Gh+mjoSxFVT1fJ63QzYdfAC+cm8Npg1kg+pCuDN48Ys8JdiRDtBCiNvDTD081aAZ81OOClWidaaM6KMc5w==\",\"signature\":\"GGnb0bH0VkhaWYlGlMZaXHANxKHPC097RMBQWt8hj8wwxblMbvgqXj+yKgHhjFRY0dsURppI6IEIisx3X3LjFxONndSEmqNRFqHNdGwR2TyCt4RxQqQLdfC1dcL0qPyFKIpVMITWNkK/uyl0cjysrY5VmOXhzlxjDSg6z0zNcEopGGJxxOrdAAwNWx/Bc9tM6x+dULZfoR+dGVdTo6TycUvR5seYnmYZbBSQW1gd6vbPFs3GH+NDb1stJjkm+GE7MzPulecmE3YiStF9JkOvv7acVZJItwRB5jOnWsbav1gkkfmuJhLaOD4bVeFdMf2o+BSM4dW3ELEAc4b31w5MVA==\",\"certificate\":\"-----BEGIN CERTIFICATE-----\\r\\nMIIDoDCCAoigAwIBAgIUBBMq8qwSK/UNvpFSdETv3UII5IowDQYJKoZIhvcNAQEL\\r\\nBQAwfjE4MDYGA1UEAwwvQ3JlZGVudGlhbHMgQ0EgNzFmODAxOGI2YzBhNGM4ZmFk\\r\\nYTcxNDZhYTRiOTNiNzIxFjAUBgNVBAsMDU9ubGluZSBWb3RpbmcxEjAQBgNVBAoM\\r\\nCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDAeFw0xNjAxMTEwOTU5\\r\\nMTFaFw0xNzAxMTEwODAwMDBaMHQxLjAsBgNVBAMMJVNpZ24gNDkxODJjMmI0OWZl\\r\\nNDg1Mzk2ZDRjMWI3ZTc0NDFmZDExFjAUBgNVBAsMDU9ubGluZSBWb3RpbmcxEjAQ\\r\\nBgNVBAoMCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJDSDCCASIwDQYJ\\r\\nKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK3zkGHWb/BQOFFd3f4K6JWDRPYJA914\\r\\naAFYHSsBnyyge3u2EuT+f3NfdoQNfB36FkLVC85Wrn1qVdeRJ3tEnN1FCKsIbki0\\r\\n6zDvzi1sSqFCI3QGPVX5HLWuzXpSVu0cjeIIyvCLTdSHA2G30XwZj7jGvlA9e0Ht\\r\\nuFYYMUsyC0AuqLxPTJ/iC+LY2ljxBHWNO1Ce7uPoKBwTz2ycVMeSdDuC4DFei/Cr\\r\\nVFCepf6Sanl/yA6Mlw2P0+X4+mhhFdtBrOe224o0O8p3HFM18RThZOKJOrIP9EnF\\r\\n+cJw2b0u3kHP4A6N0RM2kR39lx2MTG8maELBWT7GzoGpRwxslcu3dxUCAwEAAaMg\\r\\nMB4wDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBsAwDQYJKoZIhvcNAQELBQAD\\r\\nggEBAAU6uq/t5rWvnFnRmdQ0KIoDxiEIzvqFDHxtU1ol6d0iSqHslzmhSHhA8C7u\\r\\nraf4JpZ5lEzs+9HY3fdWXbSOs/raxyS8vGqqseKz9zq4pzY4vSbhzLXLA/rKDceP\\r\\nBdl+TbstOhPIy1+WZ2/qqhlK2V4QBNelx4XCXf1nIa0ecBA0/Q1ryGvMqV86MJ3f\\r\\nhc+m6dyOEboabmJlUwNrVAEIMT9p33HXDxhCOBA1BcuJgYoUNNa7koO1FLXUy/dE\\r\\nGVlANsYPM7OsdEQADWK4UX4T/TjEnbBvCvk3rSgpg0gjB/gf7bx4pOtim7SDLYx2\\r\\n4HSRoVQuUZrixQEpU+4q4JMFUD8=\\r\\n-----END CERTIFICATE-----\\r\\n\",\"credentialId\":\"49182c2b49fe485396d4c1b7e7441fd1\",\"authenticationTokenSignature\":\"GxIsu1hJiBnu/OBRSRqDOwDi55LbUh73DmK1k9gS9xD0bJe35DWI1b34/9678ZFadN4p3Zcn5zGC8MLOjQ7ZNUSknrJ98iED4NNzu+N82qfBsrTiegtWsJmY+kobsK8dvzq4MEAoNs+jitx79AcpTUbgSHUcTLH9XUF7nMNQMOBHevNf2+j93ML438H8ahNXhL4pPHphYm74rmYblrXHYzBS7yFxqB9VSyG8PCIArb2/I7QAN80m1BbTzW6iySuo7wrDS5amq9tWBiXqicv8enA68w5yRHgAfyvK5b1otx9/vFaxqFqkvZrXAXC5XVjf8ehcle6eWrVnQDpZMRiFgQ==\",\"authenticationToken\":\"{\\\"id\\\":\\\"gPXJBk/VXN1rYDi1H/5pYA==\\\",\\\"voterInformation\\\":{\\\"tenantId\\\":\\\"100\\\",\\\"electionEventId\\\":\\\"71f8018b6c0a4c8fada7146aa4b93b72\\\",\\\"votingCardId\\\":\\\"ac2e53fe2513dcc8a891ee408a950815\\\",\\\"ballotId\\\":\\\"b10190c318d6462fa50f6c46244017eb\\\",\\\"credentialId\\\":\\\"49182c2b49fe485396d4c1b7e7441fd1\\\",\\\"verificationCardId\\\":\\\"dc15d14f13234623b77cdb3e3f27d29e\\\",\\\"ballotBoxId\\\":\\\"035355528a5e4f98b49fd77a2573c842\\\",\\\"votingCardSetId\\\":\\\"d4bb487f1f424b98ad3c813e22c1a6fe\\\",\\\"verificationCardSetId\\\":\\\"0b67974c20944633ac37316d92d477c9\\\"},\\\"timestamp\\\":\\\"1452510886089\\\",\\\"signature\\\":\\\"GxIsu1hJiBnu/OBRSRqDOwDi55LbUh73DmK1k9gS9xD0bJe35DWI1b34/9678ZFadN4p3Zcn5zGC8MLOjQ7ZNUSknrJ98iED4NNzu+N82qfBsrTiegtWsJmY+kobsK8dvzq4MEAoNs+jitx79AcpTUbgSHUcTLH9XUF7nMNQMOBHevNf2+j93ML438H8ahNXhL4pPHphYm74rmYblrXHYzBS7yFxqB9VSyG8PCIArb2/I7QAN80m1BbTzW6iySuo7wrDS5amq9tWBiXqicv8enA68w5yRHgAfyvK5b1otx9/vFaxqFqkvZrXAXC5XVjf8ehcle6eWrVnQDpZMRiFgQ==\\\"}\",\"cipherTextExponentiations\":\"10295645274432500688878499908279691864116943761186898706775866549042495829447432138953632473294995919177996383661854749049570457603911125980883292109260502914981612453528725125467448246371361857724997790151845479223842111792173181966640875855042978262304803498428688165893398807106527356918691491775051910833696570040978506184495180266319827679172560341548468912424154826555959861973470938944382223087085605290237453468122179858654956269121813249304883732657065174731359210652250118426891992578055779095101488336430842279350866308855551613116610556526518821221527006371113945151571659557580021183599175179916783716716;15404281071933332645939969416812409089711167980134388919571388058832537408401451768423408112187751030408115014827683746331208085390406184171881694737926526283125620373809026441321089949884447791447990551092137900149889364806336924942042616231728664772844893992756805736029460030286172088334095587603292271636471755623464579109939019839453104384029894052292112420968753324187732824905463833976203303790573709610217782421723018877263168930325673616528496351178380278754683338464469148206565072334978751904966655496328689709779018866213231052542124245844779035269417617908722881994059431561031110388023253568674263141255\",\"exponentiationProof\":\"{\\\"zkProof\\\":{\\\"q\\\":\\\"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379\\\",\\\"hash\\\":\\\"944713616664944874492175044298695761748211367649042749313463365592\\\",\\\"values\\\":[\\\"1500879305010572192968376069098544991512682362145871617949915207375763434849620566775040447933289009694967219561977585184278341495324113534006472915510514070750615131577575065992714756475257545354734030441870731351274645068764166736009023865177741023022182451477855844357353905284164754256043673284589514771634579572963303456901365351075334941646013427276502818118409098781071582359776255403180541377335962843736285953105817217430356866728420481798591824317629435160259442280267325168204253735820436816403313731924266071735706631115723118758414048169753696788532897113111188387665166501839153536889127790117158204907\\\"]}}\",\"plaintextEqualityProof\":\"{\\\"zkProof\\\":{\\\"q\\\":\\\"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379\\\",\\\"hash\\\":\\\"5242142504318152536875596728277639078334511336934651186832058280557\\\",\\\"values\\\":[\\\"123773763923298644737518087634850647567406052860988072388985726669854436790721632334794041783593289337568146922689492225557433746123608227792273317892857510309787310497960670610016031840764771935752053317561062457639485863686445241719747262575214192167362396186961320749865294854594738929759703874366297373913057364361842590792627117857932705423181014548160334825677106062917378790858034603925450535020471761705958057136287807026552211441028948103981028070555249035361441191273719947361717260447179780181024472643598221734034288979835205445845596271954963875117849089466404936326112849976848161483187054137380633238\\\",\\\"2355339288624732490207435199737223865463233673407328104762214840340241760096388411495834277442247552877452248801491252307205147612928829021080734954192938726496421650819626806208752918199788057763146680073363526161685837628840449349461068676836791215381531801383535086472607117471325057434291435513181801336978392808753903087070765202785419470904288214414583218343575067534250302157050012965173745311623865799095831603380195395414739054817394800950771564593125903120792643355581591999269200590447147229908671189647037457635011136928139002760795280876493956698518323398450244634245402668599024626394910647545785081551\\\"]}}\"},\"timestamp\":\"1452510896133\",\"authenticationToken\":{\"id\":\"gPXJBk/VXN1rYDi1H/5pYA==\",\"voterInformation\":{\"tenantId\":\"100\",\"electionEventId\":\"71f8018b6c0a4c8fada7146aa4b93b72\",\"votingCardId\":\"ac2e53fe2513dcc8a891ee408a950815\",\"ballotId\":\"b10190c318d6462fa50f6c46244017eb\",\"credentialId\":\"49182c2b49fe485396d4c1b7e7441fd1\",\"verificationCardId\":\"dc15d14f13234623b77cdb3e3f27d29e\",\"ballotBoxId\":\"035355528a5e4f98b49fd77a2573c842\",\"votingCardSetId\":\"d4bb487f1f424b98ad3c813e22c1a6fe\",\"verificationCardSetId\":\"0b67974c20944633ac37316d92d477c9\"},\"timestamp\":\"1452510886089\",\"signature\":\"GxIsu1hJiBnu/OBRSRqDOwDi55LbUh73DmK1k9gS9xD0bJe35DWI1b34/9678ZFadN4p3Zcn5zGC8MLOjQ7ZNUSknrJ98iED4NNzu+N82qfBsrTiegtWsJmY+kobsK8dvzq4MEAoNs+jitx79AcpTUbgSHUcTLH9XUF7nMNQMOBHevNf2+j93ML438H8ahNXhL4pPHphYm74rmYblrXHYzBS7yFxqB9VSyG8PCIArb2/I7QAN80m1BbTzW6iySuo7wrDS5amq9tWBiXqicv8enA68w5yRHgAfyvK5b1otx9/vFaxqFqkvZrXAXC5XVjf8ehcle6eWrVnQDpZMRiFgQ==\"}}";

		JsonObjectBuilder jsonObject = Json.createObjectBuilder();
		JsonObjectBuilder params = Json.createObjectBuilder();
		params.add("numVotesPerVotingCard", "2");
		params.add("numVotesPerAuthToken", "1");
		jsonObject.add("electionInformationParams", params);
		ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setJson(jsonObject.build().toString());
		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		List<BallotBox> ballotBoxList = new ArrayList<>();
		BallotBox bb = new BallotBox();
		bb.setVote(vote2Json);
		ballotBoxList.add(bb);
		when(ballotBoxRepository
				.findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(ballotBoxList);

		assertEquals(ValidationErrorType.FAILED, rule.execute(vote).getValidationErrorType());
	}

	@Test
	public void getRuleName() {
		Assert.assertEquals(rule.getName(), RuleNames.VOTE_MAX_NUMBER_OF_ALLOWED_VOTES.getText());
	}
}
