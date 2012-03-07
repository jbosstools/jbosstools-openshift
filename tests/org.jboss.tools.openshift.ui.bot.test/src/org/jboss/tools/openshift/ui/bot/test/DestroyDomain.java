package org.jboss.tools.openshift.ui.bot.test;

import org.jboss.tools.openshift.ui.bot.util.DomainDestroyer;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.junit.Test;

public class DestroyDomain extends SWTTestExt {

	/*
	 * Since there is no way how to destroy a domain from JBoss Tools, we need
	 * to use OpenShift REST API to clean the account
	 */
	@Test
	public void canDestroyDomain() {

		// destroy domain
		int resp_code = DomainDestroyer.destroyDomain(
				TestProperties.getProperty("openshift.domain.new"),
				TestProperties.getProperty("openshift.user.name"),
				TestProperties.getProperty("openshift.user.pwd"));

		assertTrue("Trying to destroy domain: HTTP Response code is not 200.",
				resp_code == 200);

	}
}
