/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.spring;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ch.post.it.evoting.sdm.config.spring.SpringConfigServices;
import ch.post.it.evoting.sdm.config.spring.batch.CommonBatchInfrastructure;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.ballotbox.BallotBoxWebappAdapter;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.electionevent.ElectionEventWebappAdapter;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.voters.VotersWebappAdapter;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

@Configuration
@EnableWebMvc
@PropertySource("classpath:application.properties")
@ComponentScan({ "ch.post.it.evoting.sdm.config.webapp.mvc" })
@Import({ SpringConfigServices.class, CommonBatchInfrastructure.class })
public class WebAppConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebAppConfig.class);

	@Value("${spring.profiles.active}")
	private String activeProfiles;

	@PostConstruct
	private void postConstruct() {
		LOGGER.info("Spring active profiles : {}", activeProfiles);
	}

	@Bean
	public ElectionEventWebappAdapter electionEventWebappAdapter() {
		return new ElectionEventWebappAdapter();
	}

	@Bean
	public VotersWebappAdapter votersWebappAdapter(final ConfigObjectMapper mapper) {
		return new VotersWebappAdapter(mapper);
	}

	@Bean
	public BallotBoxWebappAdapter ballotBoxWebappAdapter() {
		return new BallotBoxWebappAdapter();
	}

}
