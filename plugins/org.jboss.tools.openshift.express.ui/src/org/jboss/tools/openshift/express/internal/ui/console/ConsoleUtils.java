/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.console;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.tools.openshift.express.core.IConsoleUtility;
import org.jboss.tools.openshift.express.internal.core.behaviour.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IApplication;

/**
 * A utility class to manager the message consoles creations and retrivals
 * 
 * @author Xavier Coulon
 * 
 */
public class ConsoleUtils implements IConsoleUtility {

	/**
	 * Constant key set into the created message console attributes to mark the
	 * given console as an 'openshift' one.
	 */
	public static final String CONSOLE_TYPE_KEY = "ConsoleType";

	/**
	 * Constant value set into the created message console attributes to mark
	 * the given console as an 'openshift' one.
	 */
	public static final String CONSOLE_TYPE_VALUE = "OpenShiftTailConsole";

	/**
	 * Registers the given listener as a console listener.
	 * 
	 * @param consoleListener
	 */
	public static void registerConsoleListener(IConsoleListener consoleListener) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		consoleManager.addConsoleListener(consoleListener);
	}

	/**
	 * Retrieve the message console given its name. If no console exists yet, a
	 * new one is created with a specifi attribute to mark it as an 'openshift'
	 * console. This attribute (or marker) is use later on by the
	 * ConsoleTypePropertyTester to add a 'remove' button on the console in the
	 * consoles view.
	 * 
	 * @param name
	 *            the name of the console to find
	 * @return the message console (found or created)
	 */
	public static MessageConsole findMessageConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		IConsole[] existing = consoleManager.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole console = new MessageConsole(name, null);
		console.setAttribute(CONSOLE_TYPE_KEY, CONSOLE_TYPE_VALUE);
		consoleManager.addConsoles(new IConsole[] { console });
		return console;
	}

	public static void displayConsoleView(IServer server) {
		if (server == null) {
			return;
		}
		MessageConsole console = findMessageConsole(server.getId());
		if (console == null) {
			return;
		}
		displayConsoleView(console);
	}

	public static MessageConsole displayConsoleView(IApplication application) {
		if (application == null) {
			return null;
		}
		MessageConsole console = findMessageConsole(getMessageConsoleName(application));
		if (console == null) {
			return null;
		}
		console.clearConsole();
		displayConsoleView(console);
		return console;
	}
	
	private static String getMessageConsoleName(final IApplication application) {
		return MessageFormat.format(
				"Snapshot Restore/Deploy for application {0} ({1}):",
				application.getName(), application.getDomain().getId());
	}
	/**
	 * Displays the given console in the consoles view which becomes visible if
	 * it was not the case before.
	 * 
	 * @param console the console to display
	 */
	public static void displayConsoleView(final IConsole console) {
		UIUtils.ensureDisplayExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchPart part = null;
				try {
					part = UIUtil.bringViewToFront(IConsoleConstants.ID_CONSOLE_VIEW);
					if (part == null) {
						Logger.warn("Could not open console, " + IConsoleConstants.ID_CONSOLE_VIEW + " was not found");
						return;
					}
					final IConsoleView view = (IConsoleView) part.getAdapter(IConsoleView.class);
					if (view == null) {
						return;
					}
					view.display(console);
				} catch (PartInitException e) {
					Logger.warn("Could not open console view", e);
				}

				
			}
			
		});
	}
	
	public static OutputStream getConsoleOutputStream(IServer server) {
		MessageConsole console = ConsoleUtils.findMessageConsole(server.getId());
		if (console == null) {
			return null;
		}
		return console.newMessageStream();
	}

	public static void appendGitPushToConsole(IServer server, PushOperationResult result) {
		appendToConsole(server, getPushResultAsString(result));
	}
	
	private static final String EMPTY_STRING = ""; //$NON-NLS-1
	private static final String SPACE = " "; //$NON-NLS-1$
	private static final String NL = "\n";//$NON-NLS-1$
	
	private static String getPushResultAsString(PushOperationResult pushOperationResult) {
		StringBuilder result = new StringBuilder(EMPTY_STRING);
		result.append(getPushErrors(pushOperationResult));
		result.append(NL);
		Set<URIish> uris = pushOperationResult.getURIs();
		Iterator<URIish> i = uris.iterator();
		while(i.hasNext()) {
			URIish uri = i.next();
			if (pushOperationResult.getPushResult(uri) == null) {
				continue;
			}
			for (RemoteRefUpdate update : pushOperationResult.getPushResult(uri).getRemoteUpdates()) {
				result.append(getOneResultAsString(pushOperationResult, uri, update));
			}
		}
		return result.toString();
	}
	
	private static String getOneResultAsString(PushOperationResult pushOperationResult, 
			URIish uri, RemoteRefUpdate update) {
		StringBuilder result = new StringBuilder(EMPTY_STRING);
		result.append("Repository");
		result.append(SPACE);
		result.append(uri.toString());
		result.append(Text.DELIMITER);
		result.append(Text.DELIMITER);
		String message = update.getMessage();
		if (message != null)
			result.append(message).append(Text.DELIMITER);
		StringBuilder messagesBuffer = new StringBuilder(pushOperationResult
				.getPushResult(uri).getMessages());
		trim(messagesBuffer);
		if (messagesBuffer.length() > 0)
			result.append(messagesBuffer).append(Text.DELIMITER);
		trim(result);
		return result.toString();
	}

	private static void trim(StringBuilder s) {
		// remove leading line breaks
		while (s.length() > 0 && (s.charAt(0) == '\n' || s.charAt(0) == '\r'))
			s.deleteCharAt(0);
		// remove trailing line breaks
		while (s.length() > 0
				&& (s.charAt(s.length() - 1) == '\n' || s
						.charAt(s.length() - 1) == '\r'))
			s.deleteCharAt(s.length() - 1);
	}

	private static String getPushErrors(PushOperationResult result) {
		StringBuilder messages = new StringBuilder();
		for (URIish uri : result.getURIs()) {
			String errorMessage = result.getErrorMessage(uri);
			if (errorMessage != null && errorMessage.length() > 0) {
				if (messages.length() > 0)
					messages.append(System.getProperty("line.separator")); //$NON-NLS-1$
				messages.append(errorMessage);
			}
		}
		return messages.toString();
	}

	
	public static void appendToConsole(IServer server, String message ) {
		if (OpenShiftServerUtils.isOpenShiftRuntime(server)) {
			final MessageConsole console = ConsoleUtils.findMessageConsole(server.getId());
			MessageConsoleStream newMessageStream = console.newMessageStream();
			newMessageStream.print(message);
			try {
				newMessageStream.close();
			} catch (IOException e) {
				// ignore 
			}

			displayConsoleView(console);
		}
	}

	@Override
	public void displayServerConsoleView(IServer server) {
		displayConsoleView(server);
	}

	@Override
	public OutputStream getServerConsoleOutputStream(IServer server) {
		return getConsoleOutputStream(server);
	}

	@Override
	public void appendToServerConsole(IServer server, String msg) {
		appendToConsole(server, msg);
	}
}
