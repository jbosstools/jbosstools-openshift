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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.matcher.RegexMatcher;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.ui.browser.BrowserEditor;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.view.resources.Service;
import org.jboss.tools.openshift.reddeer.wizard.importapp.ImportApplicationWizard;
import org.jboss.tools.openshift.reddeer.wizard.server.ServerSettingsWizard;

public class OpenShiftUtils {
	
	public static OpenShiftResource getOpenShiftPod(String projectName, String podName) {
		return getOpenShiftPod(projectName, new IsEqual<String>(podName));
	}
	
	public static OpenShiftResource getOpenShiftPod(String projectName, Matcher<String> matcher) {
		return getOpenShiftPod(projectName, Resource.POD, matcher);
	}
	
	public static OpenShiftResource getOpenShiftPod(String projectName, Resource resource, Matcher<String> matcher) {
		assertTrue(!StringUtils.isBlank(projectName));

		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		List<OpenShiftResource> pods = explorer.getOpenShift3Connection().getProject(projectName)
				.getOpenShiftResources(resource);
		for (OpenShiftResource pod : pods) {
				if (matcher.matches(pod.getName())) {
					return pod;
				}
		}
		fail("Pod with matcher:" + matcher.toString() + " for project " + projectName + " was not found");
		return null;
	}

	public static OpenShiftProject getOpenShiftProject(String projectName) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		OpenShiftProject project = explorer.getOpenShift3Connection().getProject(projectName);
		assertThat("Could not find project " + projectName + " in OpenShift Explorer",  project, notNullValue());
		project.expand();
		return project;
	}
	
	public static void deleteAllProjects() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection().refresh();
		new WaitWhile(new JobIsRunning());
		List<OpenShiftProject> projects = explorer.getOpenShift3Connection().getAllProjects();
		for (OpenShiftProject project : projects) {
			if (project != null) {
				String projectName = project.getName();
				try {
					project.delete();
					new WaitWhile(new OpenShiftProjectExists(projectName), TimePeriod.LONG);
				} catch (CoreLayerException ex) {
					// project does not exist - project has been deleted but view has not been
					// refreshed
					explorer.getOpenShift3Connection().refresh();
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
	
	public static ImportApplicationWizard openImportApplicationWizardFromOpenshiftView(Service openshiftService){
		openshiftService.select();
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
	


}
