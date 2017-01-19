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
package org.jboss.tools.openshift.reddeer.utils.v2;

import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.condition.v2.OpenShiftApplicationExists;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift2Application;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;

/**
 * Delete an OpenShift 2 application, server adapter and/or project. 
 * 
 * @author mlabuda@redhat.com
 */
public class DeleteUtils {

	private Logger logger = new Logger(DeleteUtils.class);
	
	private String username;
	private String server;
	private String domain;
	private String appName;
	private String project;
	
	/**
	 * Prepares application to be deleted. Project name is considered to be same
	 * as the application name. If names are different use constructor with project name.
	 * 
	 * @param username user name
	 * @param server server
	 * @param domain domain name
	 * @param appName application Name
	 * @param project project name
	 */
	public DeleteUtils(String username, String server, String domain, String appName, String project) {
		this.username = username;
		this.server = server;
		this.domain = domain;
		this.appName = appName;
		this.project = project;
	}
	
	/**
	 * Deletes project, server adapter and deployed OpenShift application.
	 */
	public void perform() {
		deleteOpenShiftApplication();
		deleteProject();
		deleteServerAdapter();
	}

	public void deleteServerAdapter() {
		new ServerAdapter(Version.OPENSHIFT2, appName).delete();
		logger.info("OpenShift Server Adapter for application " + appName + " has been removed.");
	}
	
	public void deleteOpenShiftApplication() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		
		OpenShift2Application openShiftApplication =
				explorer.getOpenShift2Connection(username).getDomain(domain).getApplication(appName);
		openShiftApplication.select();
		new ContextMenu(OpenShiftLabel.ContextMenu.DELETE_APPLICATION).select();
		
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.DELETE_APP), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_APP);
		new OkButton().click();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		
		explorer.getOpenShift2Connection(username).getDomain(domain).refresh();
		
		new WaitWhile(new OpenShiftApplicationExists(username, server, domain, appName), TimePeriod.LONG);
		logger.info("OpenShift Application " + appName + " has been removed."); 
	}
	
	public void deleteProject() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		
		projectExplorer.getProject(project).delete(true);
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		logger.info("Project " + project + " has been removed.");
	}
}
