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
package org.jboss.tools.openshift.reddeer.wizard.v3;

import org.apache.commons.lang.StringUtils;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.impl.combo.DefaultCombo;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.wizard.AbstractOpenShiftApplicationWizard;

/**
 * To create a new OpenShift 3 application there are several steps:
 * - open new OpenShift 3 application wizard
 * - select template either from local file system or server
 * - proceed through wizard and set up missing remaining parameters if there are any
 * 
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 */
public class NewOpenShift3ApplicationWizard extends AbstractOpenShiftApplicationWizard {

	public NewOpenShift3ApplicationWizard() {
		super(DatastoreOS3.SERVER, DatastoreOS3.USERNAME);
	}
	
	public void openWizardFromExplorer() {
		openWizardFromExplorer(null);
	}

	/**
	 * Opens a new OpenShift 3 application wizard from OpenShift Explorer view with the given project pre-selected.
	 * If the project is null, a generated project is used.
	 */
	public void openWizardFromExplorer(String project) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();

		selectExplorerProject(project, explorer);
	
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_OS3_APPLICATION).select();
		
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
	}

	private void selectExplorerProject(String project, OpenShiftExplorerView explorer) {
		if (StringUtils.isEmpty(project)) {
			explorer.getOpenShift3Connection().getProject().select();
		} else {
			explorer.getOpenShift3Connection().getProject(project).select();
		}
	}

	public void openWizardFromShellMenu(String project) {
		super.openWizardFromShellMenu();
		selectComboItem(project, new DefaultCombo(0));
	}
}
