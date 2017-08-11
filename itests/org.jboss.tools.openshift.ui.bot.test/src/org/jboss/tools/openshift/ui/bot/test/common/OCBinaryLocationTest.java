/*******************************************************************************
 * Copyright (c) 2007-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.common;

import static org.junit.Assert.fail;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShift3PreferencePage;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@OCBinary
public class OCBinaryLocationTest {
	
	private WorkbenchPreferenceDialog dialog;
	private OpenShift3PreferencePage page;
	
	@Before
	public void openDialogAndSelectPage() {
		dialog = new WorkbenchPreferenceDialog();
		page = new OpenShift3PreferencePage();
		
		dialog.open();
		dialog.select(page);
	}
	
	@Test
	public void testSetValidOCLocation() {
		page.setOCLocation(OpenShiftCommandLineToolsRequirement.getOCLocation());
		
		try {
			new WaitUntil(new WidgetIsEnabled(new PushButton(OpenShiftLabel.Button.APPLY)), 
					TimePeriod.getCustom(5));
		} catch (WaitTimeoutExpiredException ex) {
			fail("Button Apply should be enabled for valid OC binary location");
		}
	}
	
	@Test
	public void testSetInvalidOCLocation() {
		page.setOCLocation("invalidPath");
		
		try {
			new WaitWhile(new WidgetIsEnabled(new PushButton(OpenShiftLabel.Button.APPLY)), 
					TimePeriod.getCustom(5));
		} catch (WaitTimeoutExpiredException ex) {
			fail("Button Apply should be disabled for invalid OC binary location. Fails due to JBIDE-20685");
		}
	}
	
	@After
	public void closeDialog() {
		page.clearOCLocation();
		dialog.cancel();
	}
}
