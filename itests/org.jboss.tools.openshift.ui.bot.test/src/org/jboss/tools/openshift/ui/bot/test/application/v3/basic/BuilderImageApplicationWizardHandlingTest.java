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
package org.jboss.tools.openshift.ui.bot.test.application.v3.basic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.TableItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.spinner.DefaultSpinner;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.wizard.page.EnvironmentVariableWizardPage;
import org.jboss.tools.openshift.reddeer.wizard.page.EnvironmentVariableWizardPage.EnvVar;
import org.jboss.tools.openshift.reddeer.wizard.page.ResourceLabelsWizardPage;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection
@RequiredProject(name="builderimagevalidationproject")
@RunWith(RedDeerSuite.class)
public class BuilderImageApplicationWizardHandlingTest extends AbstractTest {

	public static final String BUILDER_IMAGE = "httpd:latest (builder, httpd) - openshift";
	
	@InjectRequirement
	private OpenShiftProjectRequirement projectRequirement;
	
	private ResourceLabelsWizardPage resourceLabelPage = new ResourceLabelsWizardPage();
	private EnvironmentVariableWizardPage environmentVariablesPage = new EnvironmentVariableWizardPage();
	
	private EnvVar envVar = new EnvVar("varname1", "varvalue1");
	private EnvVar envVar2 = new EnvVar("varname1", "varvalue2");
	private EnvVar homeVar = new EnvVar("HOME", "/opt/app-root/src");
	private EnvVar homeVar2 = new EnvVar("HOME", "/home/jbosstools");
	private EnvVar dataVar = new EnvVar("HTTPD_DATA_PATH", "/var/www");
	private EnvVar dataVar2 = new EnvVar("HTTPD_DATA_PATH", "/temp/www");
				
	@Before
	public void openNewApplicationWizard() {
		new NewOpenShift3ApplicationWizard().openWizardFromExplorer(projectRequirement.getProjectName());
	}
	
	@Test
	public void testCheckButtonsStateForBuildImage() {
		selectBuilderImageAndAssertButtonsAvailability();
		
		new DefaultTreeItem(OpenShiftLabel.Others.EAP_TEMPLATE).select();
		
		assertTrue("Next button should be enabled if EAP template was selected after build image.",
				new NextButton().isEnabled());
		assertFalse("Finish button should be disabled if template is selected after build image.",
				new FinishButton().isEnabled());
		
		selectBuilderImageAndAssertButtonsAvailability();
	}
	
	private void selectBuilderImageAndAssertButtonsAvailability() {
		new DefaultTreeItem(BUILDER_IMAGE).select();
		
		assertTrue("Next button should be enabled if EAP builder image is selected.",
				new NextButton().isEnabled());
		assertFalse("Finish button should be disabled on first wizard page for builder images.",
				new FinishButton().isEnabled());
	}
	
	@Test
	public void testResourceNameOnBuildConfigurationWizardPage() {
		nextToBuildConfigurationWizardPage();
		
		LabeledText resourceName = new LabeledText(OpenShiftLabel.TextLabels.BUILDER_RESOURCE_NAME);
		String defaultName = "httpd";
		
		assertTrue("Resource name has not been inferred correctly.", 
				resourceName.getText().equals(defaultName));
		
		validateInvalidResourceName("");
		validateInvalidResourceName(" ");
		validateInvalidResourceName("invAlid");
		validateValidResourceName("valid-name");
		validateInvalidResourceName("-invalid");
		validateValidResourceName("val1d-n4me-r3source");
		validateInvalidResourceName("invalid-");
		validateValidResourceName(defaultName);	
	}
	
	private void validateInvalidResourceName(String name) {
		new LabeledText(OpenShiftLabel.TextLabels.BUILDER_RESOURCE_NAME).setText(name);
		
		assertFalse("Next button should be disabled if resource name is invalid '" + name + "'",
				new NextButton().isEnabled());
		assertFalse("Finish button should be disabled if resource name is invalid '" + name + "'",
				new FinishButton().isEnabled());
	}
	
	private void validateValidResourceName(String name) {
		new LabeledText(OpenShiftLabel.TextLabels.BUILDER_RESOURCE_NAME).setText(name);
		
		assertTrue("Next button should be enabled if resource name is valid '" + name + "'",
				new NextButton().isEnabled());
		assertTrue("Finish button should be enabled if resource name is valid '" + name + "'",
				new FinishButton().isEnabled());
	}
	
	@Test
	public void testGitFieldsValidationOnBuildConfigurationWizardPage() {
		nextToBuildConfigurationWizardPage();
		
		LabeledText gitRepo = new LabeledText(OpenShiftLabel.TextLabels.GIT_REPO_URL);
		LabeledText gitReference = new LabeledText(OpenShiftLabel.TextLabels.GIT_REF);
		LabeledText contextDirectory = new LabeledText(OpenShiftLabel.TextLabels.CONTEXT_DIR);
		
		String defaultRepo = gitRepo.getText();
		String defaultRef = gitReference.getText();
		String defaultContextDir = contextDirectory.getText();
		
		validateGitRepoURL("invalid");
		validateGitRepoURL("");
		
		setDefaultValuesAndAssert(defaultRepo, defaultRef, defaultContextDir);
		
		gitReference.setText("");
		contextDirectory.setText("");
		
		assertTrue("Next button should be enabled if ref and context dir are empty.",
				new NextButton().isEnabled());
		assertTrue("Finish button should be enabled if ref and context dir are empty.",
				new FinishButton().isEnabled());
		
		setDefaultValuesAndAssert(defaultRepo, defaultRef, defaultContextDir);
		
		validateGitReference("invalid reference");
		validateGitReference("invalidad*reference");
		validateGitReference("invalid:reference");
		validateGitReference("@");
		validateGitReference("invalid\reference");
		
		setDefaultValuesAndAssert(defaultRepo, defaultRef, defaultContextDir);
	}
	
	private void setDefaultValuesAndAssert(String defaultRepo, String defaultRef, String defaultContextDir) {
		new LabeledText(OpenShiftLabel.TextLabels.GIT_REPO_URL).setText(defaultRepo);
		new LabeledText(OpenShiftLabel.TextLabels.GIT_REF).setText(defaultRef);
		new LabeledText(OpenShiftLabel.TextLabels.CONTEXT_DIR).setText(defaultContextDir);
		
		assertTrue("Next button should be enabled after setting git values to default.",
				new NextButton().isEnabled());
		assertTrue("Finish button should be enabled after setting git values to default.",
				new FinishButton().isEnabled());
	}
		
	private void validateGitRepoURL(String url) {
		new LabeledText(OpenShiftLabel.TextLabels.GIT_REPO_URL).setText(url);
		
		assertFalse("Next button should be disabled if git repo URL is invalid",
				new NextButton().isEnabled());
		assertFalse("Finish button should be disabled if git repo URL is invalid",
				new FinishButton().isEnabled());
	}
	
	private void validateGitReference(String ref) {
		new LabeledText(OpenShiftLabel.TextLabels.GIT_REF).setText(ref);
		
		assertFalse("Next button should be disabled if git reference is invalid",
				new NextButton().isEnabled());
		assertFalse("Finish button should be disabled if git reference is invalid",
				new FinishButton().isEnabled());
	}
	
	@Test
	public void testManageBuildEnvironmentVariables() {
		nextToBuildConfigurationWizardPage();

		assertFalse("Edit button should be disabled if no environmnent variable is selected.",
				new PushButton(OpenShiftLabel.Button.EDIT).isEnabled());
		assertFalse("Remove button should be disabled if there is no variable selected.",
				new PushButton(OpenShiftLabel.Button.REMOVE_BASIC).isEnabled());
		assertFalse("Reset button should be disabled if there is no change performed on environment variable.",
				new PushButton(OpenShiftLabel.Button.RESET).isEnabled());
		assertFalse("Reset All button should be disabled if there is no change performed on environment variables list.",
				new PushButton(OpenShiftLabel.Button.RESET_ALL).isEnabled());

	 	assertManagmentOfCustomEnvironmentVariable();
	}
	
	private void assertManagmentOfCustomEnvironmentVariable() {
		assertTrue("Table does not contain environment variable" + envVar.getName() + "=" + envVar.getValue(),
	 			environmentVariablesPage.addEnvironmentVariable(envVar));
	 	
		assertTrue("Environment variable is not modified successfully.",
				environmentVariablesPage.editEnvironmentVariable(envVar, envVar2));
		
		assertTrue("Environment variable should no longer be present in table.",
				environmentVariablesPage.removeEnvironmentVariable(envVar));
	}
	
	@Test
	public void testManageDefaultEnvironmentVariable() {
		nextToBuildConfigurationWizardPage();
		next();
		
		new DefaultTable().select(homeVar.getName());
		
		assertFalse("Remove button should be disabled for read only variables",
				new PushButton(OpenShiftLabel.Button.REMOVE_BASIC).isEnabled());
		
		assertTrue("Default variable has not been modified successfully",
				environmentVariablesPage.editEnvironmentVariable(homeVar, homeVar2));
		
		assertTrue("Default variable has not been reset successfully",
				environmentVariablesPage.resetEnvironmentVariable(homeVar2, homeVar));
	
		environmentVariablesPage.editEnvironmentVariable(homeVar, homeVar2);
		environmentVariablesPage.editEnvironmentVariable(dataVar, dataVar2);
		
		assertTrue("Default variables have not been reset successfully", 
				environmentVariablesPage.resetAllVariables(homeVar, dataVar));
		
		assertManagmentOfCustomEnvironmentVariable();
	}
	
	@Test
	public void createLabel() {
		nextToResourceLabelWizardPage();
		String name = "label.name";
		String value = "label-value";
		
		assertTrue("Label " + name + ":" + value + " should be present in the table, but its not there.",
				resourceLabelPage.createLabel(name, value));
	}
	
	@Test
	public void editLabel() {
		nextToResourceLabelWizardPage();
		String name = "label.name";
		String value = "label-value";
		resourceLabelPage.createLabel(name, value);
		
		assertTrue("Label value has not been modified successfully.", 
				resourceLabelPage.editLabel(name, name + "m", value + "m"));
	}
	
	@Test
	public void deleteLabel() {
		nextToResourceLabelWizardPage();
		String name = "label.name";
		String value = "label-value";
		resourceLabelPage.createLabel(name, value);
	
		assertFalse("Label should not be present in the resouce labels table, but it is.",
				resourceLabelPage.deleteLabel(name));		
	}
	
	@Test
	public void testPorts() {
		String defaultName = "8443-tcp";
		String defaultServicePort = "8443";
		String defaultPodPort = "8443";
		String newName = "1234-tcp";
		String newServicePort = "1234";
		String newPodPort = "4321";
		
		nextToBuildConfigurationWizardPage();
		next();
		next();
	
		// Test edit of an existing pod
		new DefaultTable().select(defaultName);
		new PushButton(OpenShiftLabel.Button.EDIT).click();
		
		new DefaultShell(OpenShiftLabel.Shell.SERVICE_PORTS);
		new LabeledText(OpenShiftLabel.TextLabels.POD_PORT).setText(newPodPort);
		new DefaultSpinner(OpenShiftLabel.TextLabels.SERVICE_PORT).setValue(Integer.valueOf(newServicePort));
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.SERVICE_PORTS));
		
		assertTrue("There should port mapping with name " + newName + ", but there is not.",
				new WidgetIsFound(org.eclipse.swt.widgets.TableItem.class,
						new WithTextMatcher(newName)).test());
		
		TableItem portMapping = new DefaultTable().getItem(newName);
		assertTrue("Modified pod mapping has incorrect mapped ports.", portMapping.getText(1)
				.equals(newServicePort) && portMapping.getText(2).equals(newPodPort));
		
		// Test reset of pods
		new PushButton(OpenShiftLabel.Button.RESET).click();
		
		new DefaultShell(OpenShiftLabel.Shell.RESET_PORTS);
		new YesButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.RESET_PORTS));
		new WaitWhile(new JobIsRunning());
		
		assertTrue("There should port mapping with name " + defaultName + ", but there is not.",
				new WidgetIsFound(org.eclipse.swt.widgets.TableItem.class,
						new WithTextMatcher(defaultName)).test());
		
		portMapping = new DefaultTable().getItem(defaultName);
		String resetServicePort = portMapping.getText(1);
		String resetPodPort = portMapping.getText(2);
		
		assertTrue("There should default values for port named " + defaultName + "\n"
				+ "Service port should be " + defaultServicePort + ", but it is " + resetServicePort 
				+ " and pod port should be " + defaultPodPort +", but it is " + resetPodPort, 
				resetServicePort.equals(defaultServicePort) && resetPodPort.equals(defaultPodPort));
	}

	@After
	public void closeNewApplicationWizard() {
		new CancelButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning());
	}
	
	/**************
	  NAVIGATATION
	***************/
	public static void nextToBuildConfigurationWizardPage() {
//		List<TreeItem> templates = new DefaultTree().getAllItems();
//		for (TreeItem item : templates) {
//			if (item.getText().contains(BUILDER_IMAGE)) {
//				item.select();
//				break;
//			}
//		}
		new DefaultTreeItem(BUILDER_IMAGE).select();
		
		
		new WaitUntil(new ControlIsEnabled(new NextButton()));
		
		new NextButton().click();
		
		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);
	}
	
	public static void nextToResourceLabelWizardPage() {
		nextToBuildConfigurationWizardPage();
		next();
		next();
		next();
	}
	
	private static void next() {		
		new NextButton().click();
		
		new WaitUntil(new ControlIsEnabled(new BackButton()));
	}
	
}
