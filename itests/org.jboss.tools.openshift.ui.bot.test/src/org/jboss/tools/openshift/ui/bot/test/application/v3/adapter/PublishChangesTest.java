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
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.jboss.reddeer.eclipse.core.resources.ProjectItem;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.eclipse.ui.console.ConsoleView;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.workbench.impl.editor.TextEditor;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.ApplicationPodIsRunning;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OCBinary
@OpenPerspective(value=JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = "eap-app", template = "resources/eap70-basic-s2i-helloworld.json")
public class PublishChangesTest {

	public static String PUBLISHED_CODE = "package org.jboss.as.quickstarts.helloworld;\n"
			+ "public class HelloService {\n"
			+ "String createHelloMessage(String name) { return \"Hello OpenShift \" + name + \"!\"; }"
			+ "}";
	
	private String changedClass= "ROOT.war/WEB-INF/classes/org/jboss/as/quickstarts/helloworld/HelloService.class";
	
	private static final String GIT_REPO_URL = "https://github.com/jboss-developer/jboss-eap-quickstarts";

	private static final String GIT_REPO_DIRECTORY = "target/git_repo";

	private static final String PROJECT_NAME = "helloworld";
	
	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;
	
	@BeforeClass	
	public static void waitForRunningApplication() {
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, "eap-app-1", ResourceState.COMPLETE, projectReq.getProjectName()),
				TimePeriod.getCustom(1000));
		
		OpenShiftExplorerView openShiftExplorerView = new OpenShiftExplorerView();
		OpenShiftProject project = openShiftExplorerView.getOpenShift3Connection().getProject(projectReq.getProjectName());
		new WaitUntil(new ApplicationPodIsRunning(project), TimePeriod.LONG);
		cloneGitRepoAndImportProject();
	}
	
	
	private static void cloneGitRepoAndImportProject() {
		cloneGitRepository();
		importProjectUsingSmartImport();
	}

	private static void cloneGitRepository() {
		TestUtils.cleanupGitFolder(new File(GIT_REPO_DIRECTORY),"jboss-eap-quickstarts");
		try {
			Git.cloneRepository().setURI(GIT_REPO_URL).setDirectory(new File(GIT_REPO_DIRECTORY)).call();
		} catch (GitAPIException e) {
			throw new RuntimeException("Unable to clone git repository from " + GIT_REPO_URL);
		}
	}

	@SuppressWarnings("restriction")
	private static void importProjectUsingSmartImport() {
		SmartImportJob job = new SmartImportJob(new File(GIT_REPO_DIRECTORY + File.separator + PROJECT_NAME),
				Collections.emptySet(), true, true);
		HashSet<File> directory = new HashSet<File>();
		directory.add(new File(GIT_REPO_DIRECTORY + File.separator + PROJECT_NAME));
		job.setDirectoriesToImport(directory);
		job.run(new NullProgressMonitor());
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
	}
	
	@Test
	public void testAutomaticPublish() {
		createServerAdapter();
		changeProjectAndVerifyAutoPublish();
		verifyChangesTookEffect();
	}
	
	private void createServerAdapter() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection().getProject(projectReq.getProjectName()).getService("eap-app").createServerAdapter();
	}
	
	private void changeProjectAndVerifyAutoPublish() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		ProjectItem projectItem = projectExplorer.getProject(PROJECT_NAME).getProjectItem("Java Resources", "src/main/java",
				"org.jboss.as.quickstarts.helloworld", "HelloService.java");
		projectItem.select();
		projectItem.open();
		
		TextEditor textEditor = new TextEditor("HelloService.java");
		textEditor.setText(PUBLISHED_CODE);
		textEditor.close(true);
		
		new WaitWhile(new JobIsRunning(), TimePeriod.NORMAL);
		new WaitUntil(new ConsoleHasNoChange(), TimePeriod.LONG);
	
		assertTrue("Local changes performed to project have not been autopublished, or at least rsync "
					+ "output in console view does not contain information about sending incremental list of changes,"
					+ "specifically with changed class " + changedClass,
				new ConsoleView().getConsoleText().contains(changedClass));
	}
	
	private void verifyChangesTookEffect() {
		new ServerAdapter(Version.OPENSHIFT3, "eap-app", "Service").select();
		new ContextMenu(OpenShiftLabel.ContextMenu.SHOW_IN_WEB_BROWSER).select();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		try {
			new WaitUntil(new BrowserContainsText("Hello OpenShift"), TimePeriod.VERY_LONG);			
		} catch (WaitTimeoutExpiredException ex) {
			fail("Application was not deployed successfully because it is not shown in web browser properly.");
		}
	}
	
	@AfterClass
	public static void removeAdapterAndApplication() {
		try {
			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
			new ServerAdapter(Version.OPENSHIFT3, "eap-app", "Service").delete();
		} catch (OpenShiftToolsException ex) {
			// do nothing, adapter does not exists
		}
	}
}
