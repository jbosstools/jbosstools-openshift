/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
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
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.eclipse.reddeer.eclipse.core.resources.ProjectItem;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.ApplicationPodIsRunning;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(value=JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@CleanOpenShiftExplorer
@RequiredBasicConnection
@CleanConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE_RESOURCES_PATH)
public class PublishChangesTest extends AbstractTest  {

	public static String PUBLISHED_CODE = "package org.jboss.as.quickstarts.helloworld;\n"
			+ "public class HelloService {\n"
			+ "String createHelloMessage(String name) { return \"Hello OpenShift \" + name + \"!\"; }"
			+ "}";
	
	private String changedClass= "WEB-INF/classes/org/jboss/as/quickstarts/helloworld/HelloService.class";
	
	private static final String GIT_REPO_URL = "https://github.com/jboss-developer/jboss-eap-quickstarts";

	private static final String GIT_REPO_DIRECTORY = "target/git_repo";
	
	private static final String GIT_REPO_BRANCH = "openshift";

	private static final String PROJECT_NAME = "helloworld";
	
	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;
	
	@BeforeClass	
	public static void waitForRunningApplication() {
		new ProjectExplorer().deleteAllProjects(true);
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, "eap-app-1", ResourceState.COMPLETE, projectReq.getProjectName(), connectionReq.getConnection()),
				TimePeriod.getCustom(1000));
		
		OpenShiftExplorerView openShiftExplorerView = new OpenShiftExplorerView();
		OpenShiftProject project = openShiftExplorerView.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName());
		new WaitUntil(new ApplicationPodIsRunning(project), TimePeriod.LONG);
		cloneGitRepoAndImportProject();
	}
	
	
	private static void cloneGitRepoAndImportProject() {
		OpenShiftUtils.cloneGitRepository(GIT_REPO_DIRECTORY,GIT_REPO_URL, GIT_REPO_BRANCH, true);
		OpenShiftUtils.importProjectUsingSmartImport(GIT_REPO_DIRECTORY, PROJECT_NAME);
	}

	@Test
	public void testAutomaticPublish() {
		createServerAdapter();
		changeProjectAndVerifyAutoPublish();
		verifyChangesTookEffect();
	}
	
	private void createServerAdapter() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName()).getServicesWithName(OpenShiftResources.EAP_SERVICE).get(0).createServerAdapter();
	}
	
	private void changeProjectAndVerifyAutoPublish() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		projectExplorer.getProject(PROJECT_NAME).select();
		ProjectItem projectItem = projectExplorer.getProject(PROJECT_NAME).getProjectItem("Java Resources", "src/main/java",
				"org.jboss.as.quickstarts.helloworld", "HelloService.java");
		projectItem.select();
		projectItem.open();
		
		TextEditor textEditor = new TextEditor("HelloService.java");
		textEditor.setText(PUBLISHED_CODE);
		textEditor.close(true);
		
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT);
		new WaitUntil(new ConsoleHasNoChange(), TimePeriod.VERY_LONG);
	
		assertTrue("Local changes performed to project have not been autopublished, or at least rsync "
					+ "output in console view does not contain information about sending incremental list of changes,"
					+ "specifically with changed class " + changedClass,
				new ConsoleView().getConsoleText().contains(changedClass));
	}
	
	private void verifyChangesTookEffect() {
		new ServerAdapter(Version.OPENSHIFT3, OpenShiftResources.EAP_SERVICE, "Service").select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.SHOW_IN_WEB_BROWSER).select();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		try {
			new WaitUntil(new BrowserContainsText("Hello"), TimePeriod.VERY_LONG);			
		} catch (WaitTimeoutExpiredException ex) {
			fail("Application was not deployed successfully because it is not shown in web browser properly.");
		}
	}
	
	@AfterClass
	public static void removeAdapterAndApplication() {
		try {
			OpenShiftUtils.killJobs();
			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
			new ServerAdapter(Version.OPENSHIFT3, OpenShiftResources.EAP_SERVICE, "Service").delete();
		} catch (OpenShiftToolsException ex) {
			// do nothing, adapter does not exists
		}
		new ProjectExplorer().getProject(PROJECT_NAME).delete(false);
		TestUtils.cleanupGitFolder(new File(GIT_REPO_DIRECTORY));
	}
}
