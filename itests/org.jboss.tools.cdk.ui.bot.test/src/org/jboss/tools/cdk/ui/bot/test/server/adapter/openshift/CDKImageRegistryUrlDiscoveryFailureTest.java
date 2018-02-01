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
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing of not working discovery feature
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDKImageRegistryUrlDiscoveryFailureTest extends CDKImageRegistryUrlAbstractTest {

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
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

}
