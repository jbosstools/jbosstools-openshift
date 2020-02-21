/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
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
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing of not working discovery feature
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
@RemoveCDKServers
@ContainerRuntimeServer(
		version = CDKVersion.CDK3110,
		useExistingBinaryFromConfig=true,
		makeRuntimePersistent=true,
		usernameProperty="developers.username",
		passwordProperty="developers.password",
		useExistingBinaryInProperty="cdk32.minishift")
public class CDKImageRegistryUrlDiscoveryFailureTest extends CDKImageRegistryUrlAbstractTest {

	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName();
	}
	
	@BeforeClass
	public static void setupCDKImageRegistrUrlDiscovery() {
		serverRequirement.configureCDKServerAdapter(false);
		setupOCForWorkspace(DEFAULT_MINISHIFT_HOME);
	}
	
	@Override
	protected void startServerAdapter() {
		serverRequirement.configureCDKServerAdapter(false);
		startServerAdapterIfNotRunning(getCDKServer(), () -> {
			skipRegistrationViaFlag(getCDKServer(), true);
		}, false);
	}
	
	/**
	 * Covers JBIDE-25049
	 */
	@Test
	public void testRegistryUrlNotFoundDialog() {
		wizard.getImageRegistryUrl().setText(""); 
		wizard.finish();
		stopServerAdapter(getCDKServer());
		wizard = getOpenshiftConnectionWizard(CDKTestUtils.findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		switchOffPasswordSaving(wizard);
		try {
			wizard.discover();
			fail("Expected OpenshiftToolsException was not thrown, possibly no dialog is shown."); 
		} catch (OpenShiftToolsException osExc) {
			// os exception was thrown with specific message
			assertTrue("Registry URL not found dialog did not appear.", osExc.getMessage().contains(OpenShiftLabel.Shell.REGISTRY_URL_NOT_FOUND)); 
			// error dialog is still there
			new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.REGISTRY_URL_NOT_FOUND), TimePeriod.SHORT);
			new DefaultShell(OpenShiftLabel.Shell.REGISTRY_URL_NOT_FOUND);
			new OkButton().click();
		}
		wizard.cancel();
	}

}
