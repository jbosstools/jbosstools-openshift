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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
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
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.condition.EditorIsDirty;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.exception.WorkbenchLayerException;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.requirements.CleanDockerExplorerRequirement.CleanDockerExplorer;
import org.jboss.tools.cdk.reddeer.requirements.DisableSecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK32ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK3ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.MinishiftServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.launch.configuration.CDKLaunchConfigurationDialog;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK3ServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewMinishiftServerWizardPage;
import org.jboss.tools.cdk.reddeer.ui.preferences.OpenShift3SSLCertificatePreferencePage;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.CDKAbstractTest;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
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
@OpenPerspective(value=JBossPerspective.class)
@CleanDockerExplorer
public abstract class CDKServerAdapterAbstractTest extends CDKAbstractTest {

	protected ServersView2 serversView;
	
	protected Server server;
	
	private static boolean certificateAccepted = false;
	
	public static final String OPENSHIFT_USERNAME = "developer"; 
	
	public static final String OPENSHIFT_ADMIN = "admin"; 
	
	public static final String MINISHIFT_PROFILE2 = "minishift2"; 
	
	private static final Logger log = Logger.getLogger(CDKServerAdapterAbstractTest.class);
	
	public Server getCDKServer() {
		return this.server;
	}

	protected ServersView2 getServersView() {
		return this.serversView;
	}

	protected void setServersView(ServersView2 view) {
		this.serversView = view;
	}

	protected void setCDKServer(Server server) {
		this.server = (CDKServer)server;
	}

	public static void setCertificateAccepted(boolean value) {
		CDKServerAdapterAbstractTest.certificateAccepted = value;
	}
	
	public static boolean getCertificateAccepted() {
		return CDKServerAdapterAbstractTest.certificateAccepted;
	}
	
	protected abstract String getServerAdapter();
	
	@BeforeClass
	public static void setupCDKServerAdapterAbstractTest() {
		log.info("Setting AUTOMATED_MODE of ErrorDialog to false, in order to pass some tests"); 
		// switch off errordialog.automated_mode to verify error dialog
		ErrorDialog.AUTOMATED_MODE = false;
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
	}
	
	@AfterClass
	public static void teardownCDKAbstractServerAdapterTest() {
		stopRunningServerAdapters();
		CDKUtils.deleteAllCDKServerAdapters();
		deleteCertificates();
		if (USERNAME != null) { 
			CDKTestUtils.removeAccessRedHatCredentials(CDKLabel.Others.CREDENTIALS_DOMAIN, USERNAME);
		}
		log.info("Setting AUTOMATED_MODE of ErrorDialog back to true"); 
		ErrorDialog.AUTOMATED_MODE = true;
	}
	
	@Before
	public void setupCDKServerAbstract() {
		log.info("Open Servers view tab"); 
		setServersView(new CDKServersView());
		getServersView().open();
		log.info("Getting server object from Servers View with name: " + getServerAdapter()); 
		setCDKServer(getServersView().getServer(getServerAdapter()));
		if (getCertificateAccepted()) {
			((CDKServer) getCDKServer()).setCertificateAccepted(true);
		}
		new WaitUntil(new JobIsRunning(), TimePeriod.DEFAULT, false);
	}
	
	@After
	public void tearDownServers() {
		// cleaning up different possible opened dialog after test failure
		// TODO: possibly will need to be implemented more specifically
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
		setCDKServer(null);
		getServersView().close();
	}
	
	/**
	 * Adds string containing proper flags/parameters into server adapter starting arguments
	 * @param paramsToAdd
	 */
	protected void addParamsToCDKLaunchConfig(Server server, String paramsToAdd) {
		server.open();
		(new MinishiftServerEditor(server.getLabel().getName())).openLaunchConfigurationFromLink();
		CDKLaunchConfigurationDialog launchConfig = new CDKLaunchConfigurationDialog();
		if (!launchConfig.getArguments().getText().contains(paramsToAdd)) {
			launchConfig.addArguments(paramsToAdd);
		} else {
			log.info(server.getLabel().getName() + " launch config already contains given arguments"); 
		}
		launchConfig.ok();
	} 
	
	protected void skipRegistrationViaFlag(Server server, boolean checked) {
		server.open();
		CDK32ServerEditor editor = new CDK32ServerEditor(server.getLabel().getName());
		editor.getAddSkipRegistrationOnStartCheckBox().toggle(checked);
		new WaitUntil(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.SHORT, false);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.DEFAULT, false);
		CDKTestUtils.performSave(editor.getEditorPart());
		editor.close();
	}
	
	protected void skipRegistration(Server server) {
		addParamsToCDKLaunchConfig(server, SKIP_REGISTRATION); 
	}
	
	protected void passCredentialsIntoEnvironment(boolean pass) {
		getCDKServer().open();
		CDK3ServerEditor editor = new CDK3ServerEditor(getCDKServer().getLabel().getName());
		editor.getPassCredentialsCheckBox().toggle(pass);
		new WaitUntil(new EditorIsDirty(editor), TimePeriod.MEDIUM, false);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM);
		try {
			editor.save();
		} catch (WorkbenchLayerException exc) {
			String message = exc.getMessage().toLowerCase();
			if (message.contains("save") && message.contains("not enabled")) {  
				log.info("There was nothing to save"); 
			} else {
				throw exc;
			}
		}
		editor.close(true);
	}
	
	/**
	 * Starts server adapter defined in getServerAdapter abstract method and
	 * checks server's state, method's parameter accepts lambda expression
	 * that expects or consumes method to be run before cdk is started.
	 * Was designed to set up server adapter launching arguments.
	 */

	protected void startServerAdapter(Runnable cond) {
		startServerAdapter(cond, false);
	}

	protected void startServerAdapter(Runnable cond, boolean rethrow) {
		startServerAdapter(getCDKServer(), cond, rethrow);
	}
	
	protected void startServerAdapter(Server server, Runnable cond, boolean rethrow) {
		log.info("Starting server adapter");
		new ServerOperation(() -> server.start(), cond, rethrow);
		printCertificates();
		setCertificateAccepted(true);
		checkServerIsAvailable();
		assertEquals(ServerState.STARTED, getCDKServer().getLabel().getState());
	}
	
	/**
	 * Starts server adapter only if not running yet
	 */
	public void startServerAdapterIfNotRunning(Runnable cond, boolean wait) {
		startServerAdapterIfNotRunning(getCDKServer(), cond, wait);
	}
	
	/**
	 * Starts server adapter only if not running yet
	 */
	public void startServerAdapterIfNotRunning(Server server, Runnable cond, boolean wait) {
		if (getCDKServer().getLabel().getState().equals(ServerState.STARTED)) {
			log.info("Server adapter " + getServerAdapter() + " is already started");  
		} else {
			log.info("Server adapter " + getServerAdapter() + " is not running, starting");  
			startServerAdapter(server, cond, wait);
		}
	}
	
	/**
	 * Restarts server adapter defined in getServerAdapter abstract method and
	 * checks server's state
	 */
	protected void restartServerAdapter() {
		restartServerAdapter(getCDKServer());
	}
	
	protected void restartServerAdapter(Server server) {
		log.info("Restarting server adapter"); 
		new ServerOperation(() -> server.restart(), () -> {});
		assertEquals(ServerState.STARTED, getCDKServer().getLabel().getState());		
	}
	
	/**
	 * Stops server adapter defined in getServerAdapter abstract method and
	 * checks server's state
	 */
	protected void stopServerAdapter() {
		stopServerAdapter(getCDKServer());
	}
	
	protected void stopServerAdapter(Server server) {
		log.info("Stopping server adapter"); 
		new ServerOperation(() -> server.stop(), () -> {});
		assertEquals(ServerState.STOPPED, getCDKServer().getLabel().getState());
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
				collectConsoleOutput(log, false);
			}
		}
	}
	
	/**
	 * Checks for given server adapter
	 */
	protected void checkServerIsAvailable() {
		if(!getCDKServer().getLabel().getName().contains(getServerAdapter())) {
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
        setCertificateAccepted(false);
	}

	/**
	 * Creates new Minishift server adapter via ServersView -> New -> Server
	 * 
	 * @param serverAdapter server adapter name
	 * @param hypervisor hypervisor to use
	 * @param path path to minishift binary file
	 * @param profile what profile to use
	 */
	public static void addNewMinishiftServer(String serverAdapter, String hypervisor, String path, String profile) {
		NewCDKServerWizard dialog = setupFirstNewServerWizardPage(CDKLabel.Server.MINISHIFT_SERVER_NAME, serverAdapter);
		
		// set second new server dialog page
		NewMinishiftServerWizardPage containerPage = new NewMinishiftServerWizardPage();
		if (!StringUtils.isEmptyOrNull(hypervisor)) {
			log.info("Setting hypervisor to " + hypervisor); 
			containerPage.setHypervisor(hypervisor);
		}
		log.info("Setting minishift binary file folder to " + path); 
		containerPage.setMinishiftBinary(path);
		log.info("Setting minishift profile to " + profile); 
		containerPage.setMinishiftProfile(profile);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.DEFAULT);
		log.info("Finishing Add new server dialog"); 
		if (!(new FinishButton().isEnabled())) {
			log.error("Finish button was not enabled"); 
		}
		dialog.finish(TimePeriod.MEDIUM);
	}
	
	/**
	 * Creates new CDK 3.2+ server adapter via ServersView -> New -> Server
	 * 
	 * @param serverAdapter server adapter name
	 * @param hypervisor hypervisor to use
	 * @param path path to minishift binary file
	 * @param profile what profile to use
	 * @param username redhat developers username
	 * @param password user password
	 */
	public static void addNewCDK32Server(String serverAdapter, String hypervisor, String path, String profile, String username, String password) {
		NewCDKServerWizard dialog = setupFirstNewServerWizardPage(CDKLabel.Server.CDK32_SERVER_NAME, serverAdapter);
		
		// set second new server dialog page
		NewCDK32ServerWizardPage containerPage = new NewCDK32ServerWizardPage();
		containerPage.setCredentials(username, password);
		if (hypervisor != null && !hypervisor.isEmpty()) {
			log.info("Setting hypervisor to " + hypervisor); 
			containerPage.setHypervisor(hypervisor);
		}
		log.info("Setting minishift binary file folder to " + path); 
		containerPage.setMinishiftBinary(path);
		if (!profile.isEmpty()) {
			log.info("Setting minishift profile to " + profile); 
			containerPage.setMinishiftProfile(profile);
		} else {
			log.info("Keeping minishift profile default: " + containerPage.getMinishiftProfile()); 
		}
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.DEFAULT);
		log.info("Finishing Add new server dialog"); 
		if (!(new FinishButton().isEnabled())) {
			log.error("Finish button was not enabled"); 
		}
		dialog.finish();
	}
	
	public static void addNewCDK32Server(String serverAdapter, 
			String hypervisor, String path, String profile) {
		addNewCDK32Server(serverAdapter, hypervisor, path, profile, USERNAME, PASSWORD);
	}
	
	/**
	 * Creates new CDK 3.x server adapter via ServersView -> New -> Server
	 * 
	 * @param serverAdapter server adapter name
	 * @param hypervisor hypervisor to use
	 * @param path path to minishift binary file
	 */
	public static void addNewCDK3Server(String serverAdapter, String hypervisor, String path) {
		NewCDKServerWizard dialog = setupFirstNewServerWizardPage(CDKLabel.Server.CDK3_SERVER_NAME, serverAdapter);
		
		// set second new server dialog page
		NewCDK3ServerWizardPage containerPage = new NewCDK3ServerWizardPage();
		containerPage.setCredentials(USERNAME, PASSWORD);
		if (hypervisor != null && !hypervisor.isEmpty()) {
			log.info("Setting hypervisor to " + hypervisor); 
			containerPage.setHypervisor(hypervisor);
		}
		log.info("Setting minishift binary file folder to " + path); 
		containerPage.setMinishiftBinary(path);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.DEFAULT);
		log.info("Finishing Add new server dialog"); 
		if (!(new FinishButton().isEnabled())) {
			log.error("Finish button was not enabled"); 
		}
		dialog.finish();
	}

	/**
	 * Creates new CDK 2.x server adapter via ServersView -> New -> Server
	 * 
	 * @param serverAdapter server adapter name
	 * @param path path to vagrantfile
	 */
	public static void addNewCDKServer(String serverAdapter, String path) {
		NewCDKServerWizard dialog = setupFirstNewServerWizardPage(CDKLabel.Server.CDK_SERVER_NAME, serverAdapter);
		
		// set second new server dialog page
		NewCDKServerWizardPage containerPage = new NewCDKServerWizardPage();
		containerPage.setCredentials(USERNAME, PASSWORD);
		// set cdk 2.x fields
		log.info("Setting vagrant file folder"); 
		containerPage.setFolder(path);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.MEDIUM);
		log.info("Finishing Add new server dialog"); 
		if (!(new FinishButton().isEnabled())) {
			log.error("Finish button was not enabled"); 
		}
		dialog.finish();
	}
	
	private static NewCDKServerWizard setupFirstNewServerWizardPage(String serverName, String serverAdapter) {
		NewCDKServerWizard dialog = (NewCDKServerWizard)CDKTestUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		// set first dialog page
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, serverName);
		page.setHostName(CDKLabel.Server.SERVER_HOST);
		page.setName(serverAdapter);
		dialog.next();	
		return dialog;
	}
	
	/**
	 * Tests Openshift 3 connection and try to refresh it
	 * 
	 * @param connection
	 */
	public void testOpenshiftConnection(String server, String username) {
		OpenShift3Connection connection = null;
		try {
			connection = findOpenShiftConnection(server, username);
		} catch (OpenShiftToolsException toolsExc) {
			fail(toolsExc.getMessage());
		}
		testOpenshiftConnection(connection);
	}
	
	/**
	 * Tests Openshift 3 connection and try to refresh it
	 * 
	 * @param connection
	 */
	public void testOpenshiftConnection(OpenShift3Connection connection) {
		log.info("Performing test of OS 3 connection: " + connection.getTreeItem().getText());
		// usually, when server adapter is not started, openshift connection after refresh should cause 
		// problem occurred dialog
		connection.refresh();
		try {
			new WaitUntil(new ShellIsAvailable(CDKLabel.Shell.PROBLEM_DIALOG), TimePeriod.getCustom(30)); 
			CDKUtils.captureScreenshot("CDKServerAdapterAbstractTest#testOpenshiftConnection"); 
			new DefaultShell(CDKLabel.Shell.PROBLEM_DIALOG).close(); 
			fail("Problem dialog occured when refreshing OpenShift connection"); 
		} catch (WaitTimeoutExpiredException ex) {
			// no dialog appeared, which is ok
			log.info("Expected WaitTimeoutExpiredException occured"); 
		}
	}
	
	/**
	 * Tests Docker connection
	 * 
	 * @param dockerDaemon name of docker connection
	 */
	public void testDockerConnection(String dockerDaemon) {
		log.info("Performing test of Docker Connection: " + dockerDaemon);
		DockerExplorerView dockerExplorer = new DockerExplorerView();
		dockerExplorer.open();
		DockerConnection connection =  dockerExplorer.getDockerConnectionByName(dockerDaemon);
		if (connection == null) {
			fail("Could not find Docker connection " + dockerDaemon); 
		}
		connection.select();
		connection.enableConnection();
		connection.refresh();
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT);
		try {
			assertTrue("Docker connection does not contain any images", connection.getImagesNames().size() > 0); 
		} catch (WaitTimeoutExpiredException ex) {
			ex.printStackTrace();
			fail("WaitTimeoutExpiredException occurs when expanding" 
					+ " Docker connection " + dockerDaemon); 
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
	public static void verifyConsoleContainsRegEx(String regex) {
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
	public static String collectConsoleOutput(Logger log, boolean onFail) {
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
		OpenShiftExplorerView osExplorer = new OpenShiftExplorerView();
		osExplorer.open();
		if (osExplorer.connectionExists(server, username)) {
			return osExplorer.getOpenShift3Connection(server, username);
		} else {
			throw new OpenShiftToolsException("Could not find OpenShift connection for " + 
					server + " and " + username);
		}
	}
	
	/**
	 * Returns lists of filtered docker connections names 
	 * @param view DockerExplorerView object
	 * @param name string name parameter to filter docker connections
	 * @return list of string of docker connections names fulfilling condition
	 */
	public List<String> getDockerConnectionCreatedByCDK(DockerExplorerView view, String name) {
		List<String> list = new ArrayList<>();
		for (String item : view.getDockerConnectionNames()) {
			if (item.contains(name)) {
				list.add(item);
			}
		}
		return list;
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
		 * 
		 * @param operation action to be called on server
		 * @param cond condition to be executed before operation action
		 * @param rethrow boolean param distinguishing whether re-throw caught exception
		 */
		public ServerOperation(Runnable operation, Runnable cond, boolean rethrow) {
			cond.run();
			try {
				operation.run();
			} catch (ServersViewException serversExc) {
				log.error(serversExc.getMessage());
				serversExc.printStackTrace();
			} catch (CDKServerException exc) {
				String cause = exc.getMessage() + "\r\n" + collectConsoleOutput(log, true); 
				if (rethrow) {
					throw new CDKServerException(cause);
				} else {
					fail(cause);
				}
			} catch (WaitTimeoutExpiredException waitExc) {
				String cause = waitExc.getMessage() + "\r\n" + collectConsoleOutput(log, true); 
				if (rethrow) {
					throw new WaitTimeoutExpiredException(cause);
				} else {
					fail(cause);
				}
			} 
			collectConsoleOutput(log, false);
		}
		
		/**
		 * Call given void method (operation) on server and catch possible problems
		 */
		public ServerOperation(Runnable operation, Runnable cond) {
			this(operation, cond, false);
		}
	}
}
