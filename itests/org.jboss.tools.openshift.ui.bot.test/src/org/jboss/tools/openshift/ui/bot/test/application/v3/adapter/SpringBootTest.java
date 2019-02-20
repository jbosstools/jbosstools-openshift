/*******************************************************************************
 * Copyright (c) 2007-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.reddeer.common.matcher.RegexMatcher;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasText;
import org.eclipse.reddeer.eclipse.core.resources.ProjectItem;
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizardDialog;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.WizardProjectsImportPage;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.ctab.DefaultCTabItem;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.hamcrest.core.StringStartsWith;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.ApplicationPodIsRunning;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
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
@RequiredProject(name="fuse-on-openshift")

/*
 * See: https://issues.jboss.org/browse/JBIDE-25303
 * 
 * @author jkopriva@redhat.com
 */
public class SpringBootTest extends AbstractTest  {

	public static String PUBLISHED_CODE = "package org.mycompany;\n" + 
			"\n" + 
			"import org.springframework.stereotype.Component;\n" + 
			"\n" + 
			"/**\n" + 
			" * A sample transform\n" + 
			" */\n" + 
			"@Component(value = \"myTransformer\")\n" + 
			"public class MyTransformer {\n" + 
			"\n" + 
			"    public String transform() {	\n" + 
			"    	return \"NewCode\";\n" + 
			"    }\n" + 
			"\n" + 
			"}";
	
	private static final String PROJECT_NAME = "fuse-on-openshift";
	private static final String FUSE_SERVICE_NAME = PROJECT_NAME;
	
	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;
	
	@BeforeClass	
	public static void waitForRunningApplication() {
		new ProjectExplorer().deleteAllProjects(true);
	}

	@Test
	public void testSpringBootApplication() {
		importProjectFromZip();
		editAndRunLaunchConfiguration();
		podIsRunningAndCheckLogChanging();
		createServerAdapterAndWaitForStart();
		podIsRunningAndCheckLog("OldCode");
		changeSourceCode();
		podIsRunningAndCheckLog("NewCode");
	}
	
	private void importProjectFromZip() {
		try {
			ExternalProjectImportWizardDialog dialog = new ExternalProjectImportWizardDialog();
			dialog.open();
		
			WizardProjectsImportPage page = new WizardProjectsImportPage(dialog);
			page.setArchiveFile(new File("resources/" + PROJECT_NAME + ".zip").getCanonicalPath());
			page.selectProjects(PROJECT_NAME);
			
			dialog.finish(TimePeriod.VERY_LONG);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	private void editAndRunLaunchConfiguration() {
		PackageExplorerPart pexplorer = new PackageExplorerPart();
		pexplorer.open();
		pexplorer.getProject(PROJECT_NAME).select();
		new ContextMenuItem(new WithTextMatcher("Run As"), new RegexMatcher(".*(Maven build...).*")).select();
		new WaitUntil(new ShellIsAvailable("Edit Configuration"));
		new LabeledText("Goals:").setText("clean install fabric8:deploy");
		new DefaultCTabItem("JRE").activate();
		new DefaultText(1).setText("-Dkubernetes.master="+ connectionReq.getConnection().getHost() + "\n" + 
				"-Dkubernetes.namespace=" + projectReq.getProjectName() + "\n" + 
				"-Dkubernetes.auth.basic.username="+ connectionReq.getConnection().getUsername() + "\n" + 
				"-Dkubernetes.auth.basic.password="+ connectionReq.getConnection().getPassword() + "\n" + 
				"-Dfabric8.mode=openshift\n" + 
				"-Dkubernetes.trust.certificates=true\n" + 
				"-Dfabric8.build.strategy=s2i\n" + 
				"-Dkubernetes.auth.tryServiceAccount=false\n" + 
				"-Dfabric8.generator.from=fabric8/s2i-java\n" + 
				"-Dfabric8.generator.fromMode=docker\n" + 
				"-Dkubernetes.auth.tryKubeConfig=false");
		new PushButton("Run").click();
		new WaitUntil(new ConsoleHasText(), TimePeriod.getCustom(30));
		new WaitUntil(new ConsoleHasNoChange(), TimePeriod.getCustom(600));
	}
	
	private void podIsRunningAndCheckLog(String expectedString) {
		OpenShiftResource pod  = OpenShiftUtils.getOpenShiftPod(projectReq.getProjectName(),new StringStartsWith("fuse"), connectionReq.getConnection());
		waitForLog(pod, OpenShiftLabel.ContextMenu.POD_LOG);
		
		ConsoleView consoleView = new ConsoleView();
		consoleView.open();	

		new WaitUntil(new ConsoleHasText(consoleView , expectedString), TimePeriod.LONG);
		consoleView.close();
	}
	
	private void createServerAdapterAndWaitForStart() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName()).getServicesWithName(FUSE_SERVICE_NAME).get(0).createServerAdapter();
	}
	
	private void podIsRunningAndCheckLogChanging() {
		ConsoleView consoleView = new ConsoleView();
		consoleView.open();
		consoleView.toggleShowConsoleOnStandardOutChange(false);
		OpenShiftExplorerView openShiftExplorerView = new OpenShiftExplorerView();
		OpenShiftProject project = openShiftExplorerView.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName());
		project.refresh();
		new WaitUntil(new ApplicationPodIsRunning(project), TimePeriod.VERY_LONG);
		consoleView.close();	
	}
	
	private void changeSourceCode() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		projectExplorer.getProject(PROJECT_NAME).select();
		ProjectItem projectItem = projectExplorer.getProject(PROJECT_NAME).getProjectItem("src/main/java",
				"org.mycompany", "MyTransformer.java");
		projectItem.select();
		projectItem.open();
		
		TextEditor textEditor = new TextEditor("MyTransformer.java");
		textEditor.setText(PUBLISHED_CODE);
		textEditor.close(true);
		
		new WaitWhile(new JobIsRunning(), TimePeriod.MEDIUM);
	}
	
	@AfterClass
	public static void removeAdapterAndApplication() {
		try {
			OpenShiftUtils.killJobs();
			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
			new ServerAdapter(Version.OPENSHIFT3, FUSE_SERVICE_NAME, "Service").delete();
		} catch (OpenShiftToolsException ex) {
			// do nothing, adapter does not exists
		}
		new ProjectExplorer().getProject(PROJECT_NAME).delete(false);
	}
}
