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
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.hamcrest.core.StringContains;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@OpenPerspective(value=JBossPerspective.class)
@RequiredBasicConnection
@CleanConnection
@RequiredProject(
		name = DatastoreOS3.TEST_PROJECT)
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE_RESOURCES_PATH)
public class EditResourcesTest extends AbstractTest {

	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	@InjectRequirement
	private OpenShiftProjectRequirement requiredProject;
	
	private static String GIT_FOLDER = "jboss-eap-quickstarts";
	private String customRepo = "https://github.com/some_user/jboss-eap-quickstarts.git";
	private String originalRepo = "https://github.com/jboss-developer/jboss-eap-quickstarts.git";
	private static String PROJECT_NAME = "jboss-helloworld";
	private String buildConfig;
	
	private static final String BUILD_CONFIG_EDITOR = "[" + DatastoreOS3.TEST_PROJECT + "] Build Config : eap-app.json";
	
	@BeforeClass
	public static void setUp() {
		TestUtils.cleanupGitFolder(GIT_FOLDER);
		TestUtils.setUpOcBinary();
		
	}
	
	@Test
	public void testCanEditResource() {
		getBuildConfig().select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.EDIT).select();
		
		try {
			new TextEditor(BUILD_CONFIG_EDITOR);
			// pass
		} catch (RedDeerException ex) {
			fail("Text editor to modify build config resource has not been opened.");
		}
	}
	
	
	@Test
	public void testEditBuildConfigAndCheckChangesInExplorer() {
		TextEditor editor = getBuildConfigTextEditor();
		String text = editor.getText();
		if (buildConfig == null) {
			buildConfig = text;
		}
		editor.setText(text.replace(originalRepo, customRepo));
		editor.close(true);
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		assertTrue("Changes from updating of a build config should be shown "
				+ "in Properties view, but it is not.", 
				getBuildConfig().getPropertyValue("Source", "URI").equals(customRepo));
	}

	@Test
	public void testIncorrectResourceContent() {
		TextEditor editor = getBuildConfigTextEditor();
		String text = editor.getText();
		if (buildConfig == null) {
			buildConfig = text;
		}
		text = text.replace(DatastoreOS3.TEST_PROJECT, DatastoreOS3.TEST_PROJECT + "1");
		editor.setText(text);
		try {
			editor.save();
		} catch (WaitTimeoutExpiredException ex) {
			// ok
		}
		
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT, false);
		assertTrue("Editor should be dirty, it should not be able to save incorrect content", editor.isDirty());
		try {
			new DefaultShell("Problem Occurred");
			new OkButton().click();
		} catch (RedDeerException ex) {
			// sometimes it occures, sometimes not
		}
	}
	
	private OpenShiftResource getBuildConfig() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		return explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(DatastoreOS3.TEST_PROJECT).
				getOpenShiftResources(Resource.BUILD_CONFIG).get(0);
	}

	private TextEditor getBuildConfigTextEditor() {
		getBuildConfig().select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.EDIT).select();
		System.out.println("TESTING:" + BUILD_CONFIG_EDITOR);
		
		return new TextEditor(BUILD_CONFIG_EDITOR);
	}
	
	@After
	public void setOriginalBuildConfigContent() {
		if (buildConfig != null) {
			getBuildConfig().select();
			new ContextMenuItem(OpenShiftLabel.ContextMenu.EDIT).select();
			
			TextEditor editor = new TextEditor(BUILD_CONFIG_EDITOR);
			editor.setText(buildConfig);
			editor.close(true);
			
			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		}
	}
	
	@AfterClass
	public static void closeEditor() {
		try {
			new TextEditor(new StringContains(BUILD_CONFIG_EDITOR)).close(false);
		} catch (RedDeerException ex) {
			// do nothing, there is no editor
		}
		tearDown();
	}
	
	public static void tearDown() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		OpenShift3Connection connection  = explorer.getOpenShift3Connection(connectionReq.getConnection());
		connection.getProject(DatastoreOS3.TEST_PROJECT).delete();
		
		try {
			new WaitWhile(new OpenShiftProjectExists(connectionReq.getConnection()));
		} catch (WaitTimeoutExpiredException ex) {
			connection.refresh();
		
			new WaitWhile(new OpenShiftProjectExists(connectionReq.getConnection()), TimePeriod.getCustom(5));
		}
		
		connection.createNewProject();
		
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		if (projectExplorer.containsProject(PROJECT_NAME)) {
			projectExplorer.getProject(PROJECT_NAME).delete(true);
		}
	}
}
