/*******************************************************************************
 * Copyright (c) 2007-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.v3;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.wizard.AbstractOpenShiftApplicationWizard;

/**
 * To create a new OpenShift application there are several steps:
 * - open new OpenShift application wizard
 * - select template either from local file system or server
 * - proceed through wizard and set up missing remaining parameters if there are any
 * 
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 */
public class NewOpenShift3ApplicationWizard extends AbstractOpenShiftApplicationWizard {

	public NewOpenShift3ApplicationWizard(Connection connection) {
		super(connection);
	}
	
	public void openWizardFromExplorer() {
		openWizardFromExplorer(null);
	}

	/**
	 * Opens a new OpenShift application wizard from OpenShift Explorer view with the given project pre-selected.
	 * If the project is null, a generated project is used.
	 */
	public void openWizardFromExplorer(String project) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		explorer.getOpenShift3Connection(this.connection).refresh(TimePeriod.getCustom(180));
		
		selectExplorerProject(project, explorer);
	
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_OS3_APPLICATION).select();
		
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
	}

	private void selectExplorerProject(String project, OpenShiftExplorerView explorer) {
		if (StringUtils.isEmpty(project)) {
			explorer.getOpenShift3Connection(this.connection).getProject().select();
		} else {
			explorer.getOpenShift3Connection(this.connection).getProject(project).select();
		}
	}

	public void openWizardFromShellMenu(String project) {
		super.openWizardFromShellMenu();
		selectComboItem(project, new DefaultCombo(0));
	}
}
