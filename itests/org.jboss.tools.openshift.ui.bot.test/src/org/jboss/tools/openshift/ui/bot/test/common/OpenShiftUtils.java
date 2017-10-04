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
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;

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
		List<OpenShiftProject> projects = explorer.getOpenShift3Connection().getAllProjects();
		for (OpenShiftProject project : projects) {
			if (project != null) {
				String projectName = project.getName();
				project.delete();
				new WaitWhile(new OpenShiftProjectExists(projectName), TimePeriod.LONG);
			}
		}
	}
	


}
