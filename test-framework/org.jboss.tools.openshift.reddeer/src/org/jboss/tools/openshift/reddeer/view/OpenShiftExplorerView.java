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
package org.jboss.tools.openshift.reddeer.view;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.commons.lang.StringUtils;
import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.jface.viewer.handler.TreeViewerHandler;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.link.DefaultLink;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.reddeer.workbench.impl.view.WorkbenchView;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;

/**
 * 
 * OpenShift explorer view implemented with RedDeer.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class OpenShiftExplorerView extends WorkbenchView {

	private TreeViewerHandler treeViewerHandler = TreeViewerHandler.getInstance();

	public OpenShiftExplorerView() {
		super("JBoss Tools", "OpenShift Explorer");
	}

	/**
	 * Opens a new connection shell through tool item located in the top right corner of
	 * OpenShift Explorer.
	 */
	public void openConnectionShellViaToolItem() {
		open();
		DefaultToolItem connectionButton = new DefaultToolItem(OpenShiftLabel.Others.CONNECT_TOOL_ITEM);
		connectionButton.click();
	}

	/**
	 * Override open method, because of https://issues.jboss.org/browse/JBIDE-20014.
	 */
	public void reopen() {
		if (isOpened()) {
			close();
		}
		super.open();
	}

	/**
	 * Opens a new connection shell through context menu in OpenShift explorer.
	 */
	public void openConnectionShell() {
		open();
		// there is either a link or context menu
		try {
			new ContextMenu(OpenShiftLabel.ContextMenu.NEW_CONNECTION).select();
		} catch (CoreLayerException ex) {
			new DefaultLink(OpenShiftLabel.TextLabels.CREATE_CONNECTION).click();
		}
	}

	public void connectToOpenShift3() {
		switch (DatastoreOS3.AUTH_METHOD) {
		case BASIC:
			connectToOpenShift3Basic(DatastoreOS3.SERVER, DatastoreOS3.USERNAME, DatastoreOS3.PASSWORD, false, false);
			break;
		case OAUTH:
			connectToOpenShift3OAuth(DatastoreOS3.SERVER, DatastoreOS3.TOKEN, false, false);
			break;
		default:
			throw new OpenShiftToolsException("no valid authentication method found. Could not connect.");
		}
	}

	/**
	 * Connects to OpenShift server. OpenShift connection shell has to be opened at the 
	 * moment of method invocation.
	 * @param server URL of a server
	 * @param username
	 * @param password
	 * @param storePassword whether password should be stored or not in security storage
	 * @param useDefaultServer
	 */
	public void connectToOpenShift3Basic(String server, String username, String password, boolean storePassword, boolean useDefaultServer) {
		connectToOpenShift(server, username, password, storePassword, useDefaultServer, OpenShiftExplorerView.AuthenticationMethod.BASIC,
				false);
	}

	/**
	 * Connects to OpenShift server. OpenShift connection shell has to be opened at the 
	 * moment of method invocation.
	 * @param server URL of a server
	 * @param token
	 * @param storeToken whether password should be stored or not in security storage
	 * @param useDefaultServer
	 */
	public void connectToOpenShift3OAuth(String server, String token, boolean storeToken, boolean useDefaultServer) {
		connectToOpenShift(server, null, token, storeToken, useDefaultServer, OpenShiftExplorerView.AuthenticationMethod.OAUTH, true);
	}

	public void connectToOpenShift(String server, String username, String password, boolean storePassword, boolean useDefaultServer,
			OpenShiftExplorerView.AuthenticationMethod authMethod, boolean certificateShown) {
		new DefaultShell(OpenShiftLabel.Shell.NEW_CONNECTION);

		if (new CheckBox(0).isChecked() != useDefaultServer) {
			new CheckBox(0).click();
		}

		if (!useDefaultServer) {
			new LabeledCombo(OpenShiftLabel.TextLabels.SERVER).setText(server);
		}

		new LabeledCombo(OpenShiftLabel.TextLabels.PROTOCOL).setSelection(authMethod.toString());

		if (OpenShiftExplorerView.AuthenticationMethod.OAUTH.equals(authMethod)) {
			new LabeledText(OpenShiftLabel.TextLabels.TOKEN).setText(password);
		}

		if (OpenShiftExplorerView.AuthenticationMethod.BASIC.equals(authMethod)) {
			new LabeledText(OpenShiftLabel.TextLabels.USERNAME).setText(username);
			new LabeledText(OpenShiftLabel.TextLabels.PASSWORD).setText(password);
			if (new CheckBox(OpenShiftLabel.TextLabels.STORE_PASSWORD).isChecked() != storePassword) {
				new CheckBox(OpenShiftLabel.TextLabels.STORE_PASSWORD).click();
			}
		}

		new WaitUntil(new WidgetIsEnabled(new FinishButton()), TimePeriod.NORMAL);

		new FinishButton().click();

		if (certificateShown) {
			try {
				new DefaultShell("Untrusted SSL Certificate");
				new PushButton("Yes").click();
			} catch (RedDeerException ex) {
				fail("Aceptance of SSL certificate failed.");
			}
		}

		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_CONNECTION), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	/**
	 * Finds out whether connection with specified username exists or not.
	 * 
	 * @param username user name
	 * @return true if connection exists, false otherwise
	 */
	public boolean connectionExists(String username) {
		return connectionExists(null, username);
	}

	/**
	 * Finds out whether connection with specified username and server exists or not.
	 * @param username user name
	 * @param server server
	 * @return true if connection exists, false otherwise
	 */
	public boolean connectionExists(String server, String username) {
		try {
			getConnectionItem(server, username);
			return true;
		} catch (RedDeerException ex) {
			return false;
		}
	}

	public OpenShift3Connection getOpenShiftConnection(String username, String server) {
		OpenShift3Connection connection = null;

		connection = getOpenShift3Connection(server, username);

		assertNotNull("Unable to get openshift 3 connection.", connection);
		return connection;
	}

	/**
	 * Gets default OpenShift 3 connection, which has specified server and user name in {@link DatastoreOS3}
	 * through system properties openshift.server and openshift.username.
	 * 
	 * @return OpenShift 3 connection
	 */
	public OpenShift3Connection getOpenShift3Connection() {
		return getOpenShift3Connection(DatastoreOS3.SERVER, DatastoreOS3.USERNAME);
	}

	public OpenShift3Connection getOpenShift3Connection(Connection connection) {
		return new OpenShift3Connection(getConnectionItem(connection.getHost(), connection.getUsername()));
	}

	/**
	 * Returns the OpenShift 3 connection, which has the given server and user name. 
	 * Throws {@link OpenShiftToolsException} if it doesn't exist.
	 * 
	 * @return OpenShift 3 connection
	 */
	public OpenShift3Connection getOpenShift3Connection(String server, String username) {
		return new OpenShift3Connection(getConnectionItem(server, username));
	}

	public boolean hasOpenShift3Connection() {
		return getConnectionItem(DatastoreOS3.SERVER, DatastoreOS3.USERNAME) != null;
	}

	private TreeItem getConnectionItem(String server, String username) {
		open();
		TreeItem connectionItem = treeViewerHandler.getTreeItem(new DefaultTree(), username);
		if (server != null) {
			if (treeViewerHandler.getStyledTexts(connectionItem)[0].equals(server)) {
				return connectionItem;
			} else {
				throw new OpenShiftToolsException("There is no connection with specified server " + server + " and username " + username);
			}
		} else {
			return connectionItem;
		}
	}

	public enum AuthenticationMethod {

		DEFAULT(""), BASIC("Basic"), OAUTH("OAuth");

		private final String text;

		private AuthenticationMethod(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}

		public static AuthenticationMethod valueOfIgnoreCase(String value) {
			return valueOf(StringUtils.upperCase(value));
		}

		public static AuthenticationMethod safeValueOf(String value) {
			try {
				return valueOfIgnoreCase(value);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

	}
}
