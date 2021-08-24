/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp;

import java.security.Security;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import ch.post.it.evoting.sdm.config.webapp.spring.WebAppConfig;

/**
 *
 */
public class WebAppInitializer implements WebApplicationInitializer {

	public void onStartup(final ServletContext servletContext) throws ServletException {

		Security.addProvider(new BouncyCastleProvider());

		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.register(WebAppConfig.class);
		ctx.setServletContext(servletContext);

		ServletRegistration.Dynamic dynamic = servletContext.addServlet("dispatcher", new DispatcherServlet(ctx));
		dynamic.addMapping("/");
		dynamic.setLoadOnStartup(1);
		dynamic.setAsyncSupported(true);
	}
}
