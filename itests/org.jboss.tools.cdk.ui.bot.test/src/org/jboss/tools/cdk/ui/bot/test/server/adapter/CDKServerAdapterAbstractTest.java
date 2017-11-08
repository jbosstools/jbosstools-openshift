/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewException;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.jface.exception.JFaceLayerException;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.preferences.OpenShift3SSLCertificatePreferencePage;
import org.jboss.tools.cdk.reddeer.requirements.DisableSecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServersView;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerContainerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK3ServerContainerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerContainerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.CDKAbstractTest;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Abstract class working with CDK server adapter
 * @author odockal
 *
 */
@DisableSecureStorage
@RemoveCDKServers
public abstract class CDKServerAdapterAbstractTest extends CDKAbstractTest {

	protected ServersView2 serversView;
	
	protected Server server;
	
	public static final String OPENSHIFT_USERNAME = "developer"; //$NON-NLS-1$
	
	private static final Logger log = Logger.getLogger(CDKServerAdapterAbstractTest.class);
	
	public Server getCDEServer() {
		return this.server;
	}

	protected ServersView2 getServersView() {
		return this.serversView;
	}

	protected void setServersView(ServersView2 view) {
		this.serversView = view;
	}

	protected void setCDEServer(Server server) {
		this.server = (CDEServer)server;
	}
	
	protected abstract String getServerAdapter();
	
	@BeforeClass
	public static void setUpEnvironemnt() {
		log.info("Checking given program arguments"); //$NON-NLS-1$
		checkDevelopersParameters();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
		//CDKTestUtils.deleteAllCDEServers(SERVER_ADAPTER);
	}
	
	@AfterClass
	public static void tearDownEnvironment() {
		CDKTestUtils.removeAccessRedHatCredentials(CREDENTIALS_DOMAIN, USERNAME);
	}
	
	@Before
	public void setUpServers() {
		log.info("Open Servers view tab"); //$NON-NLS-1$
		setServersView(new CDEServersView());
		getServersView().open();
		log.info("Getting server object from Servers View with name: " + getServerAdapter()); //$NON-NLS-1$
		setCDEServer(getServersView().getServer(getServerAdapter()));
		new WaitUntil(new JobIsRunning(), TimePeriod.DEFAULT, false);
	}
	
	@After
	public void tearDownServers() {
		if (getCDEServer().getLabel().getState() == ServerState.STARTED) {
			stopServerAdapter();
		}
		// remove SSL Certificate to be added at next server start at method annotated with before
		deleteCertificates();
		CDKUtils.deleteAllCDKServerAdapters();
		setCDEServer(null);
		getServersView().close();
	}
	
	/**
	 * Starts server adapter defined in getServerAdapter abstract method and
	 * checks server's state
	 */
	protected void startServerAdapter() {
		log.info("Starting server adapter"); //$NON-NLS-1$
		try {
			getCDEServer().start();
		} catch (ServersViewException serversExc) {
			log.error(serversExc.getMessage());
			serversExc.printStackTrace();
		} catch (CDKServerException exc) {
			String console = collectConsoleOutput(log, true);
			fail(exc.getMessage() + "\r\n" + console);
		}
		printCertificates();
		checkServerIsAvailable();
		assertEquals(ServerState.STARTED, getCDEServer().getLabel().getState());
	}
	
	/**
	 * Restarts server adapter defined in getServerAdapter abstract method and
	 * checks server's state
	 */
	protected void restartServerAdapter() {
		log.info("Restarting server adapter"); //$NON-NLS-1$
		try {
			getCDEServer().restart();
		} catch (ServersViewException serversExc) {
			log.error(serversExc.getMessage());
			serversExc.printStackTrace();
		} catch (CDKServerException exc) {
			String console = collectConsoleOutput(log, true);
			fail(exc.getMessage() + "\r\n" + console);
		}
		checkServerIsAvailable();
		assertEquals(ServerState.STARTED, getCDEServer().getLabel().getState());
	}
	
	/**
	 * Stops server adapter defined in getServerAdapter abstract method and
	 * checks server's state
	 */
	protected void stopServerAdapter() {
		log.info("Stopping server adapter"); //$NON-NLS-1$
		try {
			getCDEServer().stop();
		} catch (ServersViewException serversExc) {
			log.error(serversExc.getMessage());
			serversExc.printStackTrace();
		} catch (CDKServerException exc) {
			String console = collectConsoleOutput(log, true);
			fail(exc.getMessage() + "\r\n" + console);
		}
		checkServerIsAvailable();
		assertEquals(ServerState.STOPPED, getCDEServer().getLabel().getState());
	}
	
	/**
	 * Checks for given server adapter
	 */
	protected void checkServerIsAvailable() {
		if(!getCDEServer().getLabel().getName().contains(getServerAdapter())) {
			log.info("List of available servers: ");
			for (Server serverItem : getServersView().getServers()) {
				String serverName = serverItem.getLabel().getName();
				log.info(serverName);
			}
			fail("Required server " + getServerAdapter() + " is not available");
		}
	}
	
	/**
	 * Prints out Openshift 3 accepted certificates
	 */
	protected static void printCertificates() {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		OpenShift3SSLCertificatePreferencePage preferencePage = new OpenShift3SSLCertificatePreferencePage(dialog);
		dialog.select(preferencePage);
		preferencePage.printCertificates();
        dialog.ok();
	}
	
	/**
	 * Deletes all Openshift 3 SSL certificates that were accepted from
	 * Preferences -> JBossTools -> OpenShift 3 -> SSL Certificates
	 */
	protected static void deleteCertificates() {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		OpenShift3SSLCertificatePreferencePage preferencePage = new OpenShift3SSLCertificatePreferencePage(dialog);
		dialog.select(preferencePage);
		preferencePage.deleteAll();
		preferencePage.apply();
        dialog.ok();
	}

	/**
	 * Creates new CDK 3.2+ server adapter via ServersView -> New -> Server
	 * 
	 * @param serverName the name of the Server
	 * @param serverAdapter server adapter name
	 * @param hypervisor hypervisor to use
	 * @param path path to minishift binary file
	 * @param profile what profile to use
	 */
	public static void addNewCDK32Server(String serverName, String serverAdapter, String hypervisor, String path, String profile) {
		NewCDKServerWizard dialog = (NewCDKServerWizard)CDKTestUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		// set first dialog page
		page.selectType(SERVER_TYPE_GROUP, serverName);
		page.setHostName(SERVER_HOST);
		page.setName(serverAdapter);
		dialog.next();
		
		// set second new server dialog page
		NewCDK32ServerContainerWizardPage containerPage = new NewCDK32ServerContainerWizardPage();
		containerPage.setCredentials(USERNAME, PASSWORD);
		if (hypervisor != null && !hypervisor.isEmpty()) {
			log.info("Setting hypervisor to " + hypervisor); //$NON-NLS-1$
			containerPage.setHypervisor(hypervisor);
		}
		log.info("Setting minishift binary file folder to " + path); //$NON-NLS-1$
		containerPage.setMinishiftBinary(path);
		log.info("Setting minishift profile to " + profile); //$NON-NLS-1$
		containerPage.setMinishiftProfile(profile);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.DEFAULT);
		log.info("Finishing Add new server dialog"); //$NON-NLS-1$
		if (!(new FinishButton().isEnabled())) {
			log.error("Finish button was not enabled"); //$NON-NLS-1$
		}
		dialog.finish(TimePeriod.MEDIUM);
	}
	
	/**
	 * Creates new CDK 3.x server adapter via ServersView -> New -> Server
	 * 
	 * @param serverName the name of the Server
	 * @param serverAdapter server adapter name
	 * @param hypervisor hypervisor to use
	 * @param path path to minishift binary file
	 */
	public static void addNewCDK3Server(String serverName, String serverAdapter, String hypervisor, String path) {
		NewCDKServerWizard dialog = (NewCDKServerWizard)CDKTestUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		// set first dialog page
		page.selectType(SERVER_TYPE_GROUP, serverName);
		page.setHostName(SERVER_HOST);
		page.setName(serverAdapter);
		dialog.next();
		
		// set second new server dialog page
		NewCDK3ServerContainerWizardPage containerPage = new NewCDK3ServerContainerWizardPage();
		containerPage.setCredentials(USERNAME, PASSWORD);
		if (hypervisor != null && !hypervisor.isEmpty()) {
			log.info("Setting hypervisor to " + hypervisor); //$NON-NLS-1$
			containerPage.setHypervisor(hypervisor);
		}
		log.info("Setting minishift binary file folder to " + path); //$NON-NLS-1$
		containerPage.setMinishiftBinary(path);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.DEFAULT);
		log.info("Finishing Add new server dialog"); //$NON-NLS-1$
		if (!(new FinishButton().isEnabled())) {
			log.error("Finish button was not enabled"); //$NON-NLS-1$
		}
		dialog.finish(TimePeriod.MEDIUM);
	}

	/**
	 * Creates new CDK 2.x server adapter via ServersView -> New -> Server
	 * 
	 * @param serverName the name of the Server
	 * @param serverAdapter server adapter name
	 * @param path path to vagrantfile
	 */
	public static void addNewCDKServer(String serverName, String serverAdapter, String path) {
		NewCDKServerWizard dialog = (NewCDKServerWizard)CDKTestUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		// set first dialog page
		page.selectType(SERVER_TYPE_GROUP, serverName);
		page.setHostName(SERVER_HOST);
		page.setName(serverAdapter);
		dialog.next();
		
		// set second new server dialog page
		NewCDKServerContainerWizardPage containerPage = new NewCDKServerContainerWizardPage();
		containerPage.setCredentials(CDKServerAdapterAbstractTest.USERNAME, PASSWORD);
		// set cdk 2.x fields
		log.info("Setting vagrant file folder"); //$NON-NLS-1$
		containerPage.setFolder(path);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.MEDIUM);
		log.info("Finishing Add new server dialog"); //$NON-NLS-1$
		if (!(new FinishButton().isEnabled())) {
			log.error("Finish button was not enabled"); //$NON-NLS-1$
		}
		dialog.finish(TimePeriod.MEDIUM);
	}
	
	/**
	 * Tests Openshift 3 connection and try to refresh it
	 * 
	 * @param connection
	 */
	public void testOpenshiftConncetion(OpenShift3Connection connection) {
		// usually, when server adapter is not started, openshift connection after refresh should cause 
		// problem occured dialog
		connection.refresh();
		try {
			new WaitUntil(new ShellIsAvailable("Problem occurred"), TimePeriod.getCustom(30)); //$NON-NLS-1$
			fail("Problem dialog occured when refreshing OpenShift connection"); //$NON-NLS-1$
		} catch (WaitTimeoutExpiredException ex) {
			// no dialog appeared, which is ok
			log.debug("Expected WaitTimeoutExpiredException occured"); //$NON-NLS-1$
			ex.printStackTrace();
		}
	}
	
	/**
	 * Tests Docker connection
	 * 
	 * @param dockerDaemon name of docker connection
	 */
	public void testDockerConnection(String dockerDaemon) {
		DockerExplorerView dockerExplorer = new DockerExplorerView();
		dockerExplorer.open();
		DockerConnection connection =  dockerExplorer.getDockerConnectionByName(dockerDaemon);
		if (connection == null) {
			fail("Could not find Docker connection " + dockerDaemon); //$NON-NLS-1$
		}
		connection.select();
		connection.enableConnection();
		connection.refresh();
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT);
		try {
			assertTrue("Docker connection does not contain any images", connection.getImagesNames().size() > 0); //$NON-NLS-1$
		} catch (WaitTimeoutExpiredException ex) {
			ex.printStackTrace();
			fail("WaitTimeoutExpiredException occurs when expanding" //$NON-NLS-1$
					+ " Docker connection " + dockerDaemon); //$NON-NLS-1$
		} catch (JFaceLayerException jFaceExc) {
			jFaceExc.printStackTrace();
			fail(jFaceExc.getMessage());
		}
		dockerExplorer.close();
	}
	
	/**
	 * Prints out console output via logger and returns console content
	 * 
	 * @param log reference to logger
	 * @param onFail if true then error log method is used, debug otherwise
	 * @return returns console output
	 */
	public String collectConsoleOutput(Logger log, boolean onFail) {
		ConsoleView view = new ConsoleView();
		view.open();
		
		new WaitWhile(new ConsoleHasNoChange(), TimePeriod.DEFAULT, false);
		String consoleOutput = view.getConsoleLabel() + "\n\r" + view.getConsoleText();
		if (onFail) {
			log.error(consoleOutput);
		} else {
			log.debug(consoleOutput);
		}
		return consoleOutput;
	}
	
	/**
	 * Finds OpenShift 3 connection based on server username parameters
	 * 
	 * @param server server name
	 * @param username connection username
	 * @return openshift 3 connection object if found
	 */
	public OpenShift3Connection findOpenShiftConnection(String server, String username) {
		try {
			OpenShiftExplorerView osExplorer = new OpenShiftExplorerView();
			osExplorer.open();
			return osExplorer.getOpenShift3Connection(server, username);
		} catch (RedDeerException ex) {
				ex.printStackTrace();
				throw new OpenShiftToolsException("Could not find OpenShift connection for " +
						server + " and " + username + //$NON-NLS-1$
						", ended with exception: " + ex.getMessage()); //$NON-NLS-1$
		}
	}
	
}
