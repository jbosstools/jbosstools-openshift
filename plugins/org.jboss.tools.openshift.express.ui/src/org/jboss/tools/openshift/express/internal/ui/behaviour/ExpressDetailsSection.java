/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.jboss.ide.eclipse.as.core.server.internal.ServerAttributeHelper;
import org.jboss.ide.eclipse.as.ui.Messages;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

public class ExpressDetailsSection extends ServerEditorSection {
	private ModifyListener nameModifyListener, remoteModifyListener, passModifyListener;
	private Text nameText, passText, remoteText;
	private ServerAttributeHelper helper;
	
	private String passwordString;

	public void createSection(Composite parent) {
		super.createSection(parent);
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED|ExpandableComposite.TITLE_BAR);
		section.setText("Express Server in Source Mode (Details)");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		Composite composite = toolkit.createComposite(section);
		composite.setLayout(new GridLayout(2, false));

		fillSection(composite, toolkit);
		addListeners();
		
		toolkit.paintBordersFor(composite);
		section.setClient(composite);
	}
	
	private void fillSection(Composite composite, FormToolkit toolkit) {

		GridData d = new GridData(); d.horizontalSpan = 2;
		
		Label appName = toolkit.createLabel(composite, "Application Name");
		Label appNameVal = toolkit.createLabel(composite, ExpressServerUtils.getExpressApplicationName(server));
		Label appId = toolkit.createLabel(composite, "Application Id");
		Label appIdVal = toolkit.createLabel(composite, ExpressServerUtils.getExpressApplicationId(server));

		Label username = toolkit.createLabel(composite, Messages.swf_Username);
		username.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		String n = ExpressServerUtils.getExpressUsername(server);
		nameText = toolkit.createText(composite, n); 
		String p = ExpressServerUtils.getExpressPassword(server.getOriginal());
		Label password = toolkit.createLabel(composite, Messages.swf_Password);
		password.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		passText = toolkit.createText(composite, p);
		passwordString = p;
		String remote = ExpressServerUtils.getExpressRemoteName(server);
		Label remoteName = toolkit.createLabel(composite, "Remote Name");
		remoteName.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		remoteText = toolkit.createText(composite, remote);
		
		d = new GridData(); d.grabExcessHorizontalSpace = true; d.widthHint = 100;
		nameText.setLayoutData(d);
		d = new GridData(); d.grabExcessHorizontalSpace = true; d.widthHint = 100;
		passText.setLayoutData(d);

	}
	
	private void addListeners() {
		nameModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				execute(new SetUserCommand(server));
			}
		};
		remoteModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				execute(new SetRemoteCommand(server));
			}
		};
		
		passModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				execute(new SetPassCommand(server));
			}
		};

		nameText.addModifyListener(nameModifyListener);
		remoteText.addModifyListener(remoteModifyListener);
		passText.addModifyListener(passModifyListener);
	}
	
	public class SetUserCommand extends ServerWorkingCopyPropertyCommand {
		public SetUserCommand(IServerWorkingCopy server) {
			super(server, Messages.EditorChangeUsernameCommandName, nameText, nameText.getText(), 
					ExpressServerUtils.ATTRIBUTE_USERNAME, nameModifyListener);
		}
	}

	public class SetRemoteCommand extends ServerWorkingCopyPropertyCommand {
		public SetRemoteCommand(IServerWorkingCopy server) {
			super(server, "Change Remote Name", remoteText, remoteText.getText(), 
					ExpressServerUtils.ATTRIBUTE_REMOTE_NAME, remoteModifyListener);
		}
	}

	public class SetPassCommand extends ServerWorkingCopyPropertyCommand {
		public SetPassCommand(IServerWorkingCopy server) {
			super(server, Messages.EditorChangePasswordCommandName, passText, passText.getText(), 
					null, passModifyListener);
			oldVal = passText.getText();
		}
		
		public void execute() {
			passwordString = newVal;
		}
		
		public void undo() {
			passwordString = oldVal;
			text.removeModifyListener(listener);
			text.setText(oldVal);
			text.addModifyListener(listener);
		}
	}

	/**
	 * Allow a section an opportunity to respond to a doSave request on the editor.
	 * @param monitor the progress monitor for the save operation.
	 */
	public void doSave(IProgressMonitor monitor) {
		try {
			ExpressServerUtils.setExpressPassword(server.getOriginal(), passwordString);
			monitor.worked(100);
		} catch( CoreException ce ) {
			// TODO 
		}
	}

}
