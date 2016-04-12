/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.jboss.ide.eclipse.as.core.util.ServerAttributeHelper;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.ui.internal.util.FormDataUtility;

public class LocationSection extends ServerEditorSection {
	protected ServerAttributeHelper helper; 
	private SelectionListener browseListener;
	private ModifyListener locationListener;
	
	public LocationSection() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		helper = new ServerAttributeHelper(server.getOriginal(), server);
	}

	@Override
	public void createSection(Composite parent) {
		super.createSection(parent);
		createUI(parent);
		setDefaultValues();
		addListeners();
	}
	
	private Text location;
	private Button browse;
	
	protected void createUI(Composite parent) {
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE|ExpandableComposite.TITLE_BAR);
		section.setText("CDK Details");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		Composite composite = toolkit.createComposite(section);
		composite.setLayout(new FormLayout());

		
		Label l = toolkit.createLabel(composite, "Vagrantfile Location: ");
		location = toolkit.createText(composite, "");
		browse = toolkit.createButton(composite, "Browse...", SWT.PUSH);
		
		FormDataUtility fdu = new FormDataUtility();
		l.setLayoutData(fdu.createFormData(0,5,null,0,0,5,null,0));
		FormData locationData = fdu.createFormData(0,5,null,0,l,5,browse,-5);
		locationData.width = 150;
		location.setLayoutData(locationData);
		browse.setLayoutData(fdu.createFormData(0,5,null,0,null,0,100,-5));
		
		section.setClient(composite);

	}
	
	protected void setDefaultValues() {
		// set initial values
		String s = server.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		location.setText(s == null ? "" : s);
	}
	

	protected void addListeners() {
		browseListener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseHomeDirClicked();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		browse.addSelectionListener(browseListener);

		
		locationListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new SetLocationPropertyCommand(server));
			}
		};
		location.addModifyListener(locationListener);

	}
	
	protected void browseHomeDirClicked() {
		File file = location.getText() == null ? null : new File( location.getText());
		if (file != null && !file.exists()) {
			file = null;
		}

		File directory = getDirectory(file,  location.getShell());
		if (directory != null) {
			String newHomeVal = directory.getAbsolutePath();
			if( newHomeVal != null && !newHomeVal.equals(location.getText())) {
				location.setText(newHomeVal);
			}
		}
	}
	

	protected static File getDirectory(File startingDirectory, Shell shell) {
		DirectoryDialog fileDialog = new DirectoryDialog(shell, SWT.OPEN);
		if (startingDirectory != null) {
			fileDialog.setFilterPath(startingDirectory.getPath());
		}

		String dir = fileDialog.open();
		if (dir != null) {
			dir = dir.trim();
			if (dir.length() > 0) {
				return new File(dir);
			}
		}
		return null;
	}

	
	
	public class SetLocationPropertyCommand extends org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyTextCommand {
		public SetLocationPropertyCommand(IServerWorkingCopy server) {
			super(server, "Modify Vagrantfile Location",  
					location, location.getText(), CDKServer.PROP_FOLDER, locationListener);
		}
	}
	
}
