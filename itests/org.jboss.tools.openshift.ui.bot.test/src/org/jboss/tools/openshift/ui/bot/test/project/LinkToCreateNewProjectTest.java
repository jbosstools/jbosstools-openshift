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
import org.eclipse.reddeer.core.handler.TreeItemHandler;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection
@CleanConnection
@RunWith(RedDeerSuite.class)
public class LinkToCreateNewProjectTest extends AbstractTest {
	
	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;
	
	private String projectName = "os3-integration-test";
	private boolean projectCreated = false;
	
	@Test
	public void createOpenShiftProjectViaLinkInExplorer() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		OpenShiftUtils.deleteAllProjects(connectionReq.getConnection());
		TreeItem connectionItem = explorer.getOpenShift3Connection(connectionReq.getConnection()).getTreeItem();
		
		TreeItem newProjectLinkItem = null;
		try {
			newProjectLinkItem = connectionItem.getItem("No projects are available. Click here to create a new project...");
		} catch (RedDeerException ex) {
			fail("There is no link to create a new project even connection does not have any project.");
		}
		
		TreeItemHandler.getInstance().click(newProjectLinkItem.getSWTWidget());
		
		try {
			new DefaultShell(OpenShiftLabel.Shell.CREATE_OS_PROJECT);
		} catch (RedDeerException ex) {
			fail("Create new OpenShift project shell has not been opened.");
		}
		
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_NAME).setText(
				projectName);

		new WaitUntil(new ControlIsEnabled(new FinishButton()));
		
		new FinishButton().click();
		projectCreated = true;
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		assertTrue("OpenShift project is not visible in OpenShift Explorer under the connection"
				+ " although it should have been created successfully and visible.",
				explorer.getOpenShift3Connection(connectionReq.getConnection()).projectExists(projectName));
	}
	
	@After
	public void deleteTestProject() {
		if (projectCreated) {
			new OpenShiftExplorerView().getOpenShift3Connection(connectionReq.getConnection()).getProject(projectName).delete();
		}
	}
}
