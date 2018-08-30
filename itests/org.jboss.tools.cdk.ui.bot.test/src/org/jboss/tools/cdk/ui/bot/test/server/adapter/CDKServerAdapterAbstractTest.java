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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.workbench.condition.EditorIsDirty;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.exception.WorkbenchLayerException;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.handlers.ServerOperationHandler;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.requirements.CleanDockerExplorerRequirement.CleanDockerExplorer;
import org.jboss.tools.cdk.reddeer.requirements.DisableSecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK32ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK3ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.MinishiftServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.launch.configuration.CDKLaunchConfigurationDialog;
import org.jboss.tools.cdk.reddeer.ui.preferences.OpenShift3SSLCertificatePreferencePage;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.CDKAbstractTest;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
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
		CDKUtils.deleteAllContainerRuntimeServerAdapters();
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
		// possibly will need to be implemented more specifically
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
		setCDKServer(null);
		getServersView().close();
	}
	
	public void startServerAdapter(Server server, Runnable cond, boolean rethrow) {
		log.info("Starting server adapter");
		try {
			ServerOperationHandler.getInstance().handleOperation(() -> server.start(), cond, rethrow);
		} catch (AssertionError err) {
			// prevent test to fail when everything seems to be working
			if (err.getMessage().contains("The VM may not have been registered successfully") &&
					err.getMessage().contains("The CDK VM is up and running")) {
				log.info("Expected assertion error due to JBIDE-26333, causes test to fail, though functionality should be ok.");
			} else {
				throw err;
			}
		}
		assertEquals(ServerState.STARTED, server.getLabel().getState());
		setCertificateAccepted(true);
	}
	
	/**
	 * Starts server adapter only if not running yet
	 */
	public void startServerAdapterIfNotRunning(Server server, Runnable cond, boolean wait) {
		if (server.getLabel().getState().equals(ServerState.STARTED)) {
			log.info("Server adapter " + getServerAdapter() + " is already started");  
		} else {
			log.info("Server adapter " + getServerAdapter() + " is not running, starting");  
			startServerAdapter(server, cond, wait);
		}
	}

	public void restartServerAdapter(Server server) {
		log.info("Restarting server adapter"); 
		try {
			ServerOperationHandler.getInstance().handleOperation(() -> server.restart(), () -> {});
		} catch (AssertionError err) {
			// prevent test to fail when everything seems to be working
			if (err.getMessage().contains("The VM may not have been registered successfully") &&
					err.getMessage().contains("The CDK VM is up and running")) {
				log.info("Expected assertion error due to JBIDE-26333, causes test to fail, though functionality should be ok.");
			} else {
				throw err;
			}
		}
		assertEquals(ServerState.STARTED, server.getLabel().getState());		
	}
	
	public void stopServerAdapter(Server server) {
		log.info("Stopping server adapter"); 
		try {
		ServerOperationHandler.getInstance().handleOperation(() -> server.stop(), () -> {});
		} catch (AssertionError err) {
			// prevent test to fail when everything seems to be working
			if (err.getMessage().contains("The VM may not have been registered successfully") &&
					err.getMessage().contains("The CDK VM is up and running")) {
				log.info("Expected assertion error due to JBIDE-26333, causes test to fail, though functionality should be ok.");
			} else {
				throw err;
			}
		}
		assertEquals(ServerState.STOPPED, server.getLabel().getState());
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
				CDKUtils.collectConsoleOutput(log, false);
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
	 * Deletes all Openshift 3 SSL certificates that were accepted from
	 * Preferences -> JBossTools -> OpenShift 3 -> SSL Certificates
	 */
	private static void deleteCertificates() {
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
	

}
