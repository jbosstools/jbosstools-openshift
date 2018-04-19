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

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Covers validation integration tests for OS connection created by CDK startup.
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDKImageRegistryUrlValidatorTest extends CDKImageRegistryUrlAbstractTest {

	public static final String OC_IS_NOT_SET = "The workspace setting for the OC binary is not set"; 
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@After
	public void tearDownCDKValidator() {
		CDKTestUtils.setOCToPreferences(""); 
	}
	
	/**
	 * Covers JBIDE-25703.
	 */
	@Test
	public void testSettingOCToWorkspace() {
		assertStringContains(wizard.getConnectionMessage(), OC_IS_NOT_SET);
		wizard.cancel();
		setupOCForWorkspace();
		wizard = getOpenshiftConnectionWizard(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		assertStringContains(wizard.getConnectionMessage(), "OpenShift server"); 
		assertTrue(wizard.getOCLocationLabel().getText().endsWith(CDKUtils.IS_WINDOWS ? "oc.exe" : "oc"));  
	}
	
	/**
	 * Covers JBIDE-25121
	 */
	@Test
	public void testImageRegistryUrlValidator() {
		wizard.cancel();
		setupOCForWorkspace();
		wizard = getOpenshiftConnectionWizard(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		// test default description
		assertStringContains(wizard.getConnectionMessage(), "OpenShift server"); 
		// test wrong image url
		wizard.getImageRegistryUrl().setText("foo.con"); 
		assertStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
		// test https://
		wizard.getImageRegistryUrl().setText("https://foo.con"); 
		assertFalseStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
		// test http://
		wizard.getImageRegistryUrl().setText("http://foo.con"); 
		assertFalseStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
		// test valid image registry url from discovery feature
		discoverImageRegistryUrl(wizard);
		assertFalseStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
	}

}
