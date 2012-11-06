/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.portforward;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * 
 */
public class RemoteOpenShiftApplicationConfigurationTab extends AbstractLaunchConfigurationTab {

	public static final String LAUNCH_CONFIG_PROJECT = "project";

	public static final String LAUNCH_CONFIG_APPLICATION = "application";

	private Text projectNameText;

	private Text applicationNameText;

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		setControl(container);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		createApplicationSelector(container);
		createProjectSelector(container);
	}

	private void createProjectSelector(Composite container) {
		Group projectGroup = new Group(container, SWT.NONE);
		projectGroup.setText("Project");
		GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).applyTo(projectGroup);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(projectGroup);
		this.projectNameText = new Text(projectGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, true).applyTo(projectNameText);
		projectNameText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

	}

	/**
	 * Updates the buttons and message in this page's launch configuration dialog.
	 */
	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			// order is important here due to the call to
			// refresh the tab viewer in updateButtons()
			// which ensures that the messages are up to date
			getLaunchConfigurationDialog().updateButtons();
			getLaunchConfigurationDialog().updateMessage();
		}
	}

	private void createApplicationSelector(Composite container) {
		Group projectGroup = new Group(container, SWT.NONE);
		projectGroup.setText("Application");
		GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).applyTo(projectGroup);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(projectGroup);
		this.applicationNameText = new Text(projectGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, true).applyTo(applicationNameText);

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectNameText.setText(configuration.getAttribute(LAUNCH_CONFIG_PROJECT, ""));
			applicationNameText.setText(configuration.getAttribute(LAUNCH_CONFIG_APPLICATION, ""));
		} catch (CoreException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#canSave()
	 */
	@Override
	public boolean canSave() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return isValidProject() && isValidApplication();
	}

	/**
	 * @return true is the projectNameText value is an existing & opened Eclipse project in the workspace, false
	 *         otherwise.
	 */
	private boolean isValidProject() {
		final String projectName = projectNameText.getText();
		if (projectName == null || projectName.isEmpty()) {
			return false;
		}
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		return project != null && project.exists() && project.isOpen();
	}

	/**
	 * @return true is the projectNameText value is an existing & opened Eclipse project in the workspace, false
	 *         otherwise.
	 */
	private boolean isValidApplication() {
		final String applicationName = applicationNameText.getText();
		if (applicationName == null || applicationName.isEmpty()) {
			return false;
		}
		try {
			for (Connection user : ConnectionsModelSingleton.getInstance().getConnections()) {
				final IApplication application = user.getApplicationByName(applicationName);
				if (application != null) {
					return true;
				}
			}
		} catch (OpenShiftException e) {
			Logger.error("Failed to retrieve applications from user", e);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isDirty()
	 */
	@Override
	protected boolean isDirty() {
		return true;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(LAUNCH_CONFIG_PROJECT, projectNameText.getText());
		config.setAttribute(LAUNCH_CONFIG_APPLICATION, applicationNameText.getText());
		
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectNameText.getText().trim());
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, JavaRuntime.getVMConnector("org.eclipse.jdt.launching.socketAttachConnector").getIdentifier()); // see org.eclipse.jdt.launching plugin.xml for values 
		//mapResources(config);
		Map attrMap = new HashMap();
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, attrMap);
		
	}

	@Override
	public String getName() {
		return "Connect";
	}

	@Override
	public Image getImage() {
		return DebugUITools.getImage(IDebugUIConstants.IMG_LCL_DISCONNECT);
	}

}
