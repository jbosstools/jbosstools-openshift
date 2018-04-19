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

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.cdk.reddeer.core.enums.CDKRuntimeOS;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDKServerAdapterAbstractTest;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.wizard.v3.BasicAuthenticationSection;
import org.jboss.tools.openshift.reddeer.wizard.v3.OpenShift3ConnectionWizard;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * Abstract test class with sets of configuration to cdk image registry discovery feature
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public abstract class CDKImageRegistryUrlAbstractTest extends CDKServerAdapterAbstractTest {

	public OpenShift3ConnectionWizard wizard;

	private static final Logger log = Logger.getLogger(CDKImageRegistryUrlAbstractTest.class);
	
	public static final String VALIDATION_MESSAGE = "Please provide a valid image registry (HTTP/S) URL"; 
	
	protected static final String OPENSHIFT_REGISTRY = getMinishiftOpenshiftRegistry();

	@BeforeClass
	public static void setupCDKImageRegistryUrlAbstract() {
		log.info("Setting up environment, checking test arguments"); 
		log.info("Adding new CDK 3.2+ server adapter"); 
		addNewCDK32Server(SERVER_ADAPTER_32, MINISHIFT_HYPERVISOR,
				CDK32_MINISHIFT, MINISHIFT_PROFILE); 
	}
	
	@AfterClass
	public static void tearDownCDKImageResgitryUrlAbstract() {
		CDKTestUtils.setOCToPreferences(""); 
		stopRunningServerAdapters();
	}

	@Before
	public void setupAdapter() {
		startServerAdapterIfNotRunning(() -> {
			skipRegistrationViaFlag(getCDKServer(), true);
		}, false);
		wizard = getOpenshiftConnectionWizard(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		assertTrue(wizard.getAuthSection().getMethod().equals(AuthenticationMethod.BASIC));
		switchOffPasswordSaving(wizard);
	}
	
	public static void setupOCForWorkspace() {
		log.info("Setting up oc for workspace"); 
		log.info("Find oc on minishift home path"); 
		String pathToOC = CDKTestUtils.findFileOnPath(DEFAULT_MINISHIFT_HOME, "oc" + CDKRuntimeOS.get().getSuffix()); 
		log.info("Setting oc into preferences on path " + pathToOC); 
		CDKTestUtils.setOCToPreferences(pathToOC);
	}
	
	protected OpenShift3ConnectionWizard openOpenshiftConnectionWizard(OpenShift3Connection connection) {
		return connection.editConnection();
	}
	
	protected OpenShift3ConnectionWizard getOpenshiftConnectionWizard(OpenShift3Connection connection) {
		// return wizard if is open
		try {
			return new OpenShift3ConnectionWizard(OpenShiftLabel.Shell.EDIT_CONNECTION);
		} catch (CoreLayerException exc) {
			// open if it is not open
			log.info("OpenShiftConnection wizrd was not opened, openning..."); 
			return openOpenshiftConnectionWizard(connection);
		}
	}

	@After
	public void tearDownConnection() {
		closeWizardIfOpen(wizard);
	}
	
	protected void closeWizardIfOpen(OpenShift3ConnectionWizard wizard) {
		if (wizard != null) {
			if (!wizard.getShell().isDisposed()) {
				if (wizard.getShell().isVisible()) {
					wizard.getShell().close();
				}
			}
			wizard = null;
		}		
	}
	
	public void checkImageRegistryUrlRediscovered(OpenShift3ConnectionWizard wizard, String expectedUrl) {
		log.info("Checking image registry url was re-discovered after replaced with " + expectedUrl); 
		wizard.getImageRegistryUrl().setText(expectedUrl);
		checkImageRegistryUrl(wizard, expectedUrl);
		discoverImageRegistryUrl(wizard);
		checkImageRegistryUrl(wizard, OPENSHIFT_REGISTRY);
	}
	
	protected void discoverImageRegistryUrl(OpenShift3ConnectionWizard wizard) {
		try {
			wizard.discover();
		} catch (OpenShiftToolsException osExc) {
			String console = collectConsoleOutput(log, true);
			fail(console);
		}
	}
	
	protected void assertStringContains(String original, String toContain) {
		assertTrue("Failed that \"" + original + "\"\r\ndoes not contain: \"" + toContain + "\"",   
				original.contains(toContain));
	}
	
	protected void assertFalseStringContains(String original, String toContain) {
		assertFalse("Failed that \"" + original + "\"\r\ndoes contain \"" + toContain + "\"",   
				original.contains(toContain));
	}
	
	/**
	 * Creates wait condition that is never fulfilled to be passed into WaitUntil.
	 * This construct substitutes sleep process
	 * @return negative wait condition
	 */
	public static WaitCondition getWaitUntilTresholdCondition() {
		return new WaitCondition() {
			
			@Override
			public boolean test() {
				return false;
			}
			
			@Override
			public <T> T getResult() {
				return null;
			}
			
			@Override
			public String errorMessageWhile() {
				return " while condition is never true... "; 
			}
			
			@Override
			public String errorMessageUntil() {
				return " did not reach treshold... "; 
			}
			
			@Override
			public String description() {
				return "  wait until treshold is reached... "; 
			}
		};
	}
	
	protected void checkImageRegistryUrl(OpenShift3ConnectionWizard wizard, String expected) {
		String url = getImageRegistryUrlValue(wizard);
		assertTrue("Expected url value: " + expected + " but was: " +  url,  
				url.contains(expected));
	}
	
	protected String getImageRegistryUrlValue(OpenShift3ConnectionWizard wizard) {
		return wizard.getImageRegistryUrl().getText();
	}

	protected void switchOffPasswordSaving(OpenShift3ConnectionWizard wizard) {
		wizard.switchAuthenticationSection(AuthenticationMethod.BASIC);
		((BasicAuthenticationSection) wizard.getAuthSection()).setSavePassword(false);
	}
	
	/**
	 * TODO replace with real implementation Execute 'minishift openshift registry'
	 * command and get its result
	 */
	public static String getMinishiftOpenshiftRegistry() {
		return "172.30.1.1:5000"; 
	}
	
	public OpenShift3ConnectionWizard createConnection() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		explorer.openConnectionShell();
		return new OpenShift3ConnectionWizard(OpenShiftLabel.Shell.NEW_CONNECTION);
	}
	
}
