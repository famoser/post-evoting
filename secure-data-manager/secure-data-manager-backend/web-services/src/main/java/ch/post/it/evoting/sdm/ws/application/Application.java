/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 * Contains a main method to start spring application.
 */
@SpringBootApplication
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = { "ch.post.it.evoting.sdm" })
public class Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	/**
	 * Runs the spring application.
	 *
	 * @param args the arguments
	 */

	public static void main(String[] args) {
		// these props allows us to keep using 'sdm.properties' file as the main/default config file
		// instead of
		// 'application.properties'. These have to be defined as either OS env prop, system props or
		// cmdline args.
		// This way we can have an internal default configuration file, and if needed, an external one
		// with the same
		// name that we already use and not have to change anything else. Both props can be overridden
		// by cmdline args
		System.setProperty("spring.config.additional-location", "${user.home}/sdm/sdmConfig/");
		System.setProperty("spring.config.name", "sdm");

		LOGGER.info("-------------------- Starting Secure Data Manager... --------------------");

		Security.addProvider(new BouncyCastleProvider());

		ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(Application.class).run(args);

		applicationContext.registerShutdownHook();

		LOGGER.info("-------------------- Secure Data Manager successfully started. -------------------- ");
	}

}
