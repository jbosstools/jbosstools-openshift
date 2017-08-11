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
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.condition.ServerHasState;
import org.jboss.reddeer.eclipse.exception.EclipseLayerException;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.eclipse.wst.server.ui.view.Server;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersView;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersViewEnums.ServerState;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.styledtext.DefaultStyledText;
import org.jboss.reddeer.swt.impl.tree.DefaultTreeItem;
import org.jboss.tools.openshift.reddeer.condition.ServerAdapterExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@OCBinary
@RequiredBasicConnection
@RequiredProject
@RequiredService(project=DatastoreOS3.TEST_PROJECT, service=OpenShiftResources.NODEJS_SERVICE, template=OpenShiftResources.NODEJS_TEMPLATE)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerAdapterFromResourceTest {
	
	private static final Logger LOGGER = Logger.getLogger(ServerAdapterFromResourceTest.class);
	private static OpenShiftExplorerView explorer;
	private static OpenShiftProject project;
	private ServerAdapter adapter;
	
	@InjectRequirement
	private OpenShiftServiceRequirement serviceReq;
	
	@BeforeClass
	public static void importProject() {
		explorer = new OpenShiftExplorerView();
		explorer.open();
		project = explorer.getOpenShift3Connection().getProject(DatastoreOS3.TEST_PROJECT);
		project.getService(OpenShiftResources.NODEJS_SERVICE).select();
		
		new ContextMenu(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));
		
		try {
			CheckBox checkBox = new CheckBox("Do not clone - use existing repository");
			if (checkBox.isEnabled()) {
				checkBox.toggle(true);
				LOGGER.debug("Using existing project, skipping import");
			}
		} catch (RedDeerException ex) {
			LOGGER.debug("No existing project found, importing");
		}
			
		new FinishButton().click();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	@After
	public void removeAdapterIfExists() {
		try {
			new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
			adapter.delete();
		} catch (OpenShiftToolsException | NullPointerException ex) {
			LOGGER.debug("Server adapter does not exist, cannot delete");
		}
	}
	
	@AfterClass
	public static void cleanup() {
		try {
			new ProjectExplorer().getProject(OpenShiftResources.NODEJS_GIT_NAME).delete(true);
		} catch (EclipseLayerException ex) {
			LOGGER.warn("Unable to delete project " + OpenShiftResources.NODEJS_GIT_NAME);
			LOGGER.warn(ex.toString());
		} finally {
			try {
				explorer.open();
				project.delete();
			} catch (RedDeerException | NullPointerException e) {
				LOGGER.warn("Unable to delete openshift project " + DatastoreOS3.TEST_PROJECT);
				LOGGER.warn(e.toString());
			}
		}
	}
	
	@Test
	public void testAdapterFromAService() {
		newAdapterFromResource(Resource.SERVICE, serviceReq.getService().getName());		
		assertAdapterWorks();
	}
	
	@Test
	public void testAdapterFromBDeploymentConfig() {
		project.getOpenShiftResource(Resource.SERVICE, serviceReq.getService().getName()).delete();

		newAdapterFromResource(Resource.DEPLOYMENT_CONFIG, serviceReq.getService().getName());
		assertAdapterWorks();
	}
	
	@Test
	public void testAdapterFromCReplicationController() {
		project.getOpenShiftResource(Resource.DEPLOYMENT_CONFIG, serviceReq.getService().getName()).delete();
		
		newAdapterFromResource(Resource.DEPLOYMENT, OpenShiftResources.NODEJS_APP_REPLICATION_CONTROLLER);
		assertAdapterWorks();
	}
	
	private void assertAdapterWorks() {
		String serverName = adapter.getLabel();
		Server server = new ServersView().getServer(serverName);
	
		try {
			new WaitUntil(new ServerHasState(server, ServerState.STARTED), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			throw new OpenshiftTestInFailureException("https://issues.jboss.org/browse/JBIDE-24241", ex);
		}
	}
	
	private void newAdapterFromResource(Resource type, String name) {
		project.getOpenShiftResource(type, name).select();
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_ADAPTER_FROM_EXPLORER).select();
		
		new DefaultShell(OpenShiftLabel.Shell.SERVER_ADAPTER_SETTINGS);
		
		String resourceType = null;
		String selectors = name + " name=" + name;
		
		if(type.equals(Resource.DEPLOYMENT)) {
			resourceType = "ReplicationController";
			selectors = name + " deploymentconfig=" + serviceReq.getService().getName() + ", name="
					+ serviceReq.getService().getName() + ", deployment=" + name;
		} else {
			resourceType = type.toString().replaceFirst(".$","").replaceAll(" ", "");
		}		
		assertTrue("Resource should be preselected for new OpenShift 3 server adapter",
				new DefaultTreeItem(project.getName(), selectors).isSelected());
		
		try {
			new DefaultStyledText(resourceType);
		} catch (RedDeerException e) {
			fail("Resource type does not match");
		}
		
		new FinishButton().click();
		new WaitWhile(new ShellWithTextIsAvailable(""));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
		
		assertTrue("OpenShift 3 server adapter was not created.", 
				new ServerAdapterExists(Version.OPENSHIFT3, name, resourceType).test());	
		adapter = new ServerAdapter(Version.OPENSHIFT3, name, resourceType);
	}
}
