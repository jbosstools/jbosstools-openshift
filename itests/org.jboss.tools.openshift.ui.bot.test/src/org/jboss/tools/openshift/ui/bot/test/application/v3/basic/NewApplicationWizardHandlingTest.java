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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tab.DefaultTabItem;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeProjectUtils;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection
@CleanConnection
@RunWith(RedDeerSuite.class)
public class NewApplicationWizardHandlingTest extends AbstractTest {

	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;
	
	@BeforeClass
	public static void updateProjectName() {
		DatastoreOS3.generateProjectName();
		DatastoreOS3.generateProject2Name();
	}
	
	@Before
	public void openNewApplicationWizard() {
		OpenShift3NativeProjectUtils.getOrCreateProject(DatastoreOS3.PROJECT1,
				DatastoreOS3.PROJECT1_DISPLAYED_NAME, StringUtils.EMPTY, connectionReq.getConnection());
		OpenShift3NativeProjectUtils.getOrCreateProject(DatastoreOS3.PROJECT2,
				StringUtils.EMPTY, StringUtils.EMPTY, connectionReq.getConnection());
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer();
	}
	
	@Test
	public void testTemplatesRelatedWidgetAccess() {
		assertTrue("Server template selection should be chosen by default.",
				new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).isSelected());
		
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		
		try {
			new DefaultTree();
			fail("Tree with server templates should not be visible if local template "
					+ "tab item is selected.");
		} catch (RedDeerException ex) {
			// pass
		}
		assertTrue("Browse button should be visible and enabled while local template "
				+ "tab item is selected.", new PushButton(OpenShiftLabel.Button.BROWSE).isEnabled());
		
		new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).activate();
		
		assertTrue("Tree with server templates should be visible and enabled if server template "
				+ "tab item is selected.", new DefaultTree().isEnabled());
		try {
			new PushButton(1, new WithTextMatcher(OpenShiftLabel.Button.BROWSE));
			fail("Browse button should not be visible while server template tab item is selected.");
		} catch (RedDeerException ex) {
			// pass
		}
	}
	
	@Test
	public void testSwitchProject() {
		List<String> projects = new ArrayList<String>();
		String project1Text = DatastoreOS3.PROJECT1_DISPLAYED_NAME + " (" + DatastoreOS3.PROJECT1 + ")";
		String project2Text = DatastoreOS3.PROJECT2 + " (" + DatastoreOS3.PROJECT2 + ")"; 
		projects.add(project1Text);
		projects.add(project2Text);
		
		LabeledCombo projectCombo = new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT);
		
		assertTrue("Project combo should contain projects " + 
				Arrays.toString(projects.toArray()) + 
				" but those projects are not there. Combo contains following projects: " +
				Arrays.toString(projectCombo.getItems().toArray()),
				projectCombo.getItems().contains(project2Text));
		
		projectCombo.setSelection(project2Text);
		projectCombo.setSelection(project1Text);
		projectCombo.setSelection(project2Text);
	}
	
	@Test(expected = OpenshiftTestInFailureException.class)
	public void testAccessibilityOfDefinedResourcesButton() {
		new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).activate();
		
		assertDefinedResourcesButtonIsNotPresent();
		
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		
		assertDefinedResourcesButtonIsNotPresent();
		
		new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).activate();
		OpenShiftUtils.selectEAPTemplate();
		
		assertTrue("Defines Resources button should be enabled if a server template is selected.", 
				new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES).isEnabled());
		
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		
		assertDefinedResourcesButtonIsNotPresent();
		
		new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).activate();
		new DefaultTree().unselectAllItems();
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		Display.syncExec(new Runnable() {
			@Override
			public void run() {
				new DefaultText(1).getSWTWidget().setText(DatastoreOS3.TEMPLATE_PATH);
			}
		});
		try {
			assertTrue("Defines Resources button should be enabled if a local template is selected.",
					new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES).isEnabled());
		} catch (CoreLayerException ex) {
			throw new OpenshiftTestInFailureException(
					"Defined resources button was not found. Probable cause: JBIDE-24492", ex);
		}
		
		new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).activate();
		
		assertDefinedResourcesButtonIsNotPresent();
	}
	
	private void assertDefinedResourcesButtonIsNotPresent() {
		try {
			new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES);
			fail("Defined Resources button should not be present at this point.");
		} catch (RedDeerException ex) {
			// pass
		}
	}
	
	@Test
	public void testFilteringServerTemplates() {
		DefaultText searchBar = new DefaultText(1);
		
		searchBar.setText(OpenShiftResources.EAP_TEMPLATE);
		assertTrue("There should be precisely one tree item in a tree.",
				new DefaultTree().getItems().size() == 1);
		String templateLabel = new DefaultTree().getItems().get(0).getText();
		assertTrue("There should be item representing basic EAP template in a tree but it is not there.",
				templateLabel.equals(OpenShiftLabel.Others.EAP_TEMPLATE) || templateLabel.equals(OpenShiftLabel.Others.EAP_TEMPLATE_OLD));
		
		searchBar.setText("");
		assertTrue("There should be more templates if search bar does not contain any search query", 
				new DefaultTree().getItems().size() > 2);
	}
	
	@Test(expected = OpenshiftTestInFailureException.class)
	public void testShowDefinedResourcesForLocalTemplate() {
		new DefaultTabItem(OpenShiftLabel.TextLabels.CUSTOM_TEMPLATE).activate();
		Display.syncExec(new Runnable() {
			@Override
			public void run() {
				new DefaultText(1).getSWTWidget().setText(DatastoreOS3.TEMPLATE_PATH);
			}
		});
		
		verifyDefinedResourcesForTemplate();
	}
	
	@Test
	public void testShowDefinedResourcesForServerTemplate() {
		new DefaultTabItem(OpenShiftLabel.TextLabels.SERVER_TEMPLATE).activate();
		OpenShiftUtils.selectEAPTemplate();
		
		verifyDefinedResourcesForTemplate();
	}
	
	private void verifyDefinedResourcesForTemplate() {
		try {
			new WaitUntil(new ControlIsEnabled(new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES)),
					TimePeriod.DEFAULT);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Defined Resources button is not enabled");
		} catch (CoreLayerException ex) {
			// Defined resources button was not found
			throw new OpenshiftTestInFailureException(
					"Defined resources button was not found. Probable cause: JBIDE-24492", ex);
		}
		new PushButton(OpenShiftLabel.Button.DEFINED_RESOURCES).click();
		
		new DefaultShell(OpenShiftLabel.Shell.TEMPLATE_DETAILS);
		List<TreeItem> items = new DefaultTree().getItems();
		
		assertTrue("There should be build config item in tree describing resources", 
				items.get(0).getText().contains("BuildConfig"));
		assertTrue("There should be deployment config item in tree describing resources", 
				items.get(1).getText().contains("DeploymentConfig"));
		assertTrue("There should be image stream item in tree describing resources", 
				items.get(2).getText().contains("ImageStream"));
		assertTrue("There should be route item in tree describing resources", 
				items.get(3).getText().contains("Route"));
		assertTrue("There should be service item in tree describing resources", 
				items.get(4).getText().contains("Service"));
		
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.TEMPLATE_DETAILS), TimePeriod.DEFAULT);
	}
	
	@After
	public void closeNewApplicationWizard() {
		new CancelButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning());
	}
}
