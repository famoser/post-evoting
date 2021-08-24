/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.votingserver.commons.infrastructure.config;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class InfrastructureConfigTest {

	@ClassRule
	public static EnvironmentVariables environmentVariables = new EnvironmentVariables();

	@BeforeClass
	public static void setup() {
		environmentVariables.set("VAR_ONE", "123");
	}

	//Given the correct setting of an environment variable then its value should be returned
	@Test
	public void shouldFindEnvironmentVariable() {
		String var_one = InfrastructureConfig.getEnvWithDefaultOption("VAR_ONE", "123");
		Assert.assertEquals(Integer.valueOf(123), Integer.valueOf(var_one));
	}

	// "Given no environment variable then the default value should be returned"
	@Test
	public void shouldReturnDefault() {
		String var_one = InfrastructureConfig.getEnvWithDefaultOption("VAR_TWO", "456");
		Assert.assertEquals(Integer.valueOf(456), Integer.valueOf(var_one));
	}
}
