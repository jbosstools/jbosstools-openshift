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

import java.io.File;

import org.hamcrest.core.StringContains;
import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.workbench.impl.editor.TextEditor;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.wizard.v3.TemplateParameter;
import org.jboss.tools.openshift.reddeer.wizard.v3.TemplatesCreator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OCBinary
@OpenPerspective(value=JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@RequiredBasicConnection
@CleanConnection
@RequiredService(service = "eap-app", template = "resources/eap70-basic-s2i-helloworld.json")
public class EditResourcesTest{
	
	private static String GIT_FOLDER = "jboss-eap-quickstarts";
	private String customRepo = "https://github.com/rhopp/jboss-eap-quickstarts";
	private String originalRepo = "https://github.com/jboss-developer/jboss-eap-quickstarts";
	private static String TEMPLATE_PATH = CreateResourcesTest.RESOURCES_LOCATION +
			File.separator + "eap64-basic-s2i.json";
	private static String PROJECT_NAME = "jboss-helloworld";
	private static String DEFAULT_NEXUS_MIRROR = "http://10.8.175.83:8081/nexus/content/groups/all-in-one/";
	private String buildConfig;
	
	private static final String BUILD_CONFIG_EDITOR = "[" + DatastoreOS3.PROJECT1 + "] Build Config : eap-app.json";
	
	@BeforeClass
	public static void setUp() {
		TestUtils.cleanupGitFolder(GIT_FOLDER);
		TestUtils.setUpOcBinary();
		
		// If project does not exists, e.g. something went south in recreation earlier, create it
		if (!new OpenShiftProjectExists(DatastoreOS3.PROJECT1_DISPLAYED_NAME).test()) {
			new OpenShiftExplorerView().getOpenShift3Connection().createNewProject();
		}
		
		if (getNexusMirror() != null) {
			new TemplatesCreator().createOpenShiftApplicationBasedOnLocalTemplate(
				TEMPLATE_PATH, new TemplateParameter(OpenShiftLabel.Others.MAVEN_MIRROR_URL, getNexusMirror()));
		} else {
			new TemplatesCreator().createOpenShiftApplicationBasedOnLocalTemplate(TEMPLATE_PATH);
		}
	}
	
	
	
	@Test
	public void testCanEditResource() {
		getBuildConfig().select();
		new ContextMenu(OpenShiftLabel.ContextMenu.EDIT).select();
		
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
		
		editor.setText(text.replace("\"namespace\" : \"" + DatastoreOS3.PROJECT1 + "\"",
				"\"namespace\" : \"" + DatastoreOS3.PROJECT1 + "\"wtf"));		
		try {
			editor.save();
		} catch (CoreLayerException ex) {
			// ok
		}
		
		new WaitWhile(new JobIsRunning(), TimePeriod.NORMAL, false);
		assertTrue("Editor should be dirty, it should not be able to save incorrect content", editor.isDirty());
		try {
			new DefaultShell("Problem Occurred");
			new OkButton().click();
		} catch (RedDeerException ex) {
			// sometimes it occures, sometimes not
		}
	}
	
	/**
	 * Gets URL of Nexus Mirror. At first look up if user provided 
	 * nexus mirror URL by property {@link DatastoreOS3.KEY_NEXUS_MIRROR}. If
	 * none provided, try to use default, which is stored in 
	 * {@link AbstractCreateApplicationTest.DEFAULT_NEXUS_MIRROR}. If none of 
	 * the above works, use default, official nexus and this method returns null.
	 */
	private static String getNexusMirror() {
		if (isNexusMirrorProvided()) {
			return DatastoreOS3.NEXUS_MIRROR_URL;
		} else {
			if (isDefaultNexusMirrorWorking()) {
				return DEFAULT_NEXUS_MIRROR;
			} else {
				return null;
			}
		}
	}
	
	private static boolean isNexusMirrorProvided() {
		if (DatastoreOS3.NEXUS_MIRROR_URL == null || DatastoreOS3.NEXUS_MIRROR_URL.equals("")) {
			return false;
		}
		return TestUtils.isURLAccessible(DatastoreOS3.NEXUS_MIRROR_URL);
	}
	
	private static boolean isDefaultNexusMirrorWorking() {
		return TestUtils.isURLAccessible(DEFAULT_NEXUS_MIRROR);
	}
	
	private OpenShiftResource getBuildConfig() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		return explorer.getOpenShift3Connection().getProject().
				getOpenShiftResources(Resource.BUILD_CONFIG).get(0);
	}

	private TextEditor getBuildConfigTextEditor() {
		getBuildConfig().select();
		new ContextMenu(OpenShiftLabel.ContextMenu.EDIT).select();
		
		return new TextEditor(BUILD_CONFIG_EDITOR);
	}
	
	@After
	public void setOriginalBuildConfigContent() {
		if (buildConfig != null) {
			getBuildConfig().select();
			new ContextMenu(OpenShiftLabel.ContextMenu.EDIT).select();
			
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
		
		OpenShift3Connection connection  = explorer.getOpenShift3Connection();
		connection.getProject().delete();
		
		try {
			new WaitWhile(new OpenShiftProjectExists());
		} catch (WaitTimeoutExpiredException ex) {
			connection.refresh();
		
			new WaitWhile(new OpenShiftProjectExists(), TimePeriod.getCustom(5));
		}
		
		connection.createNewProject();
		
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		if (projectExplorer.containsProject(PROJECT_NAME)) {
			projectExplorer.getProject(PROJECT_NAME).delete(true);
		}
	}
}
