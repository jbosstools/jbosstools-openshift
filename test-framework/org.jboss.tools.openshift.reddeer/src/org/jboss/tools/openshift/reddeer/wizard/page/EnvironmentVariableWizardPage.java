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

import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.button.YesButton;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.table.DefaultTable;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * Wizard page containing table with environment variables.
 *  
 * @author mlabuda@redhat.com
 *
 */
public class EnvironmentVariableWizardPage {
	
	/**
	 * Removes environment.
	 * 
	 * @param envVar environment variable to remove
	 * @return true if variable was removed successfully, false otherwise
	 */
	public boolean removeEnvironmentVariable(EnvVar envVar) {
		DefaultTable table = new DefaultTable();
		table.select(envVar.getName());
		new PushButton(OpenShiftLabel.Button.REMOVE_BASIC).click();
		
		new DefaultShell(OpenShiftLabel.Shell.REMOVE_ENV_VAR);
		new YesButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.REMOVE_ENV_VAR));
		
		return !(table.containsItem(envVar.getName(), 0) && table.containsItem(envVar.getValue(), 1));
	}
	
	/**
	 * Creates a new environment variable.
	 * 
	 * @param envVar environment variable to create
	 * @return true if environment variable was created successfully, false otherwise
	 */
	public boolean addEnvironmentVariable(EnvVar envVar) {
		DefaultTable table = new DefaultTable();
		new PushButton(OpenShiftLabel.Button.ADD).click();
		
		new DefaultShell(OpenShiftLabel.Shell.ENVIRONMENT_VARIABLE);
		
		new LabeledText("Name:").setText(envVar.getName());
		new LabeledText("Value:").setText(envVar.getValue());
		new OkButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.ENVIRONMENT_VARIABLE));
		
		return table.containsItem(envVar.getName(), 0) && table.containsItem(envVar.getValue(), 1);
	}
	
	/**
	 * Changes an existing environment variable to have new values.
	 * New and old environment variables passed as arguments should be different
	 * at least in one attribute (name or value).
	 * 
	 * @param oldVar old environment variable
	 * @param newVar environment variable with new values
	 * @return if environment variable was edited successfully, false otherwise
	 */
	public boolean editEnvironmentVariable(EnvVar oldVar, EnvVar newVar) {
		DefaultTable table = new DefaultTable();
		table.getItem(oldVar.getName()).select();
		new PushButton(OpenShiftLabel.Button.EDIT).click();
		
		new DefaultShell(OpenShiftLabel.Shell.ENVIRONMENT_VARIABLE);
		LabeledText name = new LabeledText(OpenShiftLabel.TextLabels.NAME);
		if (!name.isReadOnly()) {
			name.setText(newVar.getName());
		}
		new LabeledText(OpenShiftLabel.TextLabels.VALUE).setText(newVar.getValue());
		new OkButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.ENVIRONMENT_VARIABLE));
		
		return (table.containsItem(newVar.getName(), 0) && table.containsItem(newVar.getValue(), 1)) &&
				!(table.containsItem(oldVar.getName(), 0) && table.containsItem(oldVar.getValue(), 1)); 
	}
	
	/**
	 * Resets environment variable to its default state.
	 * 
	 * @param changedVar
	 * @param originalVar
	 * @return true if variable was reset successfully, false otherwise 
	 */
	public boolean resetEnvironmentVariable(EnvVar changedVar, EnvVar defaultVar) {
		DefaultTable table = new DefaultTable();
		table.getItem(changedVar.getName()).select();
		
		new PushButton(OpenShiftLabel.Button.RESET).click();
		
		new DefaultShell(OpenShiftLabel.Shell.RESET_ENV_VAR);
		new YesButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.RESET_ENV_VAR));
		
		return table.containsItem(defaultVar.getName(), 0) && table.containsItem(defaultVar.getValue(), 1);
	}
	
	/**
	 * Resets all edited environment variables and check whether default variables passed as argument are 
	 * displayed in the table.
	 *  
	 * @param envVars environment variables to verify that were changed to default state 
	 * @return true if variables were reset successfully, false otherwise
	 */
	public boolean resetAllVariables(EnvVar... envVars) {
		DefaultTable table = new DefaultTable();
		new PushButton(OpenShiftLabel.Button.RESET_ALL).click();
		
		new DefaultShell(OpenShiftLabel.Shell.RESET_ENV_VAR);
		new YesButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.RESET_ENV_VAR));
		
		// If any of environment variables passed as argument were not reset, return false
		if (envVars != null && envVars.length > 0) {
			for (EnvVar envVar: envVars) {
				if (!(table.containsItem(envVar.getName(), 0) && (table.containsItem(envVar.getValue(), 1)))) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Object representing environment variable pair name:value.
	 * 
	 * @author mlabuda@redhat.com
	 *
	 */
	public static class EnvVar {
		
		private String name;
		private String value;
		
		public EnvVar(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
	}
}
