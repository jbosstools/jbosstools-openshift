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
package org.jboss.tools.openshift.test.common.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.junit.Test;

public class VariablesHelperTest {

	@Test
	public void testContainsVariables() {
		assertFalse(VariablesHelper.containsVariables(null));
		assertFalse(VariablesHelper.containsVariables("  "));
		assertFalse(VariablesHelper.containsVariables("}foo${"));
		assertFalse(VariablesHelper.containsVariables("}foo${"));
		assertTrue(VariablesHelper.containsVariables("${foo}"));
	}
	
	@Test
	public void testAddWorkspacePrefix() {
		String value = "foo";
		assertEquals(VariablesHelper.WORKSPACE_PREFIX+value+VariablesHelper.VARIABLE_SUFFIX, VariablesHelper.addWorkspacePrefix(value));
		value = null;
		assertNull(VariablesHelper.addWorkspacePrefix(value));
		value = " ";
		assertEquals(value, VariablesHelper.addWorkspacePrefix(value));
		value = VariablesHelper.WORKSPACE_PREFIX+"bar"+VariablesHelper.VARIABLE_SUFFIX;
		assertEquals(value, VariablesHelper.addWorkspacePrefix(value));
	}
	
	@Test
	public void testGetWorkspacePath() {
		String value = "foo";
		assertEquals(value, VariablesHelper.getWorkspacePath(value));
		String varValue = VariablesHelper.addWorkspacePrefix(value);
		assertEquals(value, VariablesHelper.getWorkspacePath(varValue));
	}
	
	
	@Test
	public void testReplaceVariables() throws Exception {
		String name = "foo";
		String value = VariablesHelper.addWorkspacePrefix(name);
		try {
			VariablesHelper.replaceVariables(value);
			fail("missing resource should fail to resolve");
		} catch (OpenShiftCoreException e) {
		}
		assertEquals(value, VariablesHelper.replaceVariables(value, true));
		IProject bar = getOrcreateProject(name);
		assertEquals(bar.getLocation().toOSString(), VariablesHelper.replaceVariables(value));
	}
	
	private IProject getOrcreateProject(String projectName) throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.exists()) {
			return project;
		}
		IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
		project.create(desc, null);
		project.open(null);
		return project;
	}
}
