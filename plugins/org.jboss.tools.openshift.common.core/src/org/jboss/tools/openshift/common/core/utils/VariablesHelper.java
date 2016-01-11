/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.utils;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;

/**
 * Utility class to handle variables and variable substitution in Strings.
 * 
 */
public class VariablesHelper {

	public static final String VARIABLE_PREFIX = "${";
	public static final String VARIABLE_SUFFIX = "}";
	public static final String WORKSPACE_LOC = "workspace_loc";
	public static final String WORKSPACE_PREFIX = VARIABLE_PREFIX+WORKSPACE_LOC+":";
	
	private VariablesHelper() {}
	
	/**
	 * @return true if the value contains <code>${</code>
	 */
	public static boolean containsVariables(String value) {
		return StringUtils.isNotBlank(value) && value.indexOf(VARIABLE_PREFIX) < value.indexOf(VARIABLE_SUFFIX);
	}
	
	public static String addWorkspacePrefix(String value) {
		if (StringUtils.isBlank(value) || value.startsWith(WORKSPACE_PREFIX)) {
			return value;
		}
		return VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(WORKSPACE_LOC, value);
	}
	
	public static String getWorkspacePath(String value) {
		if (StringUtils.isBlank(value) || !(value.startsWith(WORKSPACE_PREFIX) && value.endsWith(VARIABLE_SUFFIX))) {
			return value;
		}
		String path = value.substring(WORKSPACE_PREFIX.length(), value.length()-1);
		return path;
	}
	
	public static String replaceVariables(String value) {
		return replaceVariables(value, false);
	}
	
	public static String replaceVariables(String value, boolean ignoreErrors) {
		if (containsVariables(value)) {
			try {
				return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
			} catch (CoreException e) {
				if (ignoreErrors) {
					OpenShiftCommonCoreActivator.log("Could not interpolate "+value, e);
				} else {
					throw new OpenShiftCoreException(e);
				}
			}
		}
		return value;
	}
}
