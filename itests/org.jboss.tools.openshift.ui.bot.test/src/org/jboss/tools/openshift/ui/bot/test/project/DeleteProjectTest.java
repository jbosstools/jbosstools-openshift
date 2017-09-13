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
package org.jboss.tools.openshift.ui.bot.test.project;

import static org.junit.Assert.assertFalse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeProjectUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.Before;
import org.junit.Test;

@RequiredBasicConnection
@CleanConnection
public class DeleteProjectTest {

	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;

	private boolean projectExists;
	private static final String PROJECT_NAME = DatastoreOS3.TEST_PROJECT + "-delete";
	
	@Before
	public void setup(){
		if (!projectExists) {
			OpenShift3NativeProjectUtils.getOrCreateProject(PROJECT_NAME, StringUtils.EMPTY, StringUtils.EMPTY,
					connectionReq.getConnection());
		}
	}

	@Test
	public void testDeleteProjectViaContextMenu() {
		projectExists = true;
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();

		OpenShift3Connection connection = explorer.getOpenShift3Connection();
		connection.getProject(PROJECT_NAME).delete();

		projectExists = false;

		assertFalse("Project is still presented in OpenShift explorer under a connection.",
				connection.projectExists(PROJECT_NAME));
	}

	@Test
	public void testDeleteProjectViaManageProjectsShell() {
		projectExists = true;
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();

		OpenShift3Connection connection = explorer.getOpenShift3Connection();
		connection.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.MANAGE_OS_PROJECTS).select();

		new DefaultShell(OpenShiftLabel.Shell.MANAGE_OS_PROJECTS);
		new DefaultTable().getItem(PROJECT_NAME).select();
		new PushButton(OpenShiftLabel.Button.REMOVE).click();

		new DefaultShell(OpenShiftLabel.Shell.DELETE_RESOURCE);
		new OkButton().click();

		projectExists = false;

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.DELETE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		new WaitUntil(new ControlIsEnabled(new PushButton("Refresh...")), TimePeriod.DEFAULT);
		new PushButton("Refresh...").click();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);

		assertFalse("There should not be present project in the table.",
				new DefaultTable().containsItem(PROJECT_NAME));
		new OkButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.MANAGE_OS_PROJECTS), TimePeriod.LONG);

		assertFalse("Project is still presented in OpenShift explorer under a connection.",
				connection.projectExists(PROJECT_NAME));
	}
}
