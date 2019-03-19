/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.advanced;

import static org.junit.Assert.assertNotNull;
import org.apache.commons.lang.StringUtils;

import static org.hamcrest.core.IsNot.not;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.api.Text;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.ResourceIsUpdated;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.view.resources.Service;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author mlabuda@redhat.com
 * @author adietish
 *
 */
@RunWith(RedDeerSuite.class)
@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection()
@CleanConnection
@RequiredProject()
@RequiredService(service=OpenShiftResources.NODEJS_SERVICE, 
template=OpenShiftResources.NODEJS_TEMPLATE)

public class EditResourceLimitsTest extends AbstractTest {
	@InjectRequirement
	private OpenShiftProjectRequirement requiredProject;
	@InjectRequirement
	private OpenShiftConnectionRequirement requiredConnection;
	@InjectRequirement
	private OpenShiftServiceRequirement requiredService;
	private OpenShift3Connection connection;
	private OpenShiftProject project;

	@Before
	public void setUp() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		this.connection = explorer.getOpenShift3Connection(requiredConnection.getConnection());
		this.project = connection.getProject(requiredProject.getProjectName());
		this.project.expand();
	}

	@Test
	public void testEditMemoryRequestViaContextMenuOfService() {
		testViaContextMenuOfService(0, "MiB (1024 KB)");
	}

	@Test
	public void testEditMemoryLimitViaContextMenuOfService() {
		testViaContextMenuOfService(1, "MiB (1024 KB)");
	}

	@Test
	public void testEditCPURequestViaContextMenuOfService() {
		testViaContextMenuOfService(2, "cores");
	}

	@Test
	public void testEditCPULimitViaContextMenuOfService() {
		testViaContextMenuOfService(3, "cores");
	}

	public void testViaContextMenuOfService(int index, String defaultSuffix) {
		Service service = project.getServicesWithName(OpenShiftResources.NODEJS_SERVICE).get(0);
		assertNotNull(service);
		OpenShiftResource deploymentConfig = 
				project.getOpenShiftResource(Resource.DEPLOYMENT_CONFIG, OpenShiftResources.NODEJS_APP_DEPLOYMENT_CONFIG);
		assertNotNull(deploymentConfig);
		String version = deploymentConfig.getPropertyValue("Basic", "Resource Version");
		service.select();
		incrementLimits(index, defaultSuffix);
		new WaitUntil(new ResourceIsUpdated(project.getName(), Resource.DEPLOYMENT_CONFIG,
				      OpenShiftResources.NODEJS_APP_DEPLOYMENT_CONFIG, new String[] {"Basic", "Resource Version"},
				      not(version), requiredConnection.getConnection()), TimePeriod.getCustom(800));
	}

	private void incrementLimits(int index, String defaultSuffix) {
		new ContextMenuItem(OpenShiftLabel.ContextMenu.EDIT_RESOURCE_LIMITS).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.EDIT_RESOURCE_LIMITS));
		new DefaultShell(OpenShiftLabel.Shell.EDIT_RESOURCE_LIMITS).setFocus();
		Text text = new DefaultText(index);
		String value = text.getText();
		long val = 0L;
		try {
			val = Long.parseLong(value);
		} catch (NumberFormatException e) {}
		text.setText(Long.toString(++val));
		Combo combo = new DefaultCombo(index);
		value = combo.getSelection();
		if (StringUtils.isEmpty(value)) {
			combo.setSelection(defaultSuffix);
		}
		new WaitUntil(new ControlIsEnabled(new OkButton()));
		new OkButton().click();
	}

}
