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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.OpenShiftCoreActivator;

import com.openshift.client.IHttpClient.ISSLCertificateCallback;

/**
 * A class to aid in any tasks that may be relevant to a UI, 
 * for example, displaying a question, or any interactions
 * with an eclipse console, or prompting for credentials
 * 
 * @author Rob Stryker
 */
public class OpenshiftCoreUIIntegration {
	
	 private static final String EGITUI_ID = 
		      "org.jboss.tools.openshift.express.core.ui.egit";
	 
	// The singleton instance
	private static OpenshiftCoreUIIntegration instance;

	private IConsoleUtility consoleUtil;
	private IQuestionHandler questionHandler;
	private ICredentialsPrompter credentialPrompter;
	private ISSLCertificateCallback sslCertificateCallback;
	
	// Access the singleton
	public static OpenshiftCoreUIIntegration getDefault() {
		if (instance == null)
			instance = new OpenshiftCoreUIIntegration();
		return instance;
	}
		
	public ICredentialsPrompter getCredentialPrompter() {
		return credentialPrompter;
	}
	
	public void setCredentialPrompter(ICredentialsPrompter prompter) {
		this.credentialPrompter = prompter;
	}
	
	public ISSLCertificateCallback getSSLCertificateCallback() {
		return sslCertificateCallback;
	}
	
	public void setSSLCertificateAuthorization(ISSLCertificateCallback authorization) {
		this.sslCertificateCallback = authorization;
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
	
	public IEGitUI getEGitUI() throws CoreException {
		IEGitUI egitUI = getConfigurationElement(getExtension(EGITUI_ID));
		if (egitUI == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus("Could not find extension " + EGITUI_ID));
		}
		return egitUI;
	}

	private IConfigurationElement[] getExtension(String name) {
		return Platform.getExtensionRegistry().getConfigurationElementsFor(name);
	}

	private <T> T getConfigurationElement(IConfigurationElement[] configurations) throws CoreException {
		T element = null;
		if (configurations != null) {
			for (IConfigurationElement configuration : configurations) {
				element = (T) configuration.createExecutableExtension("class");
			}
		}
		return element;
	}
	
	/**
	 * Show the console view for the given server
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
	
	public static void openCommitDialog(IProject project, String remote, String applicationName, Runnable runnable) throws CoreException {
		getDefault().getEGitUI().commitWithUI(project, remote, applicationName, runnable);
	}
}
