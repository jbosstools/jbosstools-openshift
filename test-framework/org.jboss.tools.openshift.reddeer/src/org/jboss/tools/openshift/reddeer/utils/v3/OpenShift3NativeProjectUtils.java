/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils.v3;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.project.IProjectRequest;

/**
 * Utils class for working with OS3 projects using openshift core api (not UI).
 * 
 * @author rhopp
 *
 */

public class OpenShift3NativeProjectUtils {
	
	private OpenShift3NativeProjectUtils() {
		throw new IllegalAccessError("Utilities class");
	}

	private static Logger LOGGER = new Logger(OpenShift3NativeProjectUtils.class);

	public static IProject getOrCreateProject(String name, String displayName, String description, Connection connection) {
		IProject project = OpenShift3NativeResourceUtils.getProject(name, connection);
		if (project == null) {
			LOGGER.debug(NLS.bind("Project {0} doesnt exist yet in {1}, creating it.", name, connection.getHost()));
			project = createProject(name, displayName, description, connection);

		}
		return project;
	}

	public static IProject createProject(String name, String displayName, String description, Connection connection) {
		assertTrue(StringUtils.isNotBlank(name));
		assertNotNull(displayName);
		assertNotNull(description);
		assertNotNull(connection);

		IProjectRequest request = connection.getResourceFactory().stub(ResourceKind.PROJECT_REQUEST, name);
		request.setDisplayName(StringUtils.isEmpty(displayName) ? name : displayName);
		request.setDescription(StringUtils.isEmpty(description) ? name : description);

		IProject createdProject = (IProject) connection.createResource(request);
		
		/**
		 * WORKAROUND: explorer wont get notified of the the new project and
		 * therefore wont display it unless a manual refresh is done on the
		 * connection. https://issues.jboss.org/browse/JBIDE-23513 remove
		 * this wait once WatchManager is watching projects and notifies the
		 * ui.
		 * 
		 * @see WatchManager#KINDS
		 */
		new WaitUntil(new OpenShiftProjectExists(createdProject.getDisplayName(), connection));
		
		return createdProject;
	}

}
