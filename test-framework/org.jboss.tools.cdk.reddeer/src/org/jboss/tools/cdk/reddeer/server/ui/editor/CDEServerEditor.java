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
package org.jboss.tools.cdk.reddeer.server.ui.editor;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.wst.server.ui.editor.ServerEditor;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.hyperlink.DefaultHyperlink;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;
import org.eclipse.reddeer.workbench.condition.EditorIsDirty;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;

/**
 * Class representing  general CDK Server Editor
 * @author odockal
 *
 */
public class CDEServerEditor extends ServerEditor {
	
	private DefaultSection generalSection;
	
	private DefaultSection credentialsSection;
	
	private DefaultSection cdkSection;
	
	public static final String CREDENTIALS = "Credentials";
	
	public static final String CDK_DETAILS = "CDK Details";
	
	public static final String GENERAL = "General Information";
	
	public CDEServerEditor(String title) {
		super(title);
		this.generalSection = new DefaultSection(GENERAL);
		this.credentialsSection = new DefaultSection(CREDENTIALS);
		this.cdkSection = new DefaultSection(CDK_DETAILS);
	}
	
	public void openLaunchConfigurationFromLink() {
		log.info("Activate launch configuration via link");
		getLaunchConfigurationHyperLink().activate();
		ShellIsAvailable launch = new ShellIsAvailable("Edit Configuration");
		new WaitUntil(launch, TimePeriod.MEDIUM);	
	}
	
	@Override
	public void save() {
		activate();
		log.info("Trying to save editor via File -> Save");
		try {
			new ShellMenuItem(new WorkbenchShell(), "File", "Save").select();
			new WaitWhile(new EditorIsDirty(this));
		} catch (CoreLayerException coreExc) {
			if (coreExc.getMessage().equalsIgnoreCase("Menu item is not enabled")) {
				log.debug("Could not perform File -> Save because option was not enabled");
			} else {
				throw coreExc;
			}
		} finally {
			activate();
		}
	}

	public LabeledText getPasswordLabel() {
		return new LabeledText(credentialsSection, "Password: ");
	}
	
	public LabeledText getUsernameLabel() {
		return new LabeledText(credentialsSection, "Username: ");
	}
	
	public Button getAddButton() {
		return new PushButton(credentialsSection, "Add...");
	}
	
	public Button getEditButton() {
		return new PushButton(credentialsSection, "Edit...");
	}
	
	public Combo getDomainCombo() {
		return new LabeledCombo(credentialsSection, "Domain: ");
	}
	
	public Button getPassCredentialsCheckBox() {
		return new CheckBox(credentialsSection, "Pass credentials to environment");
	}
	
	public LabeledText getHostnameLabel() {
		return new LabeledText(generalSection,"Host name:");
	}
	
	public LabeledText getServernameLabel() {
		return new LabeledText(generalSection,"Server name:");
	}
	
	public DefaultHyperlink getLaunchConfigurationHyperLink() {
		return new DefaultHyperlink(generalSection, "Open launch configuration");
	}
	
	public DefaultSection getCDKSection() {
		return this.cdkSection;
	}
	
}
