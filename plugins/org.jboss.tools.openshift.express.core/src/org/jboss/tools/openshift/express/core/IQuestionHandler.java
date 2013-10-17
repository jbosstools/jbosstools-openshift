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

/**
 * Handle a question to the user, to be 
 * presented in some UI. 
 * 
 * Since the method of presenting a question to a user is 
 * most likely done via a UI construct, no direct
 * references to the UI may be done in a core plugin. 
 * Instead, the UI plugin should set a proper handler
 * in the {@link OpenshiftCoreUIIntegration} class. 
 */
public interface IQuestionHandler {
	
	/**
	 * A constant representing the commit and push dialog during
	 * publishing when workspace changes have been made. 
	 * 
	 * This question type expects an Object[] return value 
	 * with values as follows:
	 *   1) Boolean representing yes or no
	 *   2) String representing the commit message to be used
	 *   
	 * This question type expects a String[] as data with values as follows:
	 *   1) Title of the dialog
	 *   2) Message in the dialog
	 */
	public static final int COMMIT_AND_PUSH_QUESTION = 100;
	
	
	/**
	 * Open a dialog or question that has multiple pieces of data to return. 
	 * 
	 * @param type an integer representing the specific question type being asked
	 * @param data an array of object for use by the dialog
	 * @return
	 */
	public Object[] openMultiReturnQuestion(int type, Object[] data);
	
	
	/**
	 * Present a simple yes or no question to the user
	 * @param title
	 * @param message
	 * @param defaultAnswer
	 * @return  true if the user responded in the affirmative, false otherwise
	 */
	public boolean openQuestion(final String title, final String message, final boolean defaultAnswer);
	
	
}
