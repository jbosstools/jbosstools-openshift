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
package org.jboss.tools.openshift.reddeer.view.resources;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.editor.ServerEditor;
import org.eclipse.reddeer.jface.handler.TreeViewerHandler;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * OpenShift server adapter which binds local Eclipse project to a deployed
 * application on OpenShift. Server adapters are shown in servers view.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class ServerAdapter{

	private TreeViewerHandler treeViewerHandler = TreeViewerHandler.getInstance();

	private Version version;
	private String applicationName;
	private String resourceKind;

	private TreeItem serverAdapterItem;

	/**
	 * Constructs a new OpenShift server adapter. If there is no such server
	 * adapter, OpenShift tools exception is thrown.
	 * 
	 * @param version
	 *            version of an OpenShift server
	 * @param applicationName
	 *            name of an application that server adapter binds to
	 * @param resourceKind
	 *            kind of resource for which the server adapter was created (for
	 *            example Service or Deployment Config)
	 */
	public ServerAdapter(Version version, String applicationName, String type) {
		this.version = version;
		this.applicationName = applicationName;
		this.resourceKind = type;

		updateServerAdapterTreeItem();
	}

	public ServerAdapter(Version version, String applicationName) {
		this(version, applicationName, StringUtils.EMPTY);
	}

	/**
	 * Selects server adapter.
	 */
	public void select() {
		updateServerAdapterTreeItem();
		serverAdapterItem.select();
	}

	/**
	 * Deletes server adapter safely - if it was not selected, selects it and
	 * then deletes.
	 */
	public void delete() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DELETE).select();

		new DefaultShell(OpenShiftLabel.Shell.DELETE_ADAPTER);
		new OkButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.DELETE_ADAPTER));
		new WaitWhile(new JobIsRunning(), TimePeriod.getCustom(120));
	}

	/**
	 * Gets text (label) of a server adapter shown in Servers view without its
	 * state. Label contains information about application name and OpenShift
	 * server version.
	 * 
	 * @return label of server adapter
	 */
	public String getLabel() {
		String serverAdapterLabel = applicationName;
		if (version.equals(Version.OPENSHIFT2)) {
			serverAdapterLabel += getOS2ServerAdapterAppendix();
		} else {
			if (resourceKind != StringUtils.EMPTY) {
				serverAdapterLabel += String.format(" (%s)", resourceKind);
			}
			serverAdapterLabel += getOS3ServerAdapterAppendix();
		}
		return serverAdapterLabel;
	}

	/**
	 * Opens overview of a server adapter which is usually opened by double
	 * click on a server adapter.
	 */
	public void openOverview() {
		select();
		serverAdapterItem.doubleClick();

		new ServerEditor(getLabel()).activate();
	}

	/**
	 * Closes overview of a server adapter.
	 */
	public void closeOverview() {
		new ServerEditor(getLabel()).close();
	}

	/**
	 * Gets tree item of a server adapter.
	 * 
	 * @return Tree Item of a server adapter.
	 */
	public TreeItem getTreeItem() {
		return serverAdapterItem;
	}

	/**
	 * Useful to update tree item in case of closed Servers view, or if tree
	 * item is rendered and the old one is out of date and for successful
	 * actions it is necessary to update it.
	 */
	private void updateServerAdapterTreeItem() {
		activateView();
		try {
			serverAdapterItem = treeViewerHandler.getTreeItem(new DefaultTree(), getLabel());
		} catch (RedDeerException ex) {
			throw new OpenShiftToolsException("There is no such server adapter");
		}
	}

	/**
	 * Useful if focus on a server view was lost or if view was closed.
	 */
	private void activateView() {
		new ServersView2().open();
	}

	private static String getOS2ServerAdapterAppendix() {
		return " at OpenShift 2";
	}

	private static String getOS3ServerAdapterAppendix() {
		return " at OpenShift 3 (" + DatastoreOS3.SERVER.substring(8).split(":")[0] + ")";
	}

	public enum Version {
		OPENSHIFT2, OPENSHIFT3;
	}
}
