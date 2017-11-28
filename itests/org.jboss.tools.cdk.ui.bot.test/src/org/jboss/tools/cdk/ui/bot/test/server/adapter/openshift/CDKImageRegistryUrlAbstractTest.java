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

import static org.junit.Assert.assertTrue;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.reddeer.common.logging.Logger;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDKServerAdapterAbstractTest;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.wizard.v3.BasicAuthenticationSection;
import org.jboss.tools.openshift.reddeer.wizard.v3.OpenShift3ConnectionWizard;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Abstract test class with sets of configuration to cdk image registry discovery feature
 * @author odockal
 *
 */
public abstract class CDKImageRegistryUrlAbstractTest extends CDKServerAdapterAbstractTest {

	public OpenShift3Connection connection;

	public OpenShift3ConnectionWizard wizard;

	private static final Logger log = Logger.getLogger(CDKImageRegistryUrlAbstractTest.class);
	
	public static final String VALIDATION_MESSAGE = "Please provide a valid image registry (HTTP/S) URL";

	@BeforeClass
	public static void setupCDKImageRegistryUrlDiscovery() {
		log.info("Setting AUTOMATED_MODE of ErrorDialog to false, in order to pass some tests");
		// switch off errordialog.automated_mode to verify error dialog
		ErrorDialog.AUTOMATED_MODE = false;
		log.info("Setting up environment, checking test arguments");
		checkMinishiftProfileParameters();
		log.info("Adding new CDK 3.2+ server adapter");
		addNewCDK3Server(CDK32_SERVER_NAME, "Container Development Environment 3.2", MINISHIFT_HYPERVISOR,
				MINISHIFT_PROFILE);
	}

	@AfterClass
	public static void tearDownCDKImageRegistryUrlDiscovery() {
		log.info("Setting AUTOMATED_MODE of ErrorDialog back to true after testing is done");
		ErrorDialog.AUTOMATED_MODE = true;
	}

	@Before
	public void setupAdapter() {
		startServerAdapter();
		connection = findOpenShiftConnection(null, OPENSHIFT_USERNAME);
		wizard = connection.editConnection();
		assertTrue(wizard.getAuthSection().getMethod().equals(AuthenticationMethod.BASIC));
		switchOffPasswordSaving();
	}

	@After
	public void tearDownConnection() {
		if (wizard != null) {
			if (!wizard.getShell().isDisposed()) {
				if (wizard.getShell().isVisible()) {
					wizard.getShell().close();
				}
			}
			wizard = null;
		}
		connection = null;
	}
	
	protected void checkImageRegistryUrl(String expected) {
		String url = getImageRegistryUrlValue();
		assertTrue("Expected url value: " + expected + "but was: " +  url,
				url.contains(expected));
	}
	
	protected String getImageRegistryUrlValue() {
		return wizard.getImageRegistryUrl().getText();
	}

	protected void switchOffPasswordSaving() {
		wizard.switchAuthenticationSection("Basic");
		((BasicAuthenticationSection) wizard.getAuthSection()).setSavePassword(false);
	}

	/**
	 * TODO replace with real implementation Execute 'minishift openshift registry'
	 * command and get its result
	 */
	protected String getMinishiftOpenshiftRegistry() {
		return "172.30.1.1:5000";
	}
	
}
