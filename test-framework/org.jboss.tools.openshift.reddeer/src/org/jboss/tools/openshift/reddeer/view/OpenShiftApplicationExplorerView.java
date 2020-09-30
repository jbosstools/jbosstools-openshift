/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.view;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.handler.CTabItemHandler;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.core.lookup.WorkbenchPartLookup;
import org.eclipse.reddeer.workbench.handler.WorkbenchPartHandler;
import org.eclipse.reddeer.workbench.impl.view.WorkbenchView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.reddeer.condition.ODOIsDownloaded;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;

/**
 * 
 * OpenShift Application Explorer view implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class OpenShiftApplicationExplorerView extends WorkbenchView {

	public OpenShiftApplicationExplorerView() {
		super("JBoss Tools", "OpenShift Application Explorer");
	}

	/**
	 * Opens a new connection shell through context menu in OpenShift explorer.
	 */
	public void openConnectionShell() {
		open();
		TreeItem connection = getConnectionItem();
		connection.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.LOGIN).select();
		new DefaultShell(OpenShiftLabel.Shell.LOGIN);
	}

	public void connectToOpenShiftODO() {
		switch (DatastoreOS3.AUTH_METHOD) {
		case BASIC:
			connectToOpenShiftODOBasic(DatastoreOS3.SERVER, DatastoreOS3.USERNAME, DatastoreOS3.PASSWORD);
			break;
		case OAUTH:
			connectToOpenShiftODOOAuth(DatastoreOS3.SERVER, DatastoreOS3.TOKEN);
			break;
		default:
			throw new OpenShiftToolsException("no valid authentication method found. Could not connect.");
		}
	}

	/**
	 * Connects to OpenShift server. OpenShift connection shell has to be opened at
	 * the moment of method invocation.
	 * 
	 * @param server   URL of a server
	 * @param username
	 * @param password
	 */
	public void connectToOpenShiftODOBasic(String server, String username, String password) {
		connectToOpenShift(server, username, password, null);
	}

	/**
	 * Connects to OpenShift server. OpenShift connection shell has to be opened at
	 * the moment of method invocation.
	 * 
	 * @param server URL of a server
	 * @param token
	 */
	public void connectToOpenShiftODOOAuth(String server, String token) {
		connectToOpenShift(server, null, null, token);
	}

	public void connectToOpenShift(String server, String username, String password, String token) {
		openConnectionShell();
		new LabeledText(OpenShiftLabel.TextLabels.URL).setText(server);

		if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password) && !StringUtils.isEmpty(token)) {
			new LabeledText(OpenShiftLabel.TextLabels.TOKEN_ODO).setText(token);
		} else if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password) && StringUtils.isEmpty(token)) {
			new LabeledText(OpenShiftLabel.TextLabels.USERNAME).setText(username);
			new LabeledText(OpenShiftLabel.TextLabels.PASSWORD).setText(password);
		} else {
			throw new OpenShiftToolsException("Wrong or incomplete credentials!");
		}

		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.DEFAULT);

		new FinishButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.LOGIN), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	public boolean connectionExistsAndWorking() {
		try {
			OpenShiftODOConnection connection = getOpenShiftODOConnection();
			connection.expand();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Finds out whether connection with specified username and server exists or
	 * not.
	 * 
	 * @param server server
	 * @return true if connection exists, false otherwise
	 */
	public boolean connectionExists() {
		try {
			return getConnectionItem() != null;
		} catch (RedDeerException ex) {
			return false;
		}
	}

	public OpenShiftODOConnection getOpenShiftODOConnection() {
		OpenShiftODOConnection connection = null;
		connection = new OpenShiftODOConnection(getConnectionItem());
		return connection;
	}

	private TreeItem getConnectionItem() {
		open();
		return new DefaultTree().getItems().get(0);
	}

	@Override
	public void activate() {
		try {
			checkOpen();
		} catch (WaitTimeoutExpiredException ex) {
			//try it once again
			checkOpen();
		}
		log.info("Activate view " + getTitle());
		if (!new ODOIsDownloaded().test()) {
			CTabItemHandler handler = CTabItemHandler.getInstance();
			Display.syncExec(new Runnable() {
				public void run() {
					cTabItem.getSWTWidget().getParent().setSelection(cTabItem.getSWTWidget());
				}
			});
			Event event = handler.createEventForCTabItem(cTabItem.getSWTWidget(), SWT.Selection);
			Display.asyncExec(new Runnable() {
				public void run() {
					cTabItem.getSWTWidget().getParent().notifyListeners(event.type, event);
				}
			});
			WorkbenchPartHandler.getInstance()
			.focusChildControl(WorkbenchPartLookup.getInstance().getActiveWorkbenchPart());
			try {
				DefaultShell shell = new DefaultShell("odo tool required");
				new PushButton(shell, "Yes").click();
				new WaitWhile(new ODOIsDownloaded(), TimePeriod.LONG);
			} catch (CoreLayerException ex) {
				// swallow exception dialog is not found
			}
		} else {
			cTabItem.activate();
			WorkbenchPartHandler.getInstance()
			.focusChildControl(WorkbenchPartLookup.getInstance().getActiveWorkbenchPart());
		}
	}

}
