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
package org.jboss.tools.openshift.express.internal.ui.server;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;

/**
 * @author Rob Stryker
 */
public class OpenShiftServerEditorSection extends ServerEditorSection {
	private static final String DEFAULT_HOST_MARKER = " (default)";

	private IEditorInput input;
	protected Text connectionText, remoteText;
	protected Text deployFolderText;
	protected Text appNameText;
	protected Text domainNameText;
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

		Section section = toolkit.createSection(parent,
				ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED | ExpandableComposite.TITLE_BAR);
		section.setText("OpenShift Server Adapter");
		section.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
						| GridData.GRAB_VERTICAL));
		Composite c = toolkit.createComposite(section, SWT.NONE);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).equalWidth(true).applyTo(c);
		createWidgets(c, toolkit);
		toolkit.paintBordersFor(c);
		toolkit.adapt(c);
		section.setClient(c);

		initWidgets();
		addListeners();
	}

	protected void initWidgets() {
		// Set the widgets
		deployProjectCombo.setEnabled(true);
		ConnectionURL connectionUrl = OpenShiftServerUtils.getConnectionUrl(server);
		connectionText.setText(createConnectionLabel(connectionUrl));
		String domainName = OpenShiftServerUtils.getDomainName(server);
		domainNameText.setText(StringUtils.null2emptyString(domainName));
		String appName = OpenShiftServerUtils.getApplicationName(server);
		appNameText.setText(StringUtils.null2emptyString(appName));
		connectionText.setEnabled(false);
		domainNameText.setEnabled(false);
		appNameText.setEnabled(false);

		deployFolderText.setText(StringUtils.null2emptyString(OpenShiftServerUtils.getDeployFolder(server)));
		String remote = OpenShiftServerUtils.getRemoteName(server);
		remoteText.setText(StringUtils.null2emptyString(remote));

		deployProjectCombo.setItems(getSuitableProjects());
		int index = getProjectIndex(
				OpenShiftServerUtils.getDeployProjectName(server), Arrays.asList(deployProjectCombo.getItems()));
		if (index > -1) {
			deployProjectCombo.select(index);
		}

		boolean overrides = OpenShiftServerUtils.getOverridesProject(server);
		overrideProjectSettings.setSelection(overrides);
		remoteText.setEnabled(overrides);
		deployFolderText.setEnabled(overrides);
		browseDestButton.setEnabled(overrides);
	}

	private int getProjectIndex(String deployProject, List<String> deployProjectNames) {
		if (deployProject == null) {
			return -1;
		}
		return deployProjectNames.indexOf(deployProject);
	}

	private String createConnectionLabel(ConnectionURL connectionUrl) {
		String connectionLabel = "";
		if (connectionUrl != null) {
			Connection connection = new Connection(connectionUrl.getUsername(), connectionUrl.getHost());
			StringBuilder builder =
					new StringBuilder(connection.getUsername()).append(" - ").append(connection.getHost());
			if (connectionUrl.isDefaultHost()) {
				builder.append(DEFAULT_HOST_MARKER);
			}
			connectionLabel = builder.toString();
		}
		return connectionLabel;
	}

	private String[] getSuitableProjects() {
		IProject[] allProjects = OpenShiftServerUtils.findAllSuitableOpenshiftProjects();
		String[] allProjectNames = new String[allProjects.length];
		for (int i = 0; i < allProjects.length; i++) {
			allProjectNames[i] = allProjects[i].getName();
		}
		return allProjectNames;
	}

	protected Composite createComposite(Section section) {
		createWidgets(section, new FormToolkit(section.getDisplay()));
		return section;
	}

	private void createWidgets(Composite composite, FormToolkit toolkit) {
		GridLayoutFactory.fillDefaults()
				.numColumns(2).equalWidth(false).applyTo(composite);

		Label deployLocationLabel =
				toolkit.createLabel(composite, OpenshiftUIMessages.EditorSectionDeployLocLabel, SWT.NONE);
		deployProjectCombo = new Combo(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectCombo);

		projectSettingGroup = new Group(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1)
				.applyTo(projectSettingGroup);
		projectSettingGroup.setLayout(new GridLayout(2, false));
		projectSettingGroup.setText(OpenshiftUIMessages.EditorSectionProjectSettingsGroup);

		overrideProjectSettings = toolkit.createButton(projectSettingGroup,
				OpenshiftUIMessages.EditorSectionOverrideProjectSettings, SWT.CHECK);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1).applyTo(overrideProjectSettings);

		Label userLabel = toolkit
				.createLabel(projectSettingGroup, OpenshiftUIMessages.EditorSectionConnectionLabel, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(userLabel);
		connectionText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(connectionText);

		// domain name
		Label domainNameLabel = toolkit.createLabel(
				projectSettingGroup, OpenshiftUIMessages.EditorSectionDomainNameLabel, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(domainNameLabel);
		domainNameText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(domainNameText);

		// application name
		Label appNameLabel = toolkit.createLabel(
				projectSettingGroup, OpenshiftUIMessages.EditorSectionAppNameLabel,SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);
		appNameText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(appNameText);

		Label outputDestLabel = 
				toolkit.createLabel(projectSettingGroup, OpenshiftUIMessages.EditorSectionOutputDestLabel, SWT.NONE);
		Composite outputDestComposite = toolkit.createComposite(projectSettingGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(outputDestComposite);
		outputDestComposite.setLayout(new FormLayout());

		browseDestButton = toolkit.createButton(
				outputDestComposite, OpenshiftUIMessages.EditorSectionBrowseDestButton, SWT.PUSH);
		browseDestButton.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, null, 0, 100, 0));
		deployFolderText = toolkit.createText(outputDestComposite, "", SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, 0, 0, browseDestButton, -5));

		Label remoteLabel = toolkit.createLabel(
				projectSettingGroup, OpenshiftUIMessages.EditorSectionRemoteLabel, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);
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
			}
		};
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
		if (f != null) {
			deployFolderText.setText(f.getFullPath().removeFirstSegments(1).makeRelative().toOSString());
		}
	}

	private IFolder chooseFolder() {
		String depProject = OpenShiftServerUtils.getDeployProjectName(server);

		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(depProject);

		ElementTreeSelectionDialog dialog = 
				new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), 
						new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setTitle("Deploy Location");
		dialog.setMessage("Please choose a location to put zipped projects");
		dialog.setInput(p);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

		IResource res = p.findMember(new Path(
				StringUtils.null2emptyString(OpenShiftServerUtils.getDeployFolder(server))));
		if (res != null)
			dialog.setInitialSelection(res);

		if (dialog.open() == Window.OK)
			return (IFolder) dialog.getFirstResult();
		return null;
	}

	public class SetRemoteCommand extends ServerWorkingCopyPropertyCommand {
		public SetRemoteCommand(IServerWorkingCopy server) {
			super(server, "Change Remote Name", remoteText, remoteText.getText(),
					OpenShiftServerUtils.ATTRIBUTE_REMOTE_NAME, remoteModifyListener,
					OpenShiftServerUtils.ATTRIBUTE_REMOTE_NAME_DEFAULT);
		}
	}

	public class SetProjectCommand extends ServerWorkingCopyPropertyComboCommand {
		public SetProjectCommand(IServerWorkingCopy wc, String newVal) {
			super(wc, "Change OpenShift Project", deployProjectCombo, newVal,
					OpenShiftServerUtils.ATTRIBUTE_DEPLOY_PROJECT, deployProjectListener);
		}

		@Override
		protected void postOp(int type) {
			updateWidgetsFromWorkingCopy();
		}
	}

	public class SetDeployFolderCommand extends ServerWorkingCopyPropertyCommand {
		public SetDeployFolderCommand(IServerWorkingCopy server) {
			super(server, "Change Deployment Folder", deployFolderText, deployFolderText.getText(),
					OpenShiftServerUtils.ATTRIBUTE_DEPLOY_FOLDER_NAME, deployDestinationModifyListener,
					OpenShiftServerUtils.getDefaultDeployFolder(server));
		}
	}

	public class SetOverrideCommand extends ServerWorkingCopyPropertyButtonCommand {
		public SetOverrideCommand(IServerWorkingCopy wc) {
			super(wc, "Override OpenShift Project Settings Command",
					overrideProjectSettings, overrideProjectSettings.getSelection(),
					OpenShiftServerUtils.ATTRIBUTE_OVERRIDE_PROJECT_SETTINGS, overrideListener);
		}

		@Override
		protected void postOp(int type) {
			updateWidgetsFromWorkingCopy();
		}
	}

	private void updateWidgetsFromWorkingCopy() {
		ConnectionURL connectionUrl = OpenShiftServerUtils.getConnectionUrl(server);
		connectionText.setText(createConnectionLabel(connectionUrl));
		String appName = OpenShiftServerUtils.getApplicationName(server);
		appNameText.setText(StringUtils.null2emptyString(appName));
		String domainName = OpenShiftServerUtils.getDomainName(server);
		domainNameText.setText(StringUtils.null2emptyString(domainName));

		browseDestButton.setEnabled(overrideProjectSettings.getSelection());
		deployFolderText.setEnabled(overrideProjectSettings.getSelection());
		remoteText.setEnabled(overrideProjectSettings.getSelection());
		String remote = OpenShiftServerUtils.getRemoteName(server, OpenShiftServerUtils.SETTING_FROM_PROJECT);
		String depFolder = OpenShiftServerUtils.getDeployFolder(server, OpenShiftServerUtils.SETTING_FROM_PROJECT);

		remoteText.removeModifyListener(remoteModifyListener);
		deployFolderText.removeModifyListener(deployDestinationModifyListener);
		remoteText.setText(StringUtils.null2emptyString(remote));
		deployFolderText.setText(StringUtils.null2emptyString(depFolder));
		remoteText.addModifyListener(remoteModifyListener);
		deployFolderText.addModifyListener(deployDestinationModifyListener);
	}
}
