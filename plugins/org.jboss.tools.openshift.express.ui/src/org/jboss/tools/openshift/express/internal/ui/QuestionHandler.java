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
package org.jboss.tools.openshift.express.internal.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.core.IQuestionHandler;

/**
 * @author Rob Stryker
 */
public class QuestionHandler implements IQuestionHandler {

	@Override
	public boolean openQuestion(final String title, final String message, final boolean defaultAnswer) {
		final boolean[] answer = new boolean[1];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				
				Shell shell = Display.getCurrent().getActiveShell();
				String[] labels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
				int defaultValue = defaultAnswer ? 0 : 1;
				MessageDialog dialog = 
						new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, labels, defaultValue);
				answer[0] = dialog.open() == 0;
			}
		});
		return answer[0];
	}
}
