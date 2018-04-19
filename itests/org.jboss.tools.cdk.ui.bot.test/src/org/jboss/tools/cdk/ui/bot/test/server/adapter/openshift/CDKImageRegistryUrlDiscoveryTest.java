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
import org.jboss.tools.cdk.reddeer.server.exception.JBIDE25120AssertionError;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;
import org.jboss.tools.openshift.reddeer.wizard.v3.BasicAuthenticationSection;
import org.jboss.tools.openshift.reddeer.wizard.v3.OpenShift3ConnectionWizard;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * CDK image registry URL integration tests.
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDKImageRegistryUrlDiscoveryTest extends CDKImageRegistryUrlAbstractTest {

	private static final Logger log = Logger.getLogger(CDKImageRegistryUrlDiscoveryTest.class);

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@BeforeClass
	public static void setupCDKImageRegistrUrlDiscovery() {
		setupOCForWorkspace();
	}
	
	/**
	 * Base test for Discover Image Registry URL feature JBIDE-25093
	 * Covers also JBIDE-25014
	 */
	@Test
	public void testRediscoveryOfUrlAfterValueChanged() {
		log.info("Checking image registry url after cdk was started"); 
		checkImageRegistryUrl(wizard, OPENSHIFT_REGISTRY);
		log.info("Checking overwriting of empty image registry url"); 
		checkImageRegistryUrlRediscovered(wizard, ""); 
		log.info("Testing overwriting of proper url value"); 
		checkImageRegistryUrlRediscovered(wizard, "http://localhost:8443"); 
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
		startServerAdapter(() -> skipRegistrationViaFlag(getCDKServer(), true), false);
		wizard = getOpenshiftConnectionWizard(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		switchOffPasswordSaving(wizard);
		checkImageRegistryUrl(wizard, OPENSHIFT_REGISTRY);
		wizard.finish();
	}

	/**
	 * Covers JBIDE-25120
	 * Remove @Ignore after issue is resolved. Do not use expected exc., due to vast demands on time
	 */
	@Ignore
	@Test (expected = JBIDE25120AssertionError.class)
	public void testAllConnectionsImageRegistryUrlUpdated() {
		// delete registry url at first connection
		wizard.getImageRegistryUrl().setText(""); 
		String server = wizard.getServer().getText();
		wizard.finish();
		// create new connection and have empty image registry url
		OpenShift3ConnectionWizard newWizard = createConnection();
		newWizard.switchAuthenticationSection(AuthenticationMethod.BASIC);
		newWizard.getServer().setSelection(server);
		((BasicAuthenticationSection) newWizard.getAuthSection()).getUsernameLabel().setText(OPENSHIFT_ADMIN);
		((BasicAuthenticationSection) newWizard.getAuthSection()).getPasswordLabel().setText(OPENSHIFT_ADMIN);
		switchOffPasswordSaving(newWizard);
		newWizard.openAdvancedSection();
		newWizard.getImageRegistryUrl().setText(""); 
		newWizard.getClusterNamespace().setText("openshift"); 
		newWizard.finish();
		newWizard = null;
		// stop and start server adapter
		stopServerAdapter();
		startServerAdapter(() -> skipRegistrationViaFlag(getCDKServer(), true), false);
		// check that both connections have filled image registry url
		try {
			wizard = getOpenshiftConnectionWizard(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
			checkImageRegistryUrl(wizard, getMinishiftOpenshiftRegistry());
			wizard.cancel();
			newWizard = getOpenshiftConnectionWizard(findOpenShiftConnection(null, OPENSHIFT_ADMIN));
			newWizard.openAdvancedSection();
			checkImageRegistryUrl(newWizard, getMinishiftOpenshiftRegistry());
			newWizard.cancel();
		} catch (AssertionError err) {
			throw new JBIDE25120AssertionError();
		} finally {
			closeWizardIfOpen(wizard);
			closeWizardIfOpen(newWizard);
		}
		
	}

}
