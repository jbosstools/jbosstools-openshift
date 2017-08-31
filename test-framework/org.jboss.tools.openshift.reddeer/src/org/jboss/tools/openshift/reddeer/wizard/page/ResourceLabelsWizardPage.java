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
package org.jboss.tools.openshift.reddeer.wizard.page;

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * Wizard page for resource labels.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class ResourceLabelsWizardPage {
	
	/**
	 * Creates a new resource label with specified name and value.
	 * 
	 * @param name name of label
	 * @param value value of label
	 * @return true if label was created successfully, false otherwise
	 */
	public boolean createLabel(String name, String value) {
		new PushButton(OpenShiftLabel.Button.ADD).click();
		
		new DefaultShell(OpenShiftLabel.Shell.RESOURCE_LABEL);
		new LabeledText(OpenShiftLabel.TextLabels.LABEL).setText(name);
		new LabeledText(OpenShiftLabel.TextLabels.VALUE).setText(value);
		
		new WaitUntil(new ControlIsEnabled(new OkButton()));
		
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.RESOURCE_LABEL));
		
		return new DefaultTable().containsItem(name, 0) &&
				new DefaultTable().containsItem(value, 1);
	}
	
	/**
	 * Changes resource label to have a new name and value.
	 * @param name current name of resource label
	 * @param newName new name of resource label
	 * @param newValue new value of resource label
	 * @return true if resource label was successfully modified, false otherwise
	 */
	public boolean editLabel(String name, String newName, String newValue) {
		new DefaultTable().select(name);
		new PushButton(OpenShiftLabel.Button.EDIT).click();
		
		new DefaultShell(OpenShiftLabel.Shell.RESOURCE_LABEL);
		new LabeledText(OpenShiftLabel.TextLabels.LABEL).setText(newName);
		new LabeledText(OpenShiftLabel.TextLabels.VALUE).setText(newValue);
		
		new WaitUntil(new ControlIsEnabled(new OkButton()));
		
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.RESOURCE_LABEL));
		
		return new DefaultTable().containsItem(newName, 0) &&
				new DefaultTable().containsItem(newValue, 1);
	}
	
	/**
	 * Removes resource label
	 * @param name name of label to remove
	 * @return true if label has been removed successfully, false otherwise
	 */
	public boolean deleteLabel(String name) {
		new DefaultTable().select(name);
		new PushButton(OpenShiftLabel.Button.REMOVE).click();
		
		new DefaultShell(OpenShiftLabel.Shell.REMOVE_LABEL);
		new YesButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.REMOVE_LABEL));
		
		return new DefaultTable().containsItem(name);
	}
	
}
