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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.matcher.RegexMatcher;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.ui.browser.BrowserEditor;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.NoButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.jboss.tools.common.reddeer.utils.FileUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.view.resources.Service;
import org.jboss.tools.openshift.reddeer.wizard.importapp.ImportApplicationWizard;
import org.jboss.tools.openshift.reddeer.wizard.server.ServerSettingsWizard;

@SuppressWarnings("restriction")
public class OpenShiftUtils {
	
	public static OpenShiftResource getOpenShiftPod(String projectName, String podName, Connection connection) {
		return getOpenShiftPod(projectName, new IsEqual<String>(podName), connection);
	}
	
	public static OpenShiftResource getOpenShiftPod(String projectName, Matcher<String> matcher, Connection connection) {
		return getOpenShiftPod(projectName, Resource.POD, matcher, connection);
	}
	
	public static OpenShiftResource getOpenShiftPod(String projectName, Resource resource, Matcher<String> matcher, Connection connection) {
		assertTrue(!StringUtils.isBlank(projectName));

		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		List<OpenShiftResource> pods = explorer.getOpenShift3Connection(connection).getProject(projectName)
				.getOpenShiftResources(resource);
		for (OpenShiftResource pod : pods) {
				if (matcher.matches(pod.getName())) {
					return pod;
				}
		}
		fail("Pod with matcher:" + matcher.toString() + " for project " + projectName + " was not found");
		return null;
	}

	public static OpenShiftProject getOpenShiftProject(String projectName, Connection connection) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		OpenShiftProject project = explorer.getOpenShift3Connection(connection).getProject(projectName);
		assertThat("Could not find project " + projectName + " in OpenShift Explorer",  project, notNullValue());
		project.expand();
		return project;
	}
	
	public static void deleteAllProjects(Connection connection) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection(connection).refresh();
		new WaitWhile(new JobIsRunning());
		List<OpenShiftProject> projects = explorer.getOpenShift3Connection(connection).getAllProjects();
		for (OpenShiftProject project : projects) {
			if (project != null) {
				String projectName = project.getName();
				try {
					project.delete();
					new WaitWhile(new OpenShiftProjectExists(projectName, connection), TimePeriod.LONG);
				} catch (CoreLayerException ex) {
					// project does not exist - project has been deleted but view has not been
					// refreshed
					explorer.getOpenShift3Connection(connection).refresh();
				}
			}
		}
	}
	
	public static void closeBrowser() {
		try {
			BrowserEditor browser = new BrowserEditor(new RegexMatcher(".*"));
			while (browser != null) {
				browser.close();
				try {
					browser = new BrowserEditor(new RegexMatcher(".*"));
				} catch (CoreLayerException ex) {
					browser = null;
				}
			}
		} catch (CoreLayerException ex) {
			return;
		}
	}
	
	public static ImportApplicationWizard openImportApplicationWizardFromOpenshiftView(OpenShiftProject openshiftProject){
		openshiftProject.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));
		return new ImportApplicationWizard();
	}
	
	public static ServerSettingsWizard openServerSettingsWizardFromOpenshiftView(Service openshiftService){
		openshiftService.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_ADAPTER_FROM_EXPLORER).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.SERVER_ADAPTER_SETTINGS), TimePeriod.LONG);
		return new ServerSettingsWizard();
	}
	
	
	public static void handleCheatSheetCreateServerAdapter () {
		try {
			new WaitUntil(new ShellIsAvailable(new WithTextMatcher(new RegexMatcher(OpenShiftLabel.Shell.CHEATSHEET + "|" + OpenShiftLabel.Shell.CREATE_SERVER_ADAPTER))), TimePeriod.LONG);
			new NoButton().click();
			new DefaultShell("Create server adapter");
			new NoButton().click();
		} catch (CoreLayerException ex) {
			// Swallow, shells are not opened
		} catch (WaitTimeoutExpiredException e) {
			// Also swallow, shells are not opened
		}
	}
	
	public static void importProjectUsingSmartImport(String gitRepoDirectory, String projectName) {
		SmartImportJob job = new SmartImportJob(new File(gitRepoDirectory + File.separator + projectName),
				Collections.emptySet(), true, true);
		HashSet<File> directory = new HashSet<File>();
		directory.add(new File(gitRepoDirectory + File.separator + projectName));
		job.setDirectoriesToImport(directory);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				job.schedule();
			}
		});
		OpenShiftUtils.handleCheatSheetCreateServerAdapter();
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
	}
	
	public static void cloneGitRepository(String gitRepoDirectory, String gitRepoURL) {
		cloneGitRepository(gitRepoDirectory, gitRepoURL, false);
	}
	
	public static void cloneGitRepository(String gitRepoDirectory, String gitRepoURL, boolean cleanupFolderBefore) {
		cloneGitRepository(gitRepoDirectory, gitRepoURL, "master", false);
	}
	
	public static void cloneGitRepository(String gitRepoDirectory, String gitRepoURL, String branch, boolean cleanupFolderBefore) {
		if (cleanupFolderBefore) {
			TestUtils.cleanupGitFolder(new File(gitRepoDirectory));
		}
		try {
			FileUtils.deleteDirectory(new File(gitRepoDirectory));
			Git.cloneRepository().setURI(gitRepoURL).setDirectory(new File(gitRepoDirectory)).setBranch(branch).call();
		} catch (GitAPIException|IOException e) {
			throw new RuntimeException("Unable to clone git repository from " + gitRepoURL, e);
		}
	}

	public static void cloneGitRepository(String gitRepoDirectory, String gitRepoUrl, String gitRepoName) {
		TestUtils.cleanupGitFolder(new File(gitRepoDirectory),gitRepoName);
		try {
			Git.cloneRepository().setURI(gitRepoUrl).setDirectory(new File(gitRepoDirectory)).call();
		} catch (GitAPIException e) {
			throw new RuntimeException("Unable to clone git repository from " + gitRepoUrl);
		}
		
	}

	public static void killJobs() {
		Job.getJobManager().cancel(null);
	}
	
	public static void updateMavenProject(String projectName) {
		new ProjectExplorer().getProject(projectName);
		new ContextMenuItem("Maven", "Update Project...").select();
		new DefaultShell("Update Maven Project");
		new PushButton("Select All").click();
		new PushButton("OK").click();
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);

	}
	
	public static void selectEAPTemplate () {
		try {
			new DefaultTree().selectItems(new DefaultTreeItem(OpenShiftLabel.Others.EAP_TEMPLATE));
		} catch (CoreLayerException ex) {
			//Select older EAP template(depends on OpenShift version)
			new DefaultTree().selectItems(new DefaultTreeItem(OpenShiftLabel.Others.EAP_TEMPLATE_OLD));
		}	
	}

}
