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
package org.jboss.tools.openshift.ui.bot.test.application.v3.advanced;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.condition.TreeHasChildren;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.Matcher;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.ButtonWithTextIsAvailable;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.junit.Before;
import org.junit.Test;

@OpenPerspective(value=JBossPerspective.class)
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = "eap-app", template = "resources/eap70-basic-s2i-helloworld.json")
public class ImportApplicationTest {
	
	public static String PROJECT_NAME = "jboss-helloworld";
	
	private static final String GIT_REPO_DIRECTORY = "target/git_repo";
	
	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;
	
	@Before
	public void cleanGitFolder() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		
		if (projectExplorer.containsProject(PROJECT_NAME)) {
			projectExplorer.getProject(PROJECT_NAME).delete(true);
		}
		
		try {
			TestUtils.delete(new File(GIT_REPO_DIRECTORY));
		} catch (IOException e) {
			throw new RuntimeException("Deletion of git repo was unsuccessfull.", e);
		}
	}
	
	@Test
	public void testImportOpenShiftApplicationViaOpenShiftExplorer() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD_CONFIG,(Matcher<String>) null, ResourceState.UNSPECIFIED,
				projectReq.getProjectName()), TimePeriod.LONG);
		
		explorer.getOpenShift3Connection().getProject(projectReq.getProjectName()).getOpenShiftResources(Resource.BUILD_CONFIG).get(0).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.IMPORT_APPLICATION);
		new CheckBox("Use default clone destination").toggle(false);
		File gitRepo = new File(GIT_REPO_DIRECTORY);
		new LabeledText("Git Clone Location:").setText(gitRepo.getAbsolutePath());
		
		new FinishButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION), TimePeriod.VERY_LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		assertTrue("There should be imported kitchen sink project, but there is not",
				projectExplorer.containsProject(PROJECT_NAME));
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	@Test
	public void testImportOpenShiftApplicationViaShellMenu() {
		new ShellMenuItem("File", "Import...").select();
		
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.IMPORT);
		new DefaultTreeItem("OpenShift", "Existing OpenShift Application").select();
		new NextButton().click();
		
		new DefaultShell(OpenShiftLabel.Shell.IMPORT_APPLICATION);
		TestUtils.acceptSSLCertificate();
		
		new NextButton().click();
		TestUtils.acceptSSLCertificate();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
			
		new WaitUntil(new ButtonWithTextIsAvailable("Refresh"), TimePeriod.VERY_LONG);
		new WaitUntil(new TreeHasChildren(new DefaultTree()), TimePeriod.DEFAULT);
		
		
		new DefaultTreeItem(projectReq.getProjectName() + " " + projectReq.getProjectName()).getItems().
			get(0).select();
		
		new WaitUntil(new ControlIsEnabled(new NextButton()), TimePeriod.DEFAULT);
		
		new NextButton().click();
		new FinishButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION), TimePeriod.VERY_LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		assertTrue("There should be imported " + PROJECT_NAME + "project, but there is not",
				projectExplorer.containsProject(PROJECT_NAME));
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
}
