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
package org.jboss.tools.openshift.express.internal.core;

import java.io.OutputStream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.wst.server.core.IServer;

public class OpenshiftBehaviorUIIntegration {
	private static OpenshiftBehaviorUIIntegration instance;
	public static OpenshiftBehaviorUIIntegration getDefault() {
		if( instance == null )
			instance = new OpenshiftBehaviorUIIntegration();
		return instance;
	}
	
	private IConsoleUtility consoleUtil;
	private IQuestionHandler questionHandler;
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

	public static boolean isUserInitiatedAction(IAdaptable adaptable) {
		return "user".equals(adaptable.getAdapter(String.class)) || adaptable.getAdapter(Shell.class) != null; //$NON-NLS-1$
	}
	
	public static void displayConsoleView(final IServer server) {
		getDefault().getConsoleUtility().displayServerConsoleView(server);
	}

	public static OutputStream getConsoleOutputStream(IServer server) {
		return getDefault().getConsoleUtility().getServerConsoleOutputStream(server);
	}
	
	public static void appendToConsole(IServer server, String msg) {
		getDefault().getConsoleUtility().appendToServerConsole(server, msg);
	}
	
	
	
	public static boolean requestApproval(final String message, final String title) {
		return requestApproval(message, title, true);
	}

	/**
	 * Opens question dialog where you can control what will be the default
	 * button activated.
	 * 
	 * @parem defaultAnswer if true Yes is the default answer, if false No
	 * 
	 **/
	public static boolean requestApproval(final String message, final String title, final boolean defaultAnswer) {
		return openQuestion(title, message, defaultAnswer);
	}
	
	private static boolean openQuestion(final String title, final String message, final boolean defaultAnswer) {
		return getDefault().getQuestionHandler().openQuestion(title, message, defaultAnswer);
	}

}
