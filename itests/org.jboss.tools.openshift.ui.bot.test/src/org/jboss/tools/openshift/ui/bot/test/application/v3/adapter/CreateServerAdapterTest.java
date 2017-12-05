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
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsKilled;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.core.StringContains;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.AmountOfResourcesExists;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.condition.ServerAdapterExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(value=JBossPerspective.class)
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE)
@RunWith(RedDeerSuite.class)
public class CreateServerAdapterTest extends AbstractTest  {

	private static final String PROJECT_NAME = "kitchensink";

	private static final String GIT_REPO_URL = "https://github.com/jboss-developer/jboss-eap-quickstarts";

	private static final String JOB_NAME = "Refreshing server adapter list";

	private static final String GIT_REPO_DIRECTORY = "target/git_repo";

	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;

	@BeforeClass
	public static void waitTillApplicationIsRunning() {
		new WaitWhile(new OpenShiftResourceExists(Resource.BUILD, "eap-app-1", ResourceState.RUNNING,
				projectReq.getProjectName()), TimePeriod.getCustom(600));
		new WaitUntil(new AmountOfResourcesExists(Resource.POD, 2, projectReq.getProjectName()), TimePeriod.LONG,
				false);

		cloneGitRepoAndImportProject();
	}

	private static void cloneGitRepoAndImportProject() {
		OpenShiftUtils.cloneGitRepository(GIT_REPO_DIRECTORY, GIT_REPO_URL, true);
		OpenShiftUtils.importProjectUsingSmartImport(GIT_REPO_DIRECTORY, PROJECT_NAME);
	}

	@AfterClass
	public static void afterClass() throws IOException {
		cleanProjectsAndGitRepo();
	}
	
	private static void cleanProjectsAndGitRepo() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject iProject : projects) {
			try {
				iProject.delete(false, new NullProgressMonitor());
			} catch (CoreException e) {
				throw new RuntimeException("Unable to delete project " + iProject.getName(), e);
			}
		}
		try {
			TestUtils.delete(new File(GIT_REPO_DIRECTORY));
		} catch (IOException e) {
			throw new RuntimeException("Deletion of git repo was unsuccessfull.", e);
		}
	}

	@Test
	public void testCreateOpenShift3ServerAdapterViaShellMenu() {
		NewServerWizard dialog = new NewServerWizard();
		dialog.open();
		NewServerWizardPage page = new NewServerWizardPage(dialog);

		dialog.open();
		page.selectType(OpenShiftLabel.Others.OS3_SERVER_ADAPTER);
		next();

		next();

		setAdapterDetailsAndCreateAdapterAndVerifyExistence();
	}

	@Test
	public void testCreateOpenShift3ServerAdapterViaServersView() {
		ServersView2 serversView = new ServersView2();
		serversView.open();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_SERVER).select();

		new DefaultShell(OpenShiftLabel.Shell.ADAPTER);
		new DefaultTreeItem(OpenShiftLabel.Others.OS3_SERVER_ADAPTER).select();
		next();

		next();

		setAdapterDetailsAndCreateAdapterAndVerifyExistence();
	}

	@Test
	public void testCreateOpenShift3ServerAdapterViaOpenShiftExplorerView() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection().getProject(projectReq.getProjectName()).getService("eap-app").select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_ADAPTER_FROM_EXPLORER).select();

		new DefaultShell(OpenShiftLabel.Shell.SERVER_ADAPTER_SETTINGS);

		assertTrue("Service should be preselected for new OpenShift 3 server adapter",
				new DefaultTreeItem(projectReq.getProjectName(), "eap-app deploymentConfig=eap-app").isSelected());
		assertTrue("Eclipse project should be preselected automatically for new server adapter",
				new LabeledText("Eclipse Project: ").getText().equals(PROJECT_NAME));

		new FinishButton().click();

		new WaitWhile(new ShellIsAvailable(""));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);

		assertTrue("OpenShift 3 server adapter was not created.",
				new ServerAdapterExists(Version.OPENSHIFT3, "eap-app", "Service").test());
	}

	private void setAdapterDetailsAndCreateAdapterAndVerifyExistence() {
		new LabeledText("Eclipse Project: ").setText(PROJECT_NAME);
		new DefaultTreeItem(projectReq.getProjectName()).getItems().get(0).select();
		next();

		finishNewServerAdapterWizardAndVerifyExistence();
	}

	public void finishNewServerAdapterWizardAndVerifyExistence() {
		new FinishButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.ADAPTER));

		boolean jobExists = false;
		try {
			new WaitUntil(new JobIsRunning(new StringContains(JOB_NAME)), TimePeriod.getCustom(5));
			jobExists = true;
		} catch (WaitTimeoutExpiredException e) {
			// job is not running, do nothing
		}

		if (jobExists) {
			new WaitUntil(new JobIsKilled(JOB_NAME), TimePeriod.VERY_LONG);
		}

		assertTrue("OpenShift 3 server adapter was not created.",
				new ServerAdapterExists(Version.OPENSHIFT3, "eap-app", "Service").test());

	}

	private void next() {
		new WaitUntil(new ControlIsEnabled(new NextButton()));

		new NextButton().click();
		TestUtils.acceptSSLCertificate();

		new WaitUntil(new ControlIsEnabled(new BackButton()));
	}

	@After
	public void removeAdapterIfExists() {
		try {
			new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
			new ServerAdapter(Version.OPENSHIFT3, "eap-app", "Service").delete();
		} catch (OpenShiftToolsException ex) {
			// do nothing, adapter does not exists
		}
	}
}
