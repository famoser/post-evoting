/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import ch.post.it.evoting.controlcomponents.commons.CommonsConfig;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = { "ch.post.it.evoting.controlcomponents.commons", "ch.post.it.evoting.controlcomponents.mixing" })
@EntityScan(basePackages = { "ch.post.it.evoting.controlcomponents.commons", "ch.post.it.evoting.controlcomponents.mixing" })
@Import(CommonsConfig.class)
public class MixnetApplication {
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		SpringApplication.run(MixnetApplication.class);
	}
}
