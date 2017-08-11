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
package org.jboss.tools.openshift.ui.bot.test.application.v3.basic;

import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.junit.Test;

@RequiredBasicConnection
@RequiredProject
public class OpenNewApplicationWizardTest {
	
	@InjectRequirement
	OpenShiftProjectRequirement projectReq;
	
	@Test
	public void testOpenNewApplicationWizardViaCentral() {
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
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning());
	}
}
