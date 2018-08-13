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

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.cdk.reddeer.core.enums.CDKRuntimeOS;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
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
@RemoveCDKServers
@ContainerRuntimeServer(
		version = CDKVersion.CDK350,
		useExistingBinary=true,
		makeRuntimePersistent=true,
		usernameProperty="developers.username",
		passwordProperty="developers.password")
public class CDKImageRegistryUrlValidatorTest extends CDKImageRegistryUrlAbstractTest {

	public static final String OC_IS_NOT_CONFIGURED = "OpenShift client oc not configured"; 
	
	public static final String OC_3_11_RSYNC_WARNING = "OpenShift client oc version 3.11 is required to avoid permission errors when rsync-ing from a Linux client";
	
	public static final String SIGN_TO_OPENSHIFT = "Please sign in to your OpenShift server";
	
	public static final Logger log = Logger.getLogger(CDKImageRegistryUrlValidatorTest.class);
	
	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName();
	}
	
	@Override
	protected void startServerAdapter() {
		serverRequirement.configureCDKServerAdapter(false);
		serverRequirement.startServerAdapterIfNotRunning(() -> {
			skipRegistrationViaFlag(getCDKServer(), true);
		}, false);
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
		assertStringContains(wizard.getConnectionMessage(), OC_IS_NOT_CONFIGURED);
		wizard.cancel();
		setupOCForWorkspace(DEFAULT_MINISHIFT_HOME);
		wizard = getOpenshiftConnectionWizard(CDKTestUtils.findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		assertStringContains(wizard.getConnectionMessage(), CDKRuntimeOS.get() == CDKRuntimeOS.LINUX ? OC_3_11_RSYNC_WARNING : SIGN_TO_OPENSHIFT); 
		assertTrue(wizard.getOCLocationLabel().getText().endsWith(CDKUtils.IS_WINDOWS ? "oc.exe" : "oc"));  
	}
	
	/**
	 * Covers JBIDE-25121
	 */
	@Test
	public void testImageRegistryUrlValidator() {
		wizard.cancel();
		setupOCForWorkspace(DEFAULT_MINISHIFT_HOME);
		wizard = getOpenshiftConnectionWizard(CDKTestUtils.findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		// test default description
		assertStringContains(wizard.getConnectionMessage(), CDKRuntimeOS.get() == CDKRuntimeOS.LINUX ? OC_3_11_RSYNC_WARNING : SIGN_TO_OPENSHIFT); 
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
