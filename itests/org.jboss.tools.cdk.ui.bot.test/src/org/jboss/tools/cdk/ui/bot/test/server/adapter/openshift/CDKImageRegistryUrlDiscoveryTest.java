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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CDKImageRegistryUrlDiscoveryTest extends CDKImageRegistryUrlAbstractTest {

	private String openshiftRegistry;

	private static final Logger log = Logger.getLogger(CDKImageRegistryUrlDiscoveryTest.class);

	@Before
	public void setupRegistry() {
		log.info("Obtaining URL value of openshift registry");
		openshiftRegistry = getMinishiftOpenshiftRegistry();
	}

	@Override
	protected String getServerAdapter() {
		// return SERVER_ADAPTER_32;
		// workaround for https://github.com/eclipse/reddeer/issues/1841
		return "Container Development Environment 3.2";
	}

	/**
	 * Base test for Discover Image Registry URL feature JBIDE-25093
	 */
	@Test
	public void testImageRegistryUrlIsAtPlaceAfterCDKStart() {
		checkImageRegistryUrl(openshiftRegistry);
		wizard.cancel();
	}

	/**
	 * Covers JBIDE-25014
	 */
	@Test
	public void testDiscoveryOfEmptyImageRegistryUrl() {
		wizard.getImageRegistryUrl().setText("");
		checkImageRegistryUrl("");
		wizard.discover();
		checkImageRegistryUrl(openshiftRegistry);
		wizard.finish();
	}

	/**
	 * Covers JBIDE-25014
	 */
	@Test
	public void testOverwritingOfImageRegistryUrl() {
		wizard.getImageRegistryUrl().setText("http://localhost:8443");
		assertFalse(getImageRegistryUrlValue().contains(openshiftRegistry));
		wizard.discover();
		checkImageRegistryUrl(openshiftRegistry);
		wizard.finish();
	}

	/**
	 * Covers JBIDE-25049
	 */
	@Test
	public void testRegistryUrlNotFoundDialog() {
		String shellTitle = OpenShiftLabel.Shell.REGISTRY_URL_NOT_FOUND;
		wizard.getImageRegistryUrl().setText("");
		wizard.finish();
		stopServerAdapter();
		wizard = connection.editConnection();
		switchOffPasswordSaving();
		try {
			wizard.discover();
			fail("Expected OpenshiftToolsException was not thrown, possibly no dialog is shown.");
		} catch (OpenShiftToolsException osExc) {
			// os exception was thrown with specific message
			assertTrue("Registry URL not found dialog did not appear.", osExc.getMessage().contains(shellTitle));
			// error dialog is still there
			new WaitUntil(new ShellIsAvailable(shellTitle), TimePeriod.SHORT);
			new DefaultShell(shellTitle);
			new OkButton().click();
		}
		wizard.cancel();
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
		// start server adapter -> should bring up the value of registr url in existing
		// connection
		startServerAdapter();
		connection.refresh();
		wizard = connection.editConnection();
		switchOffPasswordSaving();
		checkImageRegistryUrl(openshiftRegistry);
		wizard.finish();
	}

	/**
	 * TODO Covers JBIDE-25120
	 */
	public void testAllImageRegistryUrlUpdated() {
		// set image registry url to "" string in existing connection
		// crate new connection and have empty IR url
		// stop and start server adapter
		// check that both connections have filled IR url
	}

}
