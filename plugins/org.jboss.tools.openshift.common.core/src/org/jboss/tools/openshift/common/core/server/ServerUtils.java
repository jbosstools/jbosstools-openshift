/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.common.core.server;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.wst.server.core.IServerAttributes;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Andre Dietisheim
 */
public class ServerUtils {

	public static String getServerName(String baseName) {
		return ServerUtil.getDefaultServerName(baseName);
	}

	/**
	 * Returns the attribute value for the given name and project. The given
	 * default value is return if the value doesnt exist or cannot be retrieved.
	 * 
	 * @param project
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getProjectAttribute(String name, String defaultValue, String nodeQualifier, IProject project) {
		if (!ProjectUtils.isAccessible(project)
				|| StringUtils.isEmpty(nodeQualifier)) {
			return defaultValue;
		}
		IEclipsePreferences node = getProjectNode(nodeQualifier, project);
		if (node == null) {
			return defaultValue;
		}
		return node.get(name, defaultValue);
	}

	public static IEclipsePreferences getProjectNode(String nodeQualifier, IProject project) {
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences node = context.getNode(nodeQualifier);
		return node;
	}

	public static void setProjectAttribute(String name, String value, String nodeQualifier, IProject project) {
		setProjectAttribute(name, value, nodeQualifier, project, false);
	}
	
	public static void setProjectAttribute(String name, String value, String nodeQualifier, IProject project, boolean sync) {
		IEclipsePreferences node = getProjectNode(nodeQualifier, project);
		node.put(name, value);
		if( sync ) {
			try {
				node.flush();
			} catch(BackingStoreException bse) {
				OpenShiftCommonCoreActivator.pluginLog().logError("Error saving project setting", bse);
			}
		}
	}

	
	public static String getServerAttribute(String name, String defaultValue, IServerAttributes attributes) {
		if (attributes == null) {
			return null;
		}
		return attributes.getAttribute(name, defaultValue);
	}
	
	public static IContainer getContainer(String name, IProject project) {
		if (!StringUtils.isEmpty(name)) {
			return (IContainer) project.findMember(new Path(name));
		} else {
			return project;
		}
	}
}
