/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.express.core;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.eclipse.wst.server.core.IServer;

/**
 * A class to aid in any tasks that may be relevant to a UI, 
 * for example, displaying a question, or any interactions
 * with an eclipse console, or prompting for credentials
 */
public class OpenshiftCoreUIIntegration {
	
	// The singleton instance
	private static OpenshiftCoreUIIntegration instance;
	
	// Access the singleton
	public static OpenshiftCoreUIIntegration getDefault() {
		if( instance == null )
			instance = new OpenshiftCoreUIIntegration();
		return instance;
	}
	
	/* An IConsoleUtility for interaction with a console */
	private IConsoleUtility consoleUtil;
	
	/* An IQuestionHandler for presenting questions to a user */
	private IQuestionHandler questionHandler;
	
	/* An ICredentialsPrompter for prompting for credentials */
	private ICredentialsPrompter credentialPrompter = null;
	

	/* Get access to the credential prompter */
	public ICredentialsPrompter getCredentialPrompter() {
		return credentialPrompter;
	}
	
	/* Set the credential prompter */
	public void setCredentialPrompter(ICredentialsPrompter prompter) {
		this.credentialPrompter = prompter;
	}
	
	public void setConsoleUtility(IConsoleUtility util) {
		this.consoleUtil = util;
	}
	
	public IConsoleUtility getConsoleUtility() {
		return consoleUtil;
	}

	public IQuestionHandler getQuestionHandler() {
		return questionHandler;
	}

	public void setQuestionHandler(IQuestionHandler questionHandler) {
		this.questionHandler = questionHandler;
	}

	/*
	 * Force the console view for the given server to show
	 */
	public static void displayConsoleView(final IServer server) {
		IConsoleUtility util = getDefault().getConsoleUtility();
		if( util != null )
			util.displayServerConsoleView(server);
	}

	/**
	 * Return an outputstream which can be used to print 
	 * to the console for the given server. If no such output
	 * stream can be found, return a dummy output stream 
	 * 
	 * @param server
	 * @return
	 */
	public static OutputStream getConsoleOutputStream(IServer server) {
		IConsoleUtility util = getDefault().getConsoleUtility();
		OutputStream os = null;
		if( util != null )
			os = util.getServerConsoleOutputStream(server);
		// There is no console utility, but this method must not return null. 
		// Instead, provide a dummy bytearray output stream. 
		return os == null ? new ByteArrayOutputStream() : os;
	}
	
	/**
	 * Append the given text to the console for the given server. 
	 * This is a convenience method and is functionally equivilent to 
	 * getConsoleOutputStream(server).write(msg.getBytes());
	 * 
	 * @param server
	 * @param msg
	 */
	public static void appendToConsole(IServer server, String msg) {
		IConsoleUtility util = getDefault().getConsoleUtility();
		if( util != null )
			util.appendToServerConsole(server, msg);
	}
	
	
	/**
	 * 
	 * Opens question dialog 
	 * @param message
	 * @param title
	 * 
	 * @return true if the question was approved, false otherwise.
	 */
	public static boolean requestApproval(final String message, final String title) {
		return requestApproval(message, title, true);
	}
	
	/**
	 * 
	 * Opens question dialog where you can control what will be the default
	 * button activated.
	 * @param message
	 * @param title
	 * @parem defaultAnswer if true Yes is the default answer, if false No
	 * 
	 * @return true if the question was approved, defaultAnswer otherwise.
	 */
	public static boolean requestApproval(final String message, final String title, final boolean defaultAnswer) {
		return openQuestion(title, message, defaultAnswer);
	}
	
	/**
	 * Use the IQuestionHandler to prompt the user with a question. 
	 * If no handler is found, return false, otherwise, 
	 * return the value from the handler. 
	 * 
	 * @param title
	 * @param message
	 * @param defaultAnswer
	 * @return
	 */
	private static boolean openQuestion(final String title, final String message, final boolean defaultAnswer) {
		IQuestionHandler handler = getDefault().getQuestionHandler();
		return handler == null ? false : handler.openQuestion(title, message, defaultAnswer);
	}
}
