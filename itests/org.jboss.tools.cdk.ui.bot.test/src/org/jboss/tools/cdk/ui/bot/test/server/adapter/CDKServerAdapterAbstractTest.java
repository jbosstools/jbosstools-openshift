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

import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
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
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.core.ServerOperable;
import org.jboss.tools.cdk.reddeer.preferences.OpenShift3SSLCertificatePreferencePage;
import org.jboss.tools.cdk.reddeer.requirements.DisableSecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDKServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.launch.configuration.CDKLaunchConfigurationDialog;
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
		this.server = (CDKServer)server;
	}
	
	protected abstract String getServerAdapter();
	
	@BeforeClass
	public static void setUpEnvironemnt() {
		log.info("Setting AUTOMATED_MODE of ErrorDialog to false, in order to pass some tests");
		// switch off errordialog.automated_mode to verify error dialog
		ErrorDialog.AUTOMATED_MODE = false;
		log.info("Checking given program arguments"); //$NON-NLS-1$
		checkDevelopersParameters();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
	}
	
	@AfterClass
	public static void tearDownEnvironment() {
		CDKUtils.deleteAllCDKServerAdapters();
		deleteCertificates();
		CDKTestUtils.removeAccessRedHatCredentials(CREDENTIALS_DOMAIN, USERNAME);
		log.info("Setting AUTOMATED_MODE of ErrorDialog back to true");
		ErrorDialog.AUTOMATED_MODE = true;
	}
	
	@Before
	public void setUpServers() {
		log.info("Open Servers view tab"); //$NON-NLS-1$
		setServersView(new CDKServersView());
		getServersView().open();
		log.info("Getting server object from Servers View with name: " + getServerAdapter()); //$NON-NLS-1$
		setCDEServer(getServersView().getServer(getServerAdapter()));
		new WaitUntil(new JobIsRunning(), TimePeriod.DEFAULT, false);
	}
	
	@After
	public void tearDownServers() {
		setCDEServer(null);
		getServersView().close();
	}
	
	/**
	 * Adds string containing proper flags/parameters into server adapter starting arguments
	 * @param paramsToAdd
	 */
	protected void addParamsToCDKLaunchConfig(Server server, String paramsToAdd) {
		server.open();
		(new CDKServerEditor(server.getLabel().getName())).openLaunchConfigurationFromLink();
		CDKLaunchConfigurationDialog launchConfig = new CDKLaunchConfigurationDialog();
		if (!launchConfig.getArguments().getText().contains(paramsToAdd)) {
			launchConfig.addArguments(paramsToAdd);
		} else {
			log.info(server.getLabel().getName() + " launch config already contains given arguments");
		}
		launchConfig.ok();
	} 
	
	protected void skipRegistration(Server server) {
		addParamsToCDKLaunchConfig(server, "--skip-registration");
	}
	
	/**
	 * Starts server adapter defined in getServerAdapter abstract method and
	 * checks server's state, method's parameter accepts lambda expression
	 * that expects or consumes method to be run before cdk is started.
	 * Was designed to set up server adapter launching arguments.
	 */
	protected void startServerAdapter(ServerOperable cond) {
		log.info("Starting server adapter"); //$NON-NLS-1$
		// Workaround for CDK-216
		new WaitUntil(new NeverFulfilledCondition(), TimePeriod.getCustom(90), false);
		new ServerOperation(() -> getCDEServer().start(), cond);
		printCertificates();
		checkServerIsAvailable();
		assertEquals(ServerState.STARTED, getCDEServer().getLabel().getState());
	}
	
	/**
	 * Starts server adapter only if not running yet
	 */
	public void startServerAdapterIfNotRunning(ServerOperable cond) {
		if (getCDEServer().getLabel().getState().equals(ServerState.STARTED)) {
			log.info("Server adapter " + getServerAdapter() + " is already started");
		} else {
			log.info("Server adapter " + getServerAdapter() + " is not running, starting");
			startServerAdapter(cond);
		}
	}
	
	/**
	 * Restarts server adapter defined in getServerAdapter abstract method and
	 * checks server's state
	 */
	protected void restartServerAdapter() {
		log.info("Restarting server adapter"); //$NON-NLS-1$
		new ServerOperation(() -> getCDEServer().restart(), () -> {});
		assertEquals(ServerState.STARTED, getCDEServer().getLabel().getState());
	}
	
	/**
	 * Stops server adapter defined in getServerAdapter abstract method and
	 * checks server's state
	 */
	protected void stopServerAdapter() {
		log.info("Stopping server adapter"); //$NON-NLS-1$
		new ServerOperation(() -> getCDEServer().stop(), () -> {});
		assertEquals(ServerState.STOPPED, getCDEServer().getLabel().getState());
	}
	
	/**
	 * static method for server adapters stopping in after class method
	 */
	public static void stopRunningServerAdapters() {
		log.info("Cleaning up resources, stopping server adapter");
		ServersView2 view = new ServersView2();
		if (!view.isOpen()) {
			view.open();
		}
		for (Server server : view.getServers()) {
			log.info("Checking server " + server.getLabel().getName() +  " state: " + server.getLabel().getState());
			if (server.getLabel().getState().equals(ServerState.STARTED)) {
				log.info("Stopping server");
				server.stop();
			}
		}
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
	public static void deleteCertificates() {
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
			CDKUtils.captureScreenshot("CDKServerAdapterAbstractTest#testOpenshiftConnection");
			new DefaultShell("Problem occurred").close();
			fail("Problem dialog occured when refreshing OpenShift connection"); //$NON-NLS-1$
		} catch (WaitTimeoutExpiredException ex) {
			// no dialog appeared, which is ok
			log.info("Expected WaitTimeoutExpiredException occured"); //$NON-NLS-1$
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
	 * Verification of successful registration of rhel image during cdk start up
	 * Checks for reg. expressions in console
	 */
	public void verifyConsoleContainsRegEx(String regex) {
		ConsoleView view = new ConsoleView();
		view.open();
		String consoleText = view.getConsoleText();
		Pattern pattern = Pattern.compile(regex);
		assertTrue("Console text does not contains regex: \r\n" + regex +
				"\r\nConsole text:\r\n" + consoleText,
				pattern.matcher(consoleText).find());
	}
	
	/**
	 * Prints out console output via logger and returns console content
	 * 
	 * @param log reference to logger
	 * @param onFail if true then error log method is used, debug otherwise
	 * @return returns console output
	 */
	public String collectConsoleOutput(Logger log, boolean onFail) {
		String consoleOutput = "Console is empty...";
		ConsoleView view = new ConsoleView();
		view.open();
		
		if (view.getConsoleText() != null) {
			new WaitWhile(new ConsoleHasNoChange(), TimePeriod.DEFAULT, false);
			consoleOutput = view.getConsoleLabel() + "\n\r" + view.getConsoleText();
			if (onFail) {
				log.info("Loggin console, called from test failure");
				log.error(consoleOutput);
			} else {
				log.info("Logging console for debugging purposes");
				log.debug(consoleOutput);
			}
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
	
	/**
	 * WaitCondition that is never fulfilled, to be used in WaitUntil to reach specific time treshold.
	 * @author odockal
	 *
	 */
	@SuppressWarnings("unused")
	private class NeverFulfilledCondition extends AbstractWaitCondition {

		@Override
		public boolean test() {
			return false;
		}

	}
	
	/**
	 * Inner class representing server's operation to be called using lambda expression
	 */
	private class ServerOperation {
		
		/**
		 * Call given void method (operation) on server and catch possible problems
		 */
		public ServerOperation(ServerOperable operation, ServerOperable cond) {
			cond.operate();
			try {
				operation.operate();
			} catch (ServersViewException serversExc) {
				log.error(serversExc.getMessage());
				serversExc.printStackTrace();
			} catch (CDKServerException exc) {
				String console = collectConsoleOutput(log, true);
				fail(exc.getMessage() + "\r\n" + console);
			} catch (WaitTimeoutExpiredException waitExc) {
				String console = collectConsoleOutput(log, true);
				fail(waitExc.getMessage() + "\r\n" + console);
			} 
			collectConsoleOutput(log, false);
		}
	}
}
