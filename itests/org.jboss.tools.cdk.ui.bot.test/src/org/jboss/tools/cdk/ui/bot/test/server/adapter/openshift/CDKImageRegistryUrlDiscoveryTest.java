/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.server.adapter.openshift;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
public class CDKImageRegistryUrlDiscoveryTest extends CDKImageRegistryUrlAbstractTest {

	private static final Logger log = Logger.getLogger(CDKImageRegistryUrlDiscoveryTest.class);

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}

	/**
	 * Base test for Discover Image Registry URL feature JBIDE-25093
	 * Covers also JBIDE-25014
	 */
	@Test
	public void testRediscoveryOfUrlAfterValueChanged() {
		log.info("Checking image registry url after cdk was started");
		checkImageRegistryUrl(OPENSHIFT_REGISTRY);
		log.info("Checking overwriting of empty image registry url");
		checkImageRegistryUrlRediscovered("");
		log.info("Testing overwriting of proper url value");
		checkImageRegistryUrlRediscovered("http://localhost:8443");
		wizard.finish();
	}

	/**
	 * Partially covers JBIDE-25046
	 */
	@Test
	public void testImageRegistryUrlIsUpdated() {
		// delete registry url
		wizard.getImageRegistryUrl().setText("");
		// save empty value
		wizard.finish();
		// stop server
		stopServerAdapter();
		// start server adapter -> should bring up the value of registry url in existing
		// connection
		startServerAdapter(() -> skipRegistration(getCDEServer()), true);
		connection.refresh();
		wizard = connection.editConnection();
		switchOffPasswordSaving();
		checkImageRegistryUrl(OPENSHIFT_REGISTRY);
		wizard.finish();
	}

	/**
	 * TODO Covers JBIDE-25120
	 */
	public void testAllImageRegistryUrlUpdated() {
		// set image registry url to "" string in existing connection
		// create new connection and have empty image registry url
		// stop and start server adapter
		// check that both connections have filled image registry url
	}

}
