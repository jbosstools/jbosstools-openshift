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
package org.jboss.tools.openshift.ui.bot.test.application.v3.basic;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.junit.Assume;
import org.junit.Test;

@CleanOpenShiftExplorer
@RequiredBasicConnection
@RequiredProject
public class OpenNewApplicationWizardTest {
	
	@InjectRequirement
	OpenShiftProjectRequirement projectReq;
	
	@Test
	public void testOpenNewApplicationWizardViaCentral() {
		Assume.assumeTrue(centralIsAvailable());
		new NewOpenShift3ApplicationWizard().openWizardFromCentral();
		closeWizard();
	}

	@Test
	public void testOpenNewApplicationWizardViaShellMenu() {
		new NewOpenShift3ApplicationWizard().openWizardFromShellMenu();
		closeWizard();
	}
	
	@Test
	public void testOpenNewApplicationWizardViaOpenShiftExplorer() {
		new NewOpenShift3ApplicationWizard().openWizardFromExplorer(projectReq.getProject().getDisplayName());
		closeWizard();
	}
	
	private void closeWizard() {
		new CancelButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning());
	}
	
	
	private boolean centralIsAvailable() {
		try {
			new DefaultToolItem(new WorkbenchShell(), OpenShiftLabel.Others.RED_HAT_CENTRAL).click();
		} catch (CoreLayerException ex) {
			return false;
		}
		return true;
	}
}
