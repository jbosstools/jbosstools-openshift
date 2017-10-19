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

import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.wst.server.ui.editor.ServerEditor;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;
import org.eclipse.reddeer.workbench.condition.EditorIsDirty;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;

/**
 * Class representing  general CDK Server Editor
 * @author odockal
 *
 */
public class CDEServerEditor extends ServerEditor {
	
	public static final String CREDENTIALS = "Credentials";
	
	public static final String CDK_DETAILS = "CDK Details";
	
	public CDEServerEditor(String title) {
		super(title);
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
		return new LabeledText(new DefaultSection(CREDENTIALS), "Password: ");
	}
	
	public LabeledText getUsernameLabel() {
		return new LabeledText(new DefaultSection(CREDENTIALS), "Username: ");
	}
	
	public Button getAddButton() {
		return new PushButton(new DefaultSection(CREDENTIALS), "Add...");
	}
	
	public Button getEditButton() {
		return new PushButton(new DefaultSection(CREDENTIALS), "Edit...");
	}
	
	public Combo getDomainCombo() {
		return new LabeledCombo(new DefaultSection(CREDENTIALS), "Domain: ");
	}
	
	public Button getPassCredentialsCheckBox() {
		return new CheckBox(new DefaultSection(CREDENTIALS), "Pass credentials to environment");
	}
	
	public LabeledText getHostnameLabel() {
		return new LabeledText(new DefaultSection("General Information"),"Host name:");
	}
	
	public LabeledText getServernameLabel() {
		return new LabeledText(new DefaultSection("General Information"),"Server name:");
	}
	
}
