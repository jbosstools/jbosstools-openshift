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
package org.jboss.tools.openshift.ui.bot.test.application.v3.create;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizardDialog;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tab.DefaultTabItem;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringStartsWith;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeProjectUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.TemplateParametersTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(value=JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@RequiredBasicConnection
@CleanConnection
public class CreateApplicationFromTemplateTest extends AbstractTest {

	private String gitFolder = "jboss-eap-quickstarts";
	private String helloworldProject = "helloworld";
	private String kitchensinkProject = "kitchensink";

	private static final String TESTS_PROJECT = "os3projectWithResources";
	private static final String TESTS_PROJECT_LOCATION = new File("resources/os3projectWithResources")
			.getAbsolutePath();
	private static final String URL = "https://raw.githubusercontent.com/jbosstools/jbosstools-openshift/master/itests/org.jboss.tools.openshift.ui.bot.test/resources/eap72-basic-s2i-helloworld.json";

	private String genericWebhookURL;
	private String githubWebhookURL;

	private String srcRepoURI;
	private String applicationName;

	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;
	
	@InjectRequirement
	private CleanOpenShiftConnectionRequirement cleanReq;

	@BeforeClass
	public static void importTestsProject() {
		new ExternalProjectImportWizardDialog().open();
		new DefaultCombo().setText(TESTS_PROJECT_LOCATION);
		new PushButton("Refresh").click();

		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.LONG);

		new FinishButton().click();

		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new ProjectExists(TESTS_PROJECT), TimePeriod.LONG);
	}

	@Before
	public void setUp() {
		DatastoreOS3.generateProjectName();
		OpenShift3NativeProjectUtils.getOrCreateProject(DatastoreOS3.PROJECT1,
			DatastoreOS3.PROJECT1_DISPLAYED_NAME, StringUtils.EMPTY, connectionReq.getConnection());
		
		TestUtils.cleanupGitFolder(gitFolder);
		deleteProject(kitchensinkProject);
		deleteProject(helloworldProject);

		genericWebhookURL = null;
		githubWebhookURL = null;
		srcRepoURI = null;
		applicationName = null;
	}

	private void deleteProject(String name) {
		if (new ProjectExists(name).test()) {
			new ProjectExplorer().getProject(name).delete(true);
		}
	}

	@Test
	public void createApplicationFromLocalWorkspaceTemplate() {
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer(DatastoreOS3.PROJECT1_DISPLAYED_NAME);
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		new PushButton(OpenShiftLabel.Button.BROWSE_WORKSPACE).click();

		new DefaultShell(OpenShiftLabel.Shell.SELECT_OPENSHIFT_TEMPLATE);
		new DefaultTreeItem(TESTS_PROJECT, OpenShiftResources.EAP_TEMPLATE_RESOURCES_FILENAME).select();
		new OkButton().click();

		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		assertTrue("Template from workspace is not correctly shown in text field containing its path",
				new LabeledText(OpenShiftLabel.TextLabels.SELECT_LOCAL_TEMPLATE).getText().equals("${workspace_loc:"
						+ File.separator + TESTS_PROJECT + File.separator + OpenShiftResources.EAP_TEMPLATE_RESOURCES_FILENAME +"}"));

		new WaitUntil(new ControlIsEnabled(new CancelButton()));

//		TODO: Remove comment once JBIDE-24492 is resolved	
//		assertTrue("Defined resource button should be enabled",
//				new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES).isEnabled());

		completeApplicationCreationAndVerify(helloworldProject, 2);
	}

	@Test
	public void createApplicationFromLocalFileSystemTemplate() {
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer(DatastoreOS3.PROJECT1_DISPLAYED_NAME);
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		new LabeledText(OpenShiftLabel.TextLabels.SELECT_LOCAL_TEMPLATE).setText(
				TESTS_PROJECT_LOCATION + File.separator + OpenShiftResources.EAP_TEMPLATE_RESOURCES_FILENAME);

//		TODO: Remove comment once JBIDE-24492 is resolved			
//		assertTrue("Defined resource button should be enabled",
//				new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES).isEnabled());

		completeApplicationCreationAndVerify(helloworldProject, 2);
	}

	@Test
	public void createApplicationFromTemplateProvidedByURL() {
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer(DatastoreOS3.PROJECT1_DISPLAYED_NAME);
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		new LabeledText(OpenShiftLabel.TextLabels.SELECT_LOCAL_TEMPLATE).setText(URL);
		
// 		TODO: Remove comment once JBIDE-24492 is resolved	
//		assertTrue("Defined resource button should be enabled",
//				new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES).isEnabled());

		completeApplicationCreationAndVerify(helloworldProject, 2);
	}

	@Test
	public void testCreateApplicationFromServerTemplate() {
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer(DatastoreOS3.PROJECT1_DISPLAYED_NAME);
		OpenShiftUtils.selectEAPTemplate();

		completeApplicationCreationAndVerify(kitchensinkProject, 2);
	}

	private void completeApplicationCreationAndVerify(String projectName, int numberOfExpectedServices) {
		completeWizardAndVerify();
		importApplicationAndVerify(projectName);
		verifyCreatedApplication(numberOfExpectedServices);
	}

	private void completeWizardAndVerify() {
		new WaitUntil(new ControlIsEnabled(new NextButton()), TimePeriod.DEFAULT);

		new NextButton().click();

		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);

		String srcRepoRef = new DefaultTable().getItem(TemplateParametersTest.SOURCE_REPOSITORY_REF).getText(1);
		srcRepoURI = new DefaultTable().getItem(TemplateParametersTest.SOURCE_REPOSITORY_URL).getText(1);
		String contextDir = new DefaultTable().getItem(TemplateParametersTest.CONTEXT_DIR).getText(1);
		applicationName = new DefaultTable().getItem(TemplateParametersTest.APPLICATION_NAME).getText(1);
		new NextButton().click();

		new WaitWhile(new ControlIsEnabled(new NextButton()), TimePeriod.LONG);

		new FinishButton().click();

		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.APPLICATION_SUMMARY), TimePeriod.LONG);

		new DefaultShell(OpenShiftLabel.Shell.APPLICATION_SUMMARY);

		assertTrue(
				TemplateParametersTest.SOURCE_REPOSITORY_REF + " is not same as the one shown in "
						+ "New OpenShift Application wizard.",
				new DefaultTable().getItem(TemplateParametersTest.SOURCE_REPOSITORY_REF).getText(1).equals(srcRepoRef));
		assertTrue(
				TemplateParametersTest.SOURCE_REPOSITORY_URL.split(" ")[0] + " is not same as the one shown in "
						+ "New OpenShift Application wizard.",
				new DefaultTable().getItem(TemplateParametersTest.SOURCE_REPOSITORY_URL.split(" ")[0]).getText(1)
						.equals(srcRepoURI));
		assertTrue(
				TemplateParametersTest.CONTEXT_DIR + " is not same as the one shown in New OpenShift"
						+ " Application wizard.",
				new DefaultTable().getItem(TemplateParametersTest.CONTEXT_DIR).getText(1).equals(contextDir));
		assertTrue(
				TemplateParametersTest.APPLICATION_NAME.split(" ")[0] + " is not same as the one shown in "
						+ "New OpenShift Application wizard.",
				new DefaultTable().getItem(TemplateParametersTest.APPLICATION_NAME.split(" ")[0]).getText(1)
						.equals(applicationName));
		assertFalse(TemplateParametersTest.GENERIC_SECRET.split(" ")[0] + " should be generated and non-empty.",
				new DefaultTable().getItem(TemplateParametersTest.GENERIC_SECRET.split(" ")[0]).getText(1).isEmpty());
		assertFalse(TemplateParametersTest.GITHUB_SECRET.split(" ")[0] + " should be generated and non-empty.",
				new DefaultTable().getItem(TemplateParametersTest.GITHUB_SECRET.split(" ")[0]).getText(1).isEmpty());

		new DefaultLink("Click here to display the webhooks available to automatically trigger builds.").click();

		new DefaultShell(OpenShiftLabel.Shell.WEBHOOK_TRIGGERS);
		genericWebhookURL = new DefaultText(0).getText();
		githubWebhookURL = new DefaultText(1).getText();

		assertFalse("Generic webhook URL should not be empty.", genericWebhookURL.isEmpty());
		assertFalse("GitHub webhook URL should not be empty.", githubWebhookURL.isEmpty());

		new OkButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.WEBHOOK_TRIGGERS));

		new DefaultShell(OpenShiftLabel.Shell.APPLICATION_SUMMARY);
		new OkButton().click();
	}

	public static void importApplicationAndVerify(String projectName) {
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));

		new DefaultShell(OpenShiftLabel.Shell.IMPORT_APPLICATION);
		try {
			new CheckBox(new WithTextMatcher(new StringStartsWith("Do not clone"))).toggle(true);
		} catch (CoreLayerException ex) {
			// git directory is not in use 
		} catch (WaitTimeoutExpiredException ex) {
			//swallow, checkbox is disabled
		}
		new FinishButton().click();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
				
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();

		OpenShiftUtils.handleCheatSheetCreateServerAdapter();
		
		new WaitUntil(new ProjectExists(projectName, new ProjectExplorer()), TimePeriod.LONG, false);
		assertTrue("Project Explorer should contain imported project " + projectName,
				projectExplorer.containsProject(projectName));
	}

	private void verifyCreatedApplication(int numberOfExpectedServices) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		OpenShiftProject project = explorer.getOpenShift3Connection(connectionReq.getConnection())
				.getProject(DatastoreOS3.PROJECT1_DISPLAYED_NAME);
		project.refresh();

		new WaitWhile(new JobIsRunning(), TimePeriod.getCustom(120));
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD_CONFIG, (Matcher<String>) null,
				ResourceState.UNSPECIFIED, DatastoreOS3.PROJECT1_DISPLAYED_NAME, connectionReq.getConnection()), TimePeriod.LONG, false);

		List<OpenShiftResource> buildConfig = project.getOpenShiftResources(Resource.BUILD_CONFIG);
		assertTrue("There should be precisely 1 build config for created application, but there is following amount"
				+ " of build configs: " + buildConfig.size(), buildConfig.size() == 1);
		assertTrue(
				"There should be application name and git URI in build config tree item, but they are not."
						+ "Application name is '" + applicationName + "' and git URI is '" + srcRepoURI
						+ "', but build " + "config has name '" + buildConfig.get(0).getName() + "'",
				buildConfig.get(0).getPropertyValue("Labels", "application").equals(applicationName)
						&& buildConfig.get(0).getPropertyValue("Source", "URI").equals(srcRepoURI));

		List<OpenShiftResource> imageStream = project.getOpenShiftResources(Resource.IMAGE_STREAM);
		assertTrue("There should be precisely 1 image stream for created application, but there is following amount"
				+ " of image streams: " + imageStream.size(), imageStream.size() == 1);

		List<OpenShiftResource> routes = project.getOpenShiftResources(Resource.ROUTE);
		assertTrue("There should be precisely 1 route for created application, but there is following amount"
				+ " of routes:" + routes.size(), routes.size() == 1);
		assertTrue("Generated (default) route should contain application name, but it's not contained.",
				routes.get(0).getName().equals(applicationName));

		List<OpenShiftResource> services = project.getOpenShiftResources(Resource.SERVICE);
		assertTrue("There should be precisely " + numberOfExpectedServices + " service(s) for created application, but there is following amount"
				+ " of services: " + services.size(), services.size() == numberOfExpectedServices);
	}

	@After
	public void tearDown() {
		OpenShiftUtils.killJobs();
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();

		deleteProject(kitchensinkProject);
		deleteProject(helloworldProject);

		cleanReq.fulfill();
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
	}

	@AfterClass
	public static void deleteTestsProjectFromWorkspace() {
		new ProjectExplorer().deleteAllProjects(false);
		DatastoreOS3.generateProjectName();
	}
}
