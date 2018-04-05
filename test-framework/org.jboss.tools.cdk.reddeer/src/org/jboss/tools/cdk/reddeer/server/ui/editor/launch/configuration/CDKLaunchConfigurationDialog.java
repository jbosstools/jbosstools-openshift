/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui.editor.launch.configuration;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.jface.dialogs.TitleAreaDialog;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.Shell;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.ctab.DefaultCTabFolder;
import org.eclipse.reddeer.swt.impl.ctab.DefaultCTabItem;
import org.eclipse.reddeer.swt.impl.group.DefaultGroup;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Class representing CDK server editor's Launch Configuration Dialog
 * @author odockal
 *
 */
public class CDKLaunchConfigurationDialog extends TitleAreaDialog {
	
	private static final Logger logger = Logger.getLogger(CDKLaunchConfigurationDialog.class);
	
	public CDKLaunchConfigurationDialog(Shell shell) {
		super(shell);
	}
	
	public CDKLaunchConfigurationDialog(String text) {
		super(text);
	}
	
	public CDKLaunchConfigurationDialog() {
		this(new DefaultShell(CDKLabel.Shell.LAUNCH_CONFIG_DIALOG));
	}
	
	public void ok() {
		logger.info("Pressing OK button...");
		Button ok = new PushButton(this, CDKLabel.Buttons.OK);
		ok.click();
	}
	
	public void cancel() {
		logger.info("Pressing cancel button...");
		Button cancel = new PushButton(this, CDKLabel.Buttons.CANCEL);
		cancel.click();
	}
	
	public void apply() {
		logger.info("Pressing apply button...");
		Button apply = new PushButton(this, new WithTextMatcher(CDKLabel.Buttons.APPLY));
		if (apply.isEnabled()) {
			apply.click();
		}
	}
	
	public void revert() {
		logger.info("Pressing revert button...");
		Button revert = new PushButton(this, new WithTextMatcher(CDKLabel.Buttons.REVERT));
		if (revert.isEnabled()) {
			revert.click();
		}
	}
	
	public String getName() {
		return new LabeledText(CDKLabel.Labels.NAME).getText();
	}
	
	public DefaultCTabItem getTab(String tab) {
		new DefaultCTabFolder(getShell());
		return new DefaultCTabItem(tab);
	}
	
	public DefaultGroup getGroup(String group) {
		return new DefaultGroup(getShell(), group);
	}
	
	public String getLocation() {
		getTab("Main").activate();
		return new DefaultText(getGroup(CDKLabel.Labels.LOCATION)).getText();
	}
	
	public DefaultText getArguments() {
		getTab("Main").activate();
		return new DefaultText(getGroup(CDKLabel.Labels.ARGUMENTS));
	}
	
	public DefaultTable getEnvVariablesTable() {
		getTab("Environment").activate();
		return new DefaultTable(getShell(), 0);
	}
	
	public String getValueOfEnvVar(String variable) {
		return getEnvVariablesTable().getItem(variable, 0).getText(1);
	}
	
	public void selectEnvVariable(String variable) {
		getEnvVariablesTable().getItem(variable, 0).select();
	}
	
	public void setArguments(String arguments) {
		String actual = getArguments().getText();
		logger.info("Replacing launch config arguments \"" + actual + "\" with \"" + arguments + "\"");
		getArguments().setText(arguments);
	}
	
	public void addArguments(String argumentsToAdd) {
		String actual = getArguments().getText();
		logger.info("Adding \"" + argumentsToAdd + "\" to launch config arguments \"" + actual);
		getArguments().setText(actual + " " + argumentsToAdd);
	}
}
