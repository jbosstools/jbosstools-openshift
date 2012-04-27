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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

public class ExpressDetailsSection extends ServerEditorSection {
	private IEditorInput input;
	protected Text userText, remoteText;
	protected Text deployFolderText;
	protected Text appNameText, deployProjectText;
	protected Button verifyButton; // browseDestButton;

	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		this.input = input;
	}

	public void createSection(Composite parent) {
		super.createSection(parent);
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED|ExpandableComposite.TITLE_BAR);
		section.setText("Openshift Server");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL| GridData.GRAB_VERTICAL));
		Composite c = new Composite(section, SWT.NONE);
		c.setLayout(new GridLayout(2,true));
		createWidgets(c);
		toolkit.paintBordersFor(c);
		toolkit.adapt(c);
		section.setClient(c);
		
		initWidgets();
//		addListeners();
	}
	
	protected void initWidgets() {
		// Set the widgets
		String user = ExpressServerUtils.getExpressUsername(server);
		String appName = ExpressServerUtils.getExpressApplicationName(server);
		String depProj = ExpressServerUtils.getExpressDeployProject(server);
		userText.setText(user == null ? "" : user);
		appNameText.setText(appName == null ? "" : appName);
		deployProjectText.setText(depProj == null ? "" : depProj);
		userText.setEnabled(false);
		appNameText.setEnabled(false);
		deployProjectText.setEnabled(false);
		
		String outDir = ExpressServerUtils.getExpressDeployFolder(server);
		String remote = ExpressServerUtils.getExpressRemoteName(server);
		deployFolderText.setText(outDir == null ? "" : outDir);
		remoteText.setText(remote == null ? "" : remote);
		remoteText.setEnabled(false);
		deployFolderText.setEnabled(false);
	}
	
	protected Composite createComposite(Section section) {
		createWidgets(section);
		return section;
	}
	
	private void createWidgets(Composite composite) {
		composite.setLayout(new GridLayout(2, false));
		
		Label deployLocationLabel = new Label(composite, SWT.NONE);
		deployProjectText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectText);
		

		Label userLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(userLabel);
		userText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(userText);
		Label appNameLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);
		appNameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(appNameText);
		
		Label zipDestLabel = new Label(composite, SWT.NONE);
		
//		Composite zipDestComposite = new Composite(composite, SWT.NONE);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(zipDestComposite);
//		zipDestComposite.setLayout(new FormLayout());
//		browseDestButton = new Button(zipDestComposite, SWT.PUSH);
//		browseDestButton.setLayoutData(UIUtil.createFormData2(0,5,100,-5,null,0,100,0));
		deployFolderText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		//deployFolderText.setLayoutData(UIUtil.createFormData2(0,5,100,-5,0,0,100,0));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployFolderText);
		
		Label remoteLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);

		// Text
		appNameLabel.setText("Application Name: ");
		deployLocationLabel.setText("Deploy Project: " );
		zipDestLabel.setText("Output Directory: ");
		userLabel.setText("Username: ");
		remoteLabel.setText("Remote: ");
//		browseDestButton.setText("Browse...");
	}
	
//	ModifyListener remoteModifyListener, deployDestinationModifyListener;
//	protected void addListeners() {
//		remoteModifyListener = new ModifyListener() {
//			public void modifyText(ModifyEvent e) {
//				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetRemoteCommand(server));
//			}
//		};
//		remoteText.addModifyListener(remoteModifyListener);
//		deployDestinationModifyListener = new ModifyListener() {
//			public void modifyText(ModifyEvent e) {
//				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetDeployFolderCommand(server));
//			}
//		};
//		deployFolderText.addModifyListener(deployDestinationModifyListener);
//		
//		browseDestButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				browsePressed();
//			}
//		});

//	}
//	
//	private void browsePressed() {
//		IFolder f = chooseFolder();
//		if( f != null ) {
//			deployFolderText.setText(f.getFullPath().removeFirstSegments(1).makeRelative().toOSString());
//		}
//	}
	
//	private IFolder chooseFolder() {
//		String depProject = ExpressServerUtils.getExpressDeployProject(server);
//		String depFolder = ExpressServerUtils.getExpressDeployFolder(server);
//		
//		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(depProject);
//
//		ILabelProvider lp= new WorkbenchLabelProvider();
//		ITreeContentProvider cp= new WorkbenchContentProvider();
//
//		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), lp, cp);
//		dialog.setTitle("Deploy Location");
//		dialog.setMessage("Please choose a location to put zipped projects");
//		dialog.setInput(p);
//		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
//		
//		IResource res= p.findMember(new Path(depFolder));
//		if (res != null)
//			dialog.setInitialSelection(res);
//
//		if (dialog.open() == Window.OK)
//			return (IFolder) dialog.getFirstResult();
//		return null;
//	}
//	
//	public class SetRemoteCommand extends ServerWorkingCopyPropertyCommand {
//		public SetRemoteCommand(IServerWorkingCopy server) {
//			super(server, "Change Remote Name", remoteText, remoteText.getText(),
//					ExpressServerUtils.ATTRIBUTE_REMOTE_NAME, remoteModifyListener, 
//					ExpressServerUtils.ATTRIBUTE_REMOTE_NAME_DEFAULT);
//		}
//	}
//	
//	public class SetDeployFolderCommand extends ServerWorkingCopyPropertyCommand {
//		public SetDeployFolderCommand(IServerWorkingCopy server) {
//			super(server, "Change Deployment Folder", deployFolderText, deployFolderText.getText(),
//					ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_NAME, deployDestinationModifyListener, 
//					ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
//		}
//	}
}
