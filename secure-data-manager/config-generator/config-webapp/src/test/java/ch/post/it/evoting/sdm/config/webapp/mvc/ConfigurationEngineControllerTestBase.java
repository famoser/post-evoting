/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.mvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.sdm.commons.domain.CreateVerificationCardIdsInput;
import ch.post.it.evoting.sdm.config.spring.SpringConfigServices;
import ch.post.it.evoting.sdm.config.spring.batch.CommonBatchInfrastructure;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.ballotbox.BallotBoxWebappAdapter;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.electionevent.ElectionEventWebappAdapter;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.voters.VotersWebappAdapter;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
abstract public class ConfigurationEngineControllerTestBase {

	protected static final String ELECTION_EVENT_ID = "eeid";

	protected static final String VERIFICATION_CARD_SET_ID = "vercsid";

	protected static final ObjectMapper objectMapper = new ObjectMapper().enableDefaultTyping();

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	protected MvcResult runPrecomputeTest(int itemCount) throws Exception {
		return this.mockMvc
				// POST to /precompute
				.perform(post("/precompute")
						// with a JSON payload
						.contentType(MediaType.APPLICATION_JSON)
						// asking for 3 card to be generated
						.content(getPrecomputeBody(itemCount)))
				// The request should have started streaming
				.andExpect(request().asyncStarted())
				// Get the streaming results
				.andDo(MvcResult::getAsyncResult)
				// It should have been completed
				// The result should be OK
				.andExpect(status().isOk())
				// The contents should be a CSV
				.andExpect(content().contentType("text/csv"))
				// Finally, return the response.
				.andReturn();
	}

	/**
	 * Get a request body for the pre-compute call.
	 *
	 * @param numItems the number of verification card IDs to request.
	 * @return a JSON representation of the body.
	 */
	private byte[] getPrecomputeBody(int numItems) throws JsonProcessingException {
		CreateVerificationCardIdsInput body = new CreateVerificationCardIdsInput();
		body.setElectionEventId(ELECTION_EVENT_ID);
		body.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);
		body.setNumberOfVerificationCardIds(numItems);

		return objectMapper.writeValueAsBytes(body);
	}

	@Configuration
	@ComponentScan({ "ch.post.it.evoting.sdm.config.webapp.mvc" })
	@Import({ SpringConfigServices.class, CommonBatchInfrastructure.class })
	@EnableWebMvc
	static class TestConfig {

		@Bean
		ElectionEventWebappAdapter electionEventWebappAdapter() {
			return mock(ElectionEventWebappAdapter.class);
		}

		@Bean
		VotersWebappAdapter votersWebappAdapter() {
			return mock(VotersWebappAdapter.class);
		}

		@Bean
		BallotBoxWebappAdapter ballotBoxWebappAdapter() {
			return mock(BallotBoxWebappAdapter.class);
		}
	}
}
