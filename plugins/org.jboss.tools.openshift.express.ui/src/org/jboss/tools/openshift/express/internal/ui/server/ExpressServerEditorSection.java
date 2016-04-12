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
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyComboCommand;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyButtonCommand;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.server.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;

/**
 * @author Rob Stryker
 */
public class ExpressServerEditorSection extends ServerEditorSection {
	private static final String DEFAULT_HOST_MARKER = " (default)";

	private IEditorInput input;
	protected Text connectionText, remoteText;
	protected Text deployFolderText;
	protected Text appNameText;
	protected Text domainNameText;
	protected Combo deployProjectCombo;
	protected Button verifyButton, browseDestButton, overrideProjectSettings;
	protected Group projectSettingGroup;

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		this.input = input;
	}

	@Override
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

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				initWidgets();
				addListeners();
			}
		});
	}

	protected void initWidgets() {
		// Set the widgets
		deployProjectCombo.setEnabled(true);
		ConnectionURL connectionUrl = ExpressServerUtils.getConnectionUrl(server);
		connectionText.setText(createConnectionLabel(connectionUrl));
		String domainName = ExpressServerUtils.getDomainName(server);
		domainNameText.setText(StringUtils.null2emptyString(domainName));
		String appName = ExpressServerUtils.getApplicationName(server);
		appNameText.setText(StringUtils.null2emptyString(appName));
		connectionText.setEnabled(false);
		domainNameText.setEnabled(false);
		appNameText.setEnabled(false);

		deployFolderText.setText(StringUtils.null2emptyString(ExpressServerUtils.getDeployFolder(server)));
		String remote = ExpressServerUtils.getRemoteName(server);
		remoteText.setText(StringUtils.null2emptyString(remote));

		deployProjectCombo.setItems(getSuitableProjects());
		int index = getProjectIndex(
				ExpressServerUtils.getDeployProjectName(server), Arrays.asList(deployProjectCombo.getItems()));
		if (index > -1) {
			deployProjectCombo.select(index);
		}

		boolean overrides = ExpressServerUtils.isOverridesProject(server);
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
			ExpressConnection connection = new ExpressConnection(connectionUrl.getUsername(), connectionUrl.getHost());
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
		IProject[] allProjects = ExpressServerUtils.getAllOpenshiftProjects();
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
				toolkit.createLabel(composite, ExpressUIMessages.EditorSectionDeployLocLabel, SWT.NONE);
		deployProjectCombo = new Combo(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectCombo);

		projectSettingGroup = new Group(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1)
				.applyTo(projectSettingGroup);
		projectSettingGroup.setLayout(new GridLayout(2, false));
		projectSettingGroup.setText(ExpressUIMessages.EditorSectionProjectSettingsGroup);

		overrideProjectSettings = toolkit.createButton(projectSettingGroup,
				ExpressUIMessages.EditorSectionOverrideProjectSettings, SWT.CHECK);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1).applyTo(overrideProjectSettings);

		// connection
		Label connectionLabel = toolkit
				.createLabel(projectSettingGroup, ExpressUIMessages.EditorSectionConnectionLabel, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(connectionLabel);
		connectionText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(connectionText);

		// domain name
		Label domainNameLabel = toolkit.createLabel(
				projectSettingGroup, ExpressUIMessages.EditorSectionDomainNameLabel, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(domainNameLabel);
		domainNameText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(domainNameText);

		// application name
		Label appNameLabel = toolkit.createLabel(
				projectSettingGroup, ExpressUIMessages.EditorSectionAppNameLabel,SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);
		appNameText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(appNameText);

		Label outputDestLabel = 
				toolkit.createLabel(projectSettingGroup, ExpressUIMessages.EditorSectionOutputDestLabel, SWT.NONE);
		Composite outputDestComposite = toolkit.createComposite(projectSettingGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(outputDestComposite);
		outputDestComposite.setLayout(new FormLayout());

		browseDestButton = toolkit.createButton(
				outputDestComposite, ExpressUIMessages.EditorSectionBrowseDestButton, SWT.PUSH);
		browseDestButton.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, null, 0, 100, 0));
		deployFolderText = toolkit.createText(outputDestComposite, "", SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, 0, 0, browseDestButton, -5));

		Label remoteLabel = toolkit.createLabel(
				projectSettingGroup, ExpressUIMessages.EditorSectionRemoteLabel, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);
	}

	ModifyListener remoteModifyListener, deployDestinationModifyListener, deployProjectListener;
	SelectionListener overrideListener;

	protected void addListeners() {
		deployProjectListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int ind = deployProjectCombo.getSelectionIndex();
				String newVal = ind == -1 ? null : deployProjectCombo.getItem(ind);
				((ServerEditorPartInput) input).getServerCommandManager().execute(
						new SetProjectCommand(server, newVal));
			}
		};
		deployProjectCombo.addModifyListener(deployProjectListener);
		overrideListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetOverrideCommand(server));
			}
		};
		overrideProjectSettings.addSelectionListener(overrideListener);

		remoteModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetRemoteCommand(server));
			}
		};
		remoteText.addModifyListener(remoteModifyListener);
		deployDestinationModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				((ServerEditorPartInput) input).getServerCommandManager().execute(new SetDeployFolderCommand(server));
			}
		};
		deployFolderText.addModifyListener(deployDestinationModifyListener);

		browseDestButton.addSelectionListener(new SelectionAdapter() {
			@Override
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
		String depProject = ExpressServerUtils.getDeployProjectName(server);

		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(depProject);

		ElementTreeSelectionDialog dialog = 
				new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), 
						new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setTitle("Deploy Location");
		dialog.setMessage("Please choose a location to put zipped projects");
		dialog.setInput(p);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

		IResource res = p.findMember(new Path(
				StringUtils.null2emptyString(ExpressServerUtils.getDeployFolder(server))));
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
			super(wc, "Change OpenShift Project", deployProjectCombo, newVal,
					ExpressServerUtils.ATTRIBUTE_DEPLOY_PROJECT, deployProjectListener);
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
					ExpressServerUtils.getDefaultDeployFolder(server));
		}
	}

	public class SetOverrideCommand extends ServerWorkingCopyPropertyButtonCommand {
		public SetOverrideCommand(IServerWorkingCopy wc) {
			super(wc, "Override OpenShift Project Settings Command",
					overrideProjectSettings, overrideProjectSettings.getSelection(),
					ExpressServerUtils.ATTRIBUTE_OVERRIDE_PROJECT_SETTINGS, overrideListener);
		}

		@Override
		protected void postOp(int type) {
			updateWidgetsFromWorkingCopy();
		}
	}

	private void updateWidgetsFromWorkingCopy() {
		ConnectionURL connectionUrl = ExpressServerUtils.getConnectionUrl(server);
		connectionText.setText(createConnectionLabel(connectionUrl));
		String appName = ExpressServerUtils.getApplicationName(server);
		appNameText.setText(StringUtils.null2emptyString(appName));
		String domainName = ExpressServerUtils.getDomainName(server);
		domainNameText.setText(StringUtils.null2emptyString(domainName));

		browseDestButton.setEnabled(overrideProjectSettings.getSelection());
		deployFolderText.setEnabled(overrideProjectSettings.getSelection());
		remoteText.setEnabled(overrideProjectSettings.getSelection());
		String remote = ExpressServerUtils.getRemoteName(server, ExpressServerUtils.SETTING_FROM_PROJECT);
		String depFolder = ExpressServerUtils.getDeployFolder(server, ExpressServerUtils.SETTING_FROM_PROJECT);

		remoteText.removeModifyListener(remoteModifyListener);
		deployFolderText.removeModifyListener(deployDestinationModifyListener);
		remoteText.setText(StringUtils.null2emptyString(remote));
		deployFolderText.setText(StringUtils.null2emptyString(depFolder));
		remoteText.addModifyListener(remoteModifyListener);
		deployFolderText.addModifyListener(deployDestinationModifyListener);
	}
}
