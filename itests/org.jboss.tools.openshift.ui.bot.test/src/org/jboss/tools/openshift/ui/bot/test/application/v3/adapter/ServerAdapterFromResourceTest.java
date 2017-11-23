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

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ServerHasState;
import org.eclipse.reddeer.eclipse.exception.EclipseLayerException;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.styledtext.DefaultStyledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.condition.ServerAdapterExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.jboss.tools.openshift.reddeer.wizard.importapp.ImportApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@OpenPerspective(value=JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@RequiredBasicConnection
@CleanConnection
@RequiredProject
@RequiredService(project=DatastoreOS3.TEST_PROJECT, service=OpenShiftResources.NODEJS_SERVICE, template=OpenShiftResources.NODEJS_TEMPLATE)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerAdapterFromResourceTest extends AbstractTest  {
	
	private static final Logger LOGGER = Logger.getLogger(ServerAdapterFromResourceTest.class);
	private static OpenShiftExplorerView explorer;
	private static OpenShiftProject project;
	private ServerAdapter adapter;
	
	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	@InjectRequirement
	private OpenShiftServiceRequirement serviceReq;
	
	@BeforeClass
	public static void importProject() {
		explorer = new OpenShiftExplorerView();
		explorer.open();
		project = explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(DatastoreOS3.TEST_PROJECT);
		project.getServicesWithName(OpenShiftResources.NODEJS_SERVICE).get(0).select();
		
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));
		
		ImportApplicationWizard appWizard = new ImportApplicationWizard();
		try {
			CheckBox checkBox = new CheckBox("Do not clone - use existing repository");
			if (checkBox.isEnabled()) {
				checkBox.toggle(true);
				LOGGER.debug("Using existing project, skipping import");
			}
		} catch (RedDeerException ex) {
			LOGGER.debug("No existing project found, importing");
		}
		
		try {
			appWizard.finish();
		} catch (WaitTimeoutExpiredException ex) {
			//When running test in suite, it needs to be selected correct build config(in OpenShift instance could be more build configs)
			appWizard.selectExistingBuildConfiguration(OpenShiftResources.NODEJS_APP_DEPLOYMENT_CONFIG);
			appWizard.finish();
		}
		
		new WaitUntil(new OpenShiftResourceExists(Resource.DEPLOYMENT, OpenShiftResources.NODEJS_APP_REPLICATION_CONTROLLER, ResourceState.UNSPECIFIED, DatastoreOS3.TEST_PROJECT, connectionReq.getConnection()), TimePeriod.LONG);
	}	
	
	@After
	public void removeAdapterIfExists() {
		try {
			OpenShiftUtils.killJobs();
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
		Server server = new ServersView2().getServer(serverName);
	
		try {
			new WaitUntil(new ServerHasState(server, ServerState.STARTED), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			throw new OpenshiftTestInFailureException("https://issues.jboss.org/browse/JBIDE-24241", ex);
		}
	}
	
	private void newAdapterFromResource(Resource type, String name) {
		project.getOpenShiftResource(type, name).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_ADAPTER_FROM_EXPLORER).select();
		
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
		new WaitWhile(new ShellIsAvailable(""));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
		
		assertTrue("OpenShift 3 server adapter was not created.", 
				new ServerAdapterExists(Version.OPENSHIFT3, name, resourceType).test());	
		adapter = new ServerAdapter(Version.OPENSHIFT3, name, resourceType);
	}
	
}
