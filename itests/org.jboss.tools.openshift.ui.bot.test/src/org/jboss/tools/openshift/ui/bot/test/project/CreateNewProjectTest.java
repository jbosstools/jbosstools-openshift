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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection
@RunWith(RedDeerSuite.class)
public class CreateNewProjectTest extends AbstractTest {
	
	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;

	@Test
	public void testCreateNewProjectViaManageShell() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		OpenShift3Connection connection = explorer.getOpenShift3Connection(connectionReq.getConnection());
		connection.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.EDIT_OS_PROJECTS).select();
		
		new DefaultShell(OpenShiftLabel.Shell.MANAGE_OS_PROJECTS);
		new PushButton(OpenShiftLabel.Button.NEW).click();
		
		new DefaultShell(OpenShiftLabel.Shell.CREATE_OS_PROJECT);
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_NAME).setText(DatastoreOS3.PROJECT1);
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_DISPLAYED_NAME).setText(DatastoreOS3.PROJECT1_DISPLAYED_NAME);

		new WaitUntil(new ControlIsEnabled(new FinishButton()));
		
		new FinishButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
	
		new DefaultShell(OpenShiftLabel.Shell.MANAGE_OS_PROJECTS);
		try {
			new DefaultTable().getItem(DatastoreOS3.PROJECT1);
		} catch (RedDeerException ex) {
			fail("Project " + DatastoreOS3.PROJECT1 + " does not exist in the table. It has not been created.");
		}
		assertTrue("Displayed name for project " + DatastoreOS3.PROJECT1 + " is not shown in the table.",
				new DefaultTable().getItem(DatastoreOS3.PROJECT1).getText(1).equals(DatastoreOS3.PROJECT1_DISPLAYED_NAME));
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.MANAGE_OS_PROJECTS), TimePeriod.LONG);

		try {
			connection.getProject();
		} catch (RedDeerException ex) {
			fail("OpenShift project created for a connection has not been shown in OpenShift explorer.\n" +
					ex.getCause());
		}
	}
	
	@Test
	public void testCreateNewProjectViaContextMenu() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		OpenShift3Connection connection = explorer.getOpenShift3Connection(connectionReq.getConnection());
		try {
			connection.createNewProject2();
		} catch (RedDeerException ex) {
			fail("OpenShift project created for a connection has not been shown in OpenShift explorer.\n" + 
					ex.getMessage());
		}
	}
}
