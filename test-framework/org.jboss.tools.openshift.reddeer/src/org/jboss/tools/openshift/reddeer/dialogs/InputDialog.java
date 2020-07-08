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
package org.jboss.tools.openshift.reddeer.dialogs;

import org.eclipse.reddeer.swt.api.Text;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.jboss.tools.common.reddeer.label.IDELabel;

/**
 * {@link org.eclipse.jface.dialogs.InputDialog}
 *
 * @author Radoslav Rabara
 *
 */
public class InputDialog extends DefaultShell {

	/**
	 * Represents the InputDialog.
	 */
	public InputDialog() {
		super();
	}

	/**
	 * Represents InputDialog with the specified <var>title</var>.
	 *
	 * @param title InputDialog title
	 */
	public InputDialog(String title) {
		super(title);
	}

	/**
	 * Click on the OK button.
	 */
	public void ok() {
		new PushButton(IDELabel.Button.OK).click();
	}

	/**
	 * Click on the cancel button.
	 */
	public void cancel() {
		new PushButton(IDELabel.Button.CANCEL).click();
	}

	/**
	 * Returns text from input text field.
	 *
	 * @return input text
	 */
	public String getInputText() {
		return inputText().getText();
	}

	/**
	 * Sets the specified <var>text</var> into input text field.
	 *
	 * @param text text to be set
	 */
	public void setInputText(String text) {
		inputText().setText(text);
	}

	private Text inputText() {
		return new DefaultText();
	}
}