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
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.core.IConsoleUtility;
import org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerUtils;

import com.openshift.client.IApplication;

/**
 * A utility class to manager the message consoles creations and retrivals
 * 
 * @author Xavier Coulon
 * 
 */
public class ConsoleUtils implements IConsoleUtility {

	/**
	 * Registers the given listener as a console listener.
	 * 
	 * @param consoleListener
	 */
	public static void registerConsoleListener(IConsoleListener consoleListener) {
		org.jboss.tools.openshift.internal.common.ui.console.ConsoleUtils.registerConsoleListener(consoleListener);
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
		return org.jboss.tools.openshift.internal.common.ui.console.ConsoleUtils.findMessageConsole(name);
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
		org.jboss.tools.openshift.internal.common.ui.console.ConsoleUtils.displayConsoleView(console);
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
