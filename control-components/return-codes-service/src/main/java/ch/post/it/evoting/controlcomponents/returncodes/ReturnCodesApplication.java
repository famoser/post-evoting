/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import ch.post.it.evoting.controlcomponents.commons.CommonsConfig;

/**
 * The control components Return Codes (CCR) application
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = { "ch.post.it.evoting.controlcomponents.commons", "ch.post.it.evoting.controlcomponents.returncodes" })
@EntityScan(basePackages = { "ch.post.it.evoting.controlcomponents.commons", "ch.post.it.evoting.controlcomponents.returncodes" })
@Import(CommonsConfig.class)
public class ReturnCodesApplication {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		SpringApplication.run(ReturnCodesApplication.class);
	}
}


