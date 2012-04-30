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

import java.util.Arrays;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.ide.dialogs.ResourceComparator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.eclipse.wst.server.ui.internal.editor.ServerEditorPartInput;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyButtonCommand;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyComboCommand;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

public class ExpressDetailsSection extends ServerEditorSection {
	private IEditorInput input;
	protected Text userText, remoteText;
	protected Text deployFolderText;
	protected Text appNameText;
	protected Combo deployProjectCombo;
	protected Button verifyButton, browseDestButton, overrideProjectSettings;
	protected Group projectSettingGroup;

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
		addListeners();
	}
	
	protected void initWidgets() {
		// Set the widgets
		String user = ExpressServerUtils.getExpressUsername(server);
		String appName = ExpressServerUtils.getExpressApplicationName(server);
		deployProjectCombo.setEnabled(true);
		userText.setText(user == null ? "" : user);
		appNameText.setText(appName == null ? "" : appName);
		userText.setEnabled(false);
		appNameText.setEnabled(false);
		
		String outDir = ExpressServerUtils.getExpressDeployFolder(server);
		String remote = ExpressServerUtils.getExpressRemoteName(server);
		deployFolderText.setText(outDir == null ? "" : outDir);
		remoteText.setText(remote == null ? "" : remote);
		
		deployProjectCombo.setItems(getSuitableProjects());
		java.util.List<String> l = Arrays.asList(deployProjectCombo.getItems());
		String depProj = ExpressServerUtils.getExpressDeployProject(server);
		if( depProj != null ) {
			int ind = l.indexOf(depProj);
			if( ind != -1 ) 
				deployProjectCombo.select(ind);
		}
		
		boolean overrides = ExpressServerUtils.getOverridesProject(server);
		overrideProjectSettings.setSelection(overrides);
		remoteText.setEnabled(overrides);
		deployFolderText.setEnabled(overrides);
		browseDestButton.setEnabled(overrides);
	}
	
	private String[] getSuitableProjects() {
		IProject[] all = ExpressServerUtils.findAllSuitableOpenshiftProjects();
		String[] s = new String[all.length];
		for( int i = 0; i < all.length; i++ ) {
			s[i] = all[i].getName();
		}
		return s;
	}
	
	protected Composite createComposite(Section section) {
		createWidgets(section);
		return section;
	}
	
	private void createWidgets(Composite composite) {
		composite.setLayout(new GridLayout(2, false));
		
		Label deployLocationLabel = new Label(composite, SWT.NONE);
		deployProjectCombo = new Combo(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectCombo);
		

		projectSettingGroup = new Group(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1).applyTo(projectSettingGroup);
		projectSettingGroup.setLayout(new GridLayout(2, false));
		
		overrideProjectSettings = new Button(projectSettingGroup, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1).applyTo(overrideProjectSettings);

		Label userLabel = new Label(projectSettingGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(userLabel);
		userText = new Text(projectSettingGroup, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(userText);
		Label appNameLabel = new Label(projectSettingGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);
		appNameText = new Text(projectSettingGroup, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(appNameText);

		Label zipDestLabel = new Label(projectSettingGroup, SWT.NONE);
		Composite zipDestComposite = new Composite(projectSettingGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(zipDestComposite);
		zipDestComposite.setLayout(new FormLayout());
		browseDestButton = new Button(zipDestComposite, SWT.PUSH);
		browseDestButton.setLayoutData(UIUtil.createFormData2(0,5,100,-5,null,0,100,0));
		deployFolderText = new Text(zipDestComposite, SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0,5,100,-5,0,0,browseDestButton,-5));
		
		Label remoteLabel = new Label(projectSettingGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = new Text(projectSettingGroup, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);
		
		

		// Text
		appNameLabel.setText("Application Name: ");
		deployLocationLabel.setText("Deploy Project: " );
		zipDestLabel.setText("Output Directory: ");
		userLabel.setText("Username: ");
		remoteLabel.setText("Remote: ");
		browseDestButton.setText("Browse...");
		projectSettingGroup.setText("Project Settings:");
		overrideProjectSettings.setText("Override Project Settings");
	}
	
	ModifyListener remoteModifyListener, deployDestinationModifyListener, deployProjectListener;
	SelectionListener overrideListener;
	protected void addListeners() {
		deployProjectListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				int ind = deployProjectCombo.getSelectionIndex();
				String newVal = ind == -1 ? null : deployProjectCombo.getItem(ind);
				((ServerEditorPartInput) input).getServerCommandManager().execute(
						new SetProjectCommand(server, newVal));
			}};
		deployProjectCombo.addModifyListener(deployProjectListener);
		overrideListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetOverrideCommand(server));
			}
		};
		overrideProjectSettings.addSelectionListener(overrideListener);

		
		remoteModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetRemoteCommand(server));
			}
		};
		remoteText.addModifyListener(remoteModifyListener);
		deployDestinationModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetDeployFolderCommand(server));
			}
		};
		deployFolderText.addModifyListener(deployDestinationModifyListener);
		
		browseDestButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browsePressed();
			}
		});

	}
	
	private void browsePressed() {
		IFolder f = chooseFolder();
		if( f != null ) {
			deployFolderText.setText(f.getFullPath().removeFirstSegments(1).makeRelative().toOSString());
		}
	}
	
	private IFolder chooseFolder() {
		String depProject = ExpressServerUtils.getExpressDeployProject(server);
		String depFolder = ExpressServerUtils.getExpressDeployFolder(server);
		
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(depProject);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), lp, cp);
		dialog.setTitle("Deploy Location");
		dialog.setMessage("Please choose a location to put zipped projects");
		dialog.setInput(p);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		
		IResource res= p.findMember(new Path(depFolder));
		if (res != null)
			dialog.setInitialSelection(res);

		if (dialog.open() == Window.OK)
			return (IFolder) dialog.getFirstResult();
		return null;
	}
	
	public class SetRemoteCommand extends ServerWorkingCopyPropertyCommand {
		public SetRemoteCommand(IServerWorkingCopy server) {
			super(server, "Change Remote Name", remoteText, remoteText.getText(),
					ExpressServerUtils.ATTRIBUTE_REMOTE_NAME, remoteModifyListener, 
					ExpressServerUtils.ATTRIBUTE_REMOTE_NAME_DEFAULT);
		}
	}

	public class SetProjectCommand extends ServerWorkingCopyPropertyComboCommand {
		public SetProjectCommand(IServerWorkingCopy wc, String newVal) {
			super(wc, "Change Openshift Project", deployProjectCombo, newVal, ExpressServerUtils.ATTRIBUTE_DEPLOY_PROJECT, deployProjectListener);
		}
		@Override
		protected void postOp(int type) {
			updateWidgetsFromWorkingCopy();
		}
	}

	public class SetDeployFolderCommand extends ServerWorkingCopyPropertyCommand {
		public SetDeployFolderCommand(IServerWorkingCopy server) {
			super(server, "Change Deployment Folder", deployFolderText, deployFolderText.getText(),
					ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_NAME, deployDestinationModifyListener, 
					ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
		}
	}

	public class SetOverrideCommand extends ServerWorkingCopyPropertyButtonCommand {
		public SetOverrideCommand(IServerWorkingCopy wc) {
			super(wc, "Override Openshift Project Settings Command", 
					overrideProjectSettings, overrideProjectSettings.getSelection(), 
					ExpressServerUtils.ATTRIBUTE_OVERRIDE_PROJECT_SETTINGS, overrideListener);
		}
		@Override
		protected void postOp(int type) {
			updateWidgetsFromWorkingCopy();
		}
	}
	private void updateWidgetsFromWorkingCopy() {
		String user = ExpressServerUtils.getExpressUsername(server);
		String appName = ExpressServerUtils.getExpressApplicationName(server);
		userText.setText(user == null ? "" : user);
		appNameText.setText(appName == null ? "" : appName);
		
		browseDestButton.setEnabled(overrideProjectSettings.getSelection());
		deployFolderText.setEnabled(overrideProjectSettings.getSelection());
		remoteText.setEnabled(overrideProjectSettings.getSelection());
		String remote = ExpressServerUtils.getExpressRemoteName(server, ExpressServerUtils.SETTING_FROM_PROJECT);
		String depFolder = ExpressServerUtils.getExpressDeployFolder(server, ExpressServerUtils.SETTING_FROM_PROJECT);
		remote = remote == null ? "" : remote;
		depFolder = depFolder == null ? "" : depFolder;
		
		remoteText.removeModifyListener(remoteModifyListener);
		deployFolderText.removeModifyListener(deployDestinationModifyListener);
		remoteText.setText(remote);
		deployFolderText.setText(depFolder);
		remoteText.addModifyListener(remoteModifyListener);
		deployFolderText.addModifyListener(deployDestinationModifyListener);
	}
}
