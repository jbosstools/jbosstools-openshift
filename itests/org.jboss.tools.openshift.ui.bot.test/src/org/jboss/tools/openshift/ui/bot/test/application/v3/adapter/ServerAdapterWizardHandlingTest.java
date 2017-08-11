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
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsKilled;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsActive;
import org.jboss.reddeer.core.lookup.ShellLookup;
import org.jboss.reddeer.core.util.Display;
import org.jboss.reddeer.core.util.ResultRunnable;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardDialog;
import org.jboss.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.swt.api.Button;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.BackButton;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.button.NextButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.swt.impl.tree.DefaultTreeItem;
import org.jboss.tools.common.reddeer.utils.FileUtils;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

@OCBinary
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = "eap-app", template = "resources/eap64-basic-s2i.json")
public class ServerAdapterWizardHandlingTest {
	
	private static final String PROJECT_NAME = "kitchensink";

	private static final String GIT_REPO_URL = "https://github.com/jboss-developer/jboss-eap-quickstarts";
	
	private static final String GIT_REPO_DIRECTORY = "target/git_repo";
	
	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;
	
	@BeforeClass
	public static void waitTillApplicationIsRunning() {
		cloneGitRepoAndImportProject();
	}
	
	private static void cloneGitRepoAndImportProject() {
		cloneGitRepository();
		importProjectUsingSmartImport();
	}
	
	private static void cloneGitRepository() {
		try {
			FileUtils.deleteDirectory(new File(GIT_REPO_DIRECTORY));
			Git.cloneRepository().setURI(GIT_REPO_URL).setDirectory(new File(GIT_REPO_DIRECTORY)).call();
		} catch (GitAPIException|IOException e) {
			throw new RuntimeException("Unable to clone git repository from " + GIT_REPO_URL, e);
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
	public void testPreselectedConnectionForNewOpenShift3ServerAdapter() {
		openNewServerAdapterWizard();

		assertTrue("There should be preselected an existing OpenShift 3 connection in new server adapter wizard.",
				new LabeledCombo(OpenShiftLabel.TextLabels.CONNECTION).getSelection().contains(DatastoreOS3.USERNAME));
	}

	@Test
	public void testProjectSelectedInProjectExplorerIsPreselected() {
		new ProjectExplorer().selectProjects(PROJECT_NAME);

		openNewServerAdapterWizard();
		next();

		String eclipseProject = Display.syncExec(new ResultRunnable<String>() {

			@Override
			public String run() {
				return new LabeledText("Eclipse Project: ").getSWTWidget().getText();
			}

		});

		assertTrue("Selected project from workspace should be preselected", eclipseProject.equals(PROJECT_NAME));
	}

	@Test
	public void testPodPathWidgetAccessibility() {
		openNewServerAdapterWizard();
		next();

		new PushButton(OpenShiftLabel.Button.ADVANCED_OPEN).click();

		new CheckBox("Use inferred Pod Deployment Path").toggle(false);

		LabeledText podPath = new LabeledText("Pod Deployment Path: ");
		String podDeploymentPath = "/opt/eap/standalone/deployments/";
		podPath.setText("");

		assertFalse("Next button should be disable if pod path is empty is selected.", nextButtonIsEnabled());

		podPath.setText(podDeploymentPath);

		assertTrue("Next button should be reeenabled if pod path is correctly filled in.", nextButtonIsEnabled());
	}

	@Test
	public void testApplicationSelectionWidgetAccessibility() {
		openNewServerAdapterWizard();
		next();

		new DefaultTreeItem(projectReq.getProjectName()).select();

		assertFalse("Next button should be disable if no application is selected.", nextButtonIsEnabled());

		new DefaultTreeItem(projectReq.getProjectName()).getItems().get(0).select();

		assertTrue("Next button should be enabled if application for a new server adapter is created.",
				nextButtonIsEnabled());
	}

	@Test
	public void testFinishButtonAccessibility() {
		openNewServerAdapterWizard();

		assertFalse("Finish button should be disabled on new server "
				+ "adapter wizard page where selection of a connection is done, "
				+ "because there are still missing details to successfully create a new"
				+ "OpenShift 3 server adapter.", buttonIsEnabled(new FinishButton()));
	}

	@Test
	public void testSourcePathWidgetAccessibility() {
		openNewServerAdapterWizard();

		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);

		next();

		new PushButton(OpenShiftLabel.Button.ADVANCED_OPEN).click();

		LabeledText srcPath = new LabeledText("Source Path: ");
		String srcPathText = srcPath.getText();
		srcPath.setText("");

		assertFalse("Next button should be disable if source path is empty is selected.", nextButtonIsEnabled());

		srcPath.setText(srcPathText);

		assertTrue("Next button should be reeenabled if source path is correctly filled in.", nextButtonIsEnabled());

		srcPath.setText("invalid path");

		assertFalse("Next button should be disabled if source path is invalid or not existing.", nextButtonIsEnabled());

		srcPath.setText(srcPathText);

		assertTrue("Next button should be reeenabled if source path is correctly filled in.", nextButtonIsEnabled());
	}

	private boolean nextButtonIsEnabled() {
		return buttonIsEnabled(new NextButton());
	}

	private boolean buttonIsEnabled(Button button) {
		try {
			new WaitUntil(new WidgetIsEnabled(button), TimePeriod.getCustom(5));
			return true;
		} catch (WaitTimeoutExpiredException ex) {
			return false;
		}
	}

	private void next() {
		new NextButton().click();
		TestUtils.acceptSSLCertificate();

		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new WidgetIsEnabled(new BackButton()));
	}

	private void openNewServerAdapterWizard() {
		NewServerWizardDialog dialog = new NewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage();

		dialog.open();
		new WaitUntil(new JobIsKilled("Refreshing server adapter list"), TimePeriod.LONG, false);
		page.selectType(OpenShiftLabel.Others.OS3_SERVER_ADAPTER);
		dialog.next();
	}

	@After
	public void closeShell() {
		Shell shell = ShellLookup.getInstance().getShell(OpenShiftLabel.Shell.ADAPTER);
		if (shell != null) {
			new DefaultShell(OpenShiftLabel.Shell.ADAPTER);
			new CancelButton().click();
			new WaitWhile(new ShellWithTextIsActive(OpenShiftLabel.Shell.ADAPTER));
		}

		new WaitWhile(new JobIsRunning());
	}
}
