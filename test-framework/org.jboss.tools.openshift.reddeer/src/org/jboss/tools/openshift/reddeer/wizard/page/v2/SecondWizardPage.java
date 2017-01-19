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
package org.jboss.tools.openshift.reddeer.wizard.page.v2;

import java.util.List;

import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.BackButton;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.combo.DefaultCombo;
import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.table.DefaultTable;
import org.jboss.reddeer.swt.impl.text.DefaultText;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.condition.ButtonWithTextIsAvailable;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * Second wizard page of a New application wizard.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class SecondWizardPage {
	
	public SecondWizardPage() {
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		
		// Wait until data are processed - there is no other way currently
		new WaitUntil(new WidgetIsEnabled(new BackButton()), TimePeriod.LONG);
	}
	
	/**
	 * Fill in application details.
	 * @param domain domain name
	 * @param appName application name 
	 * @param scalable set true if application should be scalable, false otherwise
	 * @param smallGear set true if application should use small gear, false otherwise
	 * @param createEnvironmentVariable set true if environment variable should be create, false otherwise
	 * @param URL if git URL is provided application will be created from template
	 */
	public void fillApplicationDetails(String domain, String appName, boolean scalable, 
			boolean smallGear, boolean createEnvironmentVariable, String sourceCodeURL) {
		new DefaultCombo(0).setSelection(domain);

		new LabeledText("Name:").setText(appName);

		if (smallGear == false) {
			List<String> gears = new LabeledCombo("Gear profile:").getItems();
			for (String gear: gears) {
				if (gear.equals("int_general_medium") || gear.equals("medium")) {
					new LabeledCombo("Gear profile:").setSelection(gear);
				}
				
			}
		}
			
		if (scalable) {
			new CheckBox(0).click();
		}
		
		// Set URL of source code or environment variables for application
		if (sourceCodeURL != null || createEnvironmentVariable == true) {
			new PushButton(OpenShiftLabel.Button.ADVANCED_OPEN).click();
		
			if (sourceCodeURL != null) {
				new CheckBox(1).click();
				new LabeledText("Source code:").setText(sourceCodeURL);
			}
			
			if (createEnvironmentVariable) {
				new WaitUntil(new ButtonWithTextIsAvailable(OpenShiftLabel.Button.ENV_VAR),
					TimePeriod.NORMAL);
					
				new PushButton(OpenShiftLabel.Button.ENV_VAR).click();
				
				new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.ENV_VARS),
						TimePeriod.LONG);
				
				new DefaultShell(OpenShiftLabel.Shell.ENV_VARS);
				
				new PushButton(OpenShiftLabel.Button.ADD).click();
				
				new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.EDIT_ENV_VAR),
						TimePeriod.LONG);
				
				new DefaultShell(OpenShiftLabel.Shell.EDIT_ENV_VAR);
				new DefaultText(0).setText("varname");
				new DefaultText(1).setText("varvalue");
				
				new WaitUntil(new WidgetIsEnabled(new OkButton()), TimePeriod.NORMAL);
				
				new OkButton().click();
				
				new DefaultShell(OpenShiftLabel.Shell.ENV_VARS);
				
				new OkButton().click();
				
				new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.ENV_VARS),
						TimePeriod.NORMAL);
				
				new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
			}
		}
	}
		
	/**
	 * Add cartridges to a new application. Beware! Cartridges can show warning dialog 
	 * in case of incompatible/missing cartridges. Use this wisely.
	 * 
	 * @param cartridges cartridge to embed
	 */
	public void addCartridges(String... cartridges) {
		if (cartridges != null) {
			new PushButton(OpenShiftLabel.Button.ADD).click();
			
			new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.ADD_CARTRIDGES),
					TimePeriod.LONG);
			
			new DefaultShell(OpenShiftLabel.Shell.ADD_CARTRIDGES);
			
			for (String cartridge: cartridges) {
				new DefaultTable().getItem(cartridge).select();
				new DefaultTable().getItem(cartridge).setChecked(true);
			}
			
			new WaitUntil(new WidgetIsEnabled(new OkButton()), TimePeriod.NORMAL);
			
			new OkButton().click();
			
			new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		}
	}
	
	/** 
	 * Adds downloadable cartridge to new application. Beware - URL must be valid URL link.
	 * @param URL URL of custom cartridge
	 */
	public void addCodeAnythingCartridge(String URL) {
		if (URL != null) {
			new PushButton(OpenShiftLabel.Button.ADD).click();
			
			new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.ADD_CARTRIDGES),
					TimePeriod.LONG);
			
			new DefaultShell(OpenShiftLabel.Shell.ADD_CARTRIDGES);
			
			new DefaultTable().getItem(OpenShiftLabel.EmbeddableCartridge.DOWNLOADABLE_CARTRIDGE).select();
			new DefaultTable().getItem(OpenShiftLabel.EmbeddableCartridge.DOWNLOADABLE_CARTRIDGE).setChecked(true);
			new DefaultText(0).setText(URL);
			
			new WaitUntil(new WidgetIsEnabled(new OkButton()), TimePeriod.NORMAL);
			
			new OkButton().click();
			
			new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		}
	}
}
