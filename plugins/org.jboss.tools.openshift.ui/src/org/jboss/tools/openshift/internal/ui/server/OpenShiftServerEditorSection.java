/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.eclipse.wst.server.ui.internal.editor.ServerEditorPartInput;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyButtonCommand;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyComboCommand;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionFactory;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerEditorSection extends ServerEditorSection {
	private static final String DEFAULT_HOST_MARKER = " (default)";

	private IEditorInput input;
	protected Text connectionText;
	protected Text projectNameText;
	protected Text buildConfigText;
	protected Combo deployProjectCombo;
	protected Button verifyButton, overrideProjectSettings;
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
//		ConnectionURL connectionUrl = OpenShiftServerUtils.getConnectionUrl(server);
//		connectionText.setText(createConnectionLabel(connectionUrl));
//		String openShiftProjectName = OpenShiftServerUtils.getOpenShiftProject(server);
//		projectNameText.setText(StringUtils.null2emptyString(openShiftProjectName));
		connectionText.setEnabled(false);
		projectNameText.setEnabled(false);

		deployProjectCombo.setItems(ProjectUtils.getAllAccessibleProjectNames());
		int index = getProjectIndex(
				OpenShiftServerUtils.getDeployProjectName(server), Arrays.asList(deployProjectCombo.getItems()));
		if (index > -1) {
			deployProjectCombo.select(index);
		}

		overrideProjectSettings.setSelection(OpenShiftServerUtils.isOverridesProject(server));
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
			Connection connection = 
					new ConnectionFactory().create(connectionUrl.getHostWithScheme());
			StringBuilder builder =
					new StringBuilder(connection.getUsername()).append(" - ").append(connection.getHost());
			if (connectionUrl.isDefaultHost()) {
				builder.append(DEFAULT_HOST_MARKER);
			}
			connectionLabel = builder.toString();
		}
		return connectionLabel;
	}

	protected Composite createComposite(Section section) {
		createWidgets(section, new FormToolkit(section.getDisplay()));
		return section;
	}

	private void createWidgets(Composite composite, FormToolkit toolkit) {
		GridLayoutFactory.fillDefaults()
				.numColumns(2).equalWidth(false).applyTo(composite);

		deployProjectCombo = new Combo(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectCombo);

		projectSettingGroup = new Group(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1)
				.applyTo(projectSettingGroup);
		projectSettingGroup.setLayout(new GridLayout(2, false));
		projectSettingGroup.setText("Project Settings:");

		overrideProjectSettings = toolkit.createButton(projectSettingGroup,
				"Override Project Settings", SWT.CHECK);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1).applyTo(overrideProjectSettings);

		// connection
		Label connectionLabel = toolkit
				.createLabel(projectSettingGroup, "Connection:", SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(connectionLabel);
		connectionText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(connectionText);

		// project name
		Label projectNameLabel = toolkit.createLabel(
				projectSettingGroup, "Project:", SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(projectNameLabel);
		projectNameText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(projectNameText);

		// build config
		Label buildConfigLabel = toolkit.createLabel(
				projectSettingGroup, "Build Config",SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(buildConfigLabel);
		buildConfigText = toolkit.createText(projectSettingGroup, "", SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(buildConfigText);
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
	}

	public class SetProjectCommand extends ServerWorkingCopyPropertyComboCommand {
		public SetProjectCommand(IServerWorkingCopy wc, String newVal) {
			super(wc, "Change OpenShift Project", deployProjectCombo, newVal,
					OpenShiftServerUtils.ATTR_DEPLOYPROJECT, deployProjectListener);
		}

		@Override
		protected void postOp(int type) {
			updateWidgetsFromWorkingCopy();
		}
	}

	public class SetOverrideCommand extends ServerWorkingCopyPropertyButtonCommand {
		public SetOverrideCommand(IServerWorkingCopy wc) {
			super(wc, "Override OpenShift Project Settings Command",
					overrideProjectSettings, overrideProjectSettings.getSelection(),
					OpenShiftServerUtils.ATTR_OVERRIDE_PROJECT_SETTINGS, overrideListener);
		}

		@Override
		protected void postOp(int type) {
			updateWidgetsFromWorkingCopy();
		}
	}

	private void updateWidgetsFromWorkingCopy() {
//		connectionText.setText(createConnectionLabel(OpenShiftServerUtils.getConnectionUrl(server)));
//		buildConfigText.setText("");
//		projectNameText.setText(StringUtils.null2emptyString(OpenShiftServerUtils.getOpenShiftProject(server)));
	}
}
