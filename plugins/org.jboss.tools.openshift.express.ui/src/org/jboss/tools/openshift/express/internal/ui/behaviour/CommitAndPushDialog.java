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
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;

public class CommitAndPushDialog extends MessageDialog {

	private static String[] labels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
	
	/*
	 * Show the dialog in the display thread and return the proper data array
	 */
	public static Object[] showCommitAndPushDialog(final String title, final String message) {
		final CommitAndPushDialog[] dialog = new CommitAndPushDialog[1];
		Display.getDefault().syncExec(new Runnable() {
		      public void run() {
		    	  dialog[0] = new CommitAndPushDialog(title, message);
		    	  dialog[0].open();
		      }
		});
		return dialog[0].getReturnDataArray();
	}

	
	
	private String customizeMessageText;
	private int openVal = -1;
	
	
	public CommitAndPushDialog(String title, String message) {
		super(Display.getDefault().getActiveShell(), 
				title, null, message, 
				MessageDialog.QUESTION, labels,0);
	}
	
	@Override
	public int open() {
		int ret = super.open();
		openVal = ret;
		return ret;
	}
	
	private Object[] getReturnDataArray() {
		return new Object[] { 
				new Boolean(openVal == 0), 
				customizeMessageText
		};
	}
	
    protected Control createCustomArea(Composite parent) {
    	Composite c = new Composite(parent, SWT.NONE);
    	c.setLayoutData(new GridData(GridData.FILL_BOTH));
    	c.setLayout(new GridLayout(1,true));
    	Label commitMsgLabel = new Label(c, SWT.CHECK);
    	commitMsgLabel.setText(OpenshiftUIMessages.PublishDialogCustomizeGitCommitMsg);
    	final Text t = new Text(c, SWT.MULTI | SWT.BORDER | SWT.WRAP);
    	t.setText(OpenshiftUIMessages.PublishDialogDefaultGitCommitMsg);
    	t.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				customizeMessageText = t.getText();
			}
		});
    	GridDataFactory.fillDefaults().span(1,3).hint(SWT.DEFAULT, 150).grab(true,  true).applyTo(t);
    	
        return c;
    }
}
