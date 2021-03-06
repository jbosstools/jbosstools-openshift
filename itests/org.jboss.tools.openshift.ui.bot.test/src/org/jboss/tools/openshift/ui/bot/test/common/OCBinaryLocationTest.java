/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.common;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShift3PreferencePage;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
public class OCBinaryLocationTest extends AbstractTest {
	
	private WorkbenchPreferenceDialog dialog;
	private OpenShift3PreferencePage page;
	
	@Before
	public void openDialogAndSelectPage() {
		dialog = new WorkbenchPreferenceDialog();
		page = new OpenShift3PreferencePage(dialog);
		
		dialog.open();
		dialog.select(page);
	}
	
	@Test
	public void testOClocationSetByRequirement() {
		String location = page.getOCLocation().getText();
		//First condition is for unix like systems, second for windows systems
		assertTrue(location.contains("binaries/oc") || location.contains("binaries\\oc"));
	}
	
	@Test
	public void testSetValidOCLocation() {
		page.setOCLocation(OpenShiftCommandLineToolsRequirement.getDefaultOCLocation());
		
		try {
			new WaitUntil(new ControlIsEnabled(new PushButton(OpenShiftLabel.Button.APPLY)), 
					TimePeriod.getCustom(5));
		} catch (WaitTimeoutExpiredException ex) {
			fail("Button Apply should be enabled for valid OC binary location");
		}
	}
	
	@Test
	public void testSetInvalidOCLocation() {
		page.setOCLocation("invalidPath");
		try {
			new WaitWhile(new ControlIsEnabled(new PushButton(OpenShiftLabel.Button.APPLY)), 
					TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Button Apply should be disabled for invalid OC binary location. Fails due to JBIDE-20685");
		}
	}
	
	@After
	public void closeDialog() {
		try {
			new DefaultShell("Could Not Accept Changes");
			new OkButton().click();
		} catch (CoreLayerException e) {
			// Just swallow
		}
		dialog.cancel();
	}
}
