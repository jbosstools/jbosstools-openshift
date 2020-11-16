/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.jboss.tools.common.launcher.reddeer.wizards.NewLauncherProjectWizard;
import org.jboss.tools.common.launcher.reddeer.wizards.NewLauncherProjectWizardPage;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;
import org.jboss.tools.openshift.reddeer.wizard.CreateComponentWizard;
import org.jboss.tools.openshift.reddeer.wizard.page.CreateComponentWizadPage;
import org.junit.After;

/**
 * Abstract class for ODO Tests
 * 
 * @author jkopriva@redhat.com
 */
public abstract class AbstractODOTest {
	
	private static String eclipseProject = "myService";
	
	@After
	public void cleanUp() {
		//Cleanup notifications
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
	}
	
	protected static void importLauncherProject() {
		NewLauncherProjectWizard wizard = new NewLauncherProjectWizard();	
		wizard.openWizardFromShellMenu();
		
		NewLauncherProjectWizardPage wizardPage = new NewLauncherProjectWizardPage(wizard);
		wizardPage.setTargetMission("rest-http");
		wizardPage.setTargetRuntime("vert.x community");
		wizardPage.setProjectName(eclipseProject);
		wizardPage.toggleUseDefaultLocationCheckBox(true);
		
		wizard.finish(TimePeriod.getCustom(2500));
	}
	
	protected static void createComponent(String projectName, String javaComponentType, boolean devfile) {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		OpenShiftODOProject project = explorer.getOpenShiftODOConnection().getProject(projectName);
		project.openCreateComponentWizard();
		CreateComponentWizard componentWizard = new CreateComponentWizard();
		CreateComponentWizadPage componentWizardPage = new CreateComponentWizadPage(componentWizard);
		componentWizardPage.setComponentName(eclipseProject);
		componentWizardPage.setEclipseProject(eclipseProject);
		componentWizardPage.selectComponentType(javaComponentType, devfile);
		if (!devfile) {
	    componentWizardPage.selectComponentVersion("latest");
		}
		componentWizardPage.setApplication("myapp");
		componentWizard.finish(TimePeriod.VERY_LONG);
		
		new WaitWhile(new ConsoleHasNoChange(), TimePeriod.VERY_LONG);
	}

}
