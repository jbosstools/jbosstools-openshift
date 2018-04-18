/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.server.adapter;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test class for multiple server adapter profiles running at once
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CDK32ServerAdapterProfilesTest extends CDKServerAdapterAbstractTest {

	private Server secondServer;
	
	private OpenShiftExplorerView view;
	
	private DockerExplorerView dockerView;
	
	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER_32;
	
	private static final Logger log = Logger.getLogger(CDK32ServerAdapterProfilesTest.class);
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	protected static String getSecondServerAdapter() {
		return SERVER_ADAPTER_32 + " " + MINISHIFT_PROFILE2;
	}
	
	protected void setSecondCDKServer(Server server) {
		this.secondServer = (CDKServer) server;
	}
	
	protected Server getSecondCDKServer() {
		return this.secondServer;
	}
	
	@BeforeClass
	public static void setupServerAdapterWithMultipleProfile() {
		checkCDK32Parameters();
		addNewCDK32Server(SERVER_ADAPTER_32, MINISHIFT_HYPERVISOR, CDK32_MINISHIFT, MINISHIFT_PROFILE);
		addNewCDK32Server(getSecondServerAdapter(), MINISHIFT_HYPERVISOR, CDK32_MINISHIFT, MINISHIFT_PROFILE2);
	}
	
	@Before
	public void setupSecondAdapter() {
		log.info("Open Servers view tab");
		setServersView(new CDKServersView());
		getServersView().open();
		log.info("Getting server object from Servers View with name: " + getSecondServerAdapter());
		setSecondCDKServer(getServersView().getServer(getSecondServerAdapter()));
		new WaitUntil(new JobIsRunning(), TimePeriod.DEFAULT, false);
		view = new OpenShiftExplorerView();
		dockerView = new DockerExplorerView();
	}

	@Test
	public void testCDK32ServerAdapterWithMultipleProfiles() {
		// fisrt adapter start verification
		startServerAdapter(() -> { skipRegistration(getCDKServer());}, false);
		int conCount = view.getOpenShift3Connections().size();
		int docCount = getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION).size();
		assertEquals("Expected only one OS connection, got " + conCount, 1, conCount);
		assertEquals("Expected only one Docker connection, got " + docCount, 1, docCount);
		// second adapter start verification
		startServerAdapter(getSecondCDKServer(), 
				() -> { skipRegistration(getSecondCDKServer());
				}, false);
		// check counts of connections
		conCount = view.getOpenShift3Connections().size();
		docCount = getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION).size();
		assertEquals("Expected two OS connections, got " + conCount, 2, conCount);
		assertEquals("Expectedtwo Docker connections, got " + docCount, 2, docCount);
		// check functionality of connections
		for (String docker : getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION)) {
			testDockerConnection(docker);
		}
		for (OpenShift3Connection conn : view.getOpenShift3Connections()) {
			testOpenshiftConnection(conn);
		}
		// adapters stopping verification
		stopServerAdapter();
		stopServerAdapter(getSecondCDKServer());
	}

}
