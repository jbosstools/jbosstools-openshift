/******************************************************************************* 
 * Copyright (c) 2018-2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.requirements;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.handlers.ServerOperationHandler;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.core.server.ServerAdapter;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.server.exception.CDKException;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK3ServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewMinishiftServerWizardPage;
import org.jboss.tools.cdk.reddeer.ui.preferences.OpenShift3SSLCertificatePreferencePage;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.reddeer.utils.DownloadCDKRuntimesUtility;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * Container runtime requirement class providing cdk/minishift binary and base
 * functionality and configuration
 * 
 * @author odockal
 *
 */
public class ContainerRuntimeServerRequirement implements Requirement<ContainerRuntimeServer> {

	private ContainerRuntimeServer config;
	private ServerAdapter adapter;

	public static final Path INSTALLATION_DIRECTORY = Paths.get(System.getProperty("user.dir"), "requirement_runtimes");
	public static final Path DOWNLOAD_DIRECTORY = Paths.get(INSTALLATION_DIRECTORY.toString(), "tmp");
	public static final Path MINISHIFT_HOME_DIRECTORY = Paths.get(System.getProperty("user.home"), ".minishift");
	public static final Path RESOURCES_DIRECTORY = Paths.get(System.getProperty("user.dir"), "resources");
	public static final Path PROPERTIES_FILE = Paths.get(RESOURCES_DIRECTORY.toString(), "containers.properties");

	private static final Logger log = Logger.getLogger(ContainerRuntimeServerRequirement.class);

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ContainerRuntimeServer {
		/**
		 * CDKVersion object to download
		 * 
		 * @return CDKversion object
		 */
		CDKVersion version();

		/**
		 * Adapter's name, default is empty string which will use default value
		 * 
		 * @return string representation of adapter's name
		 */
		String adapterName() default "";

		/**
		 * Adapter profile's name, default is empty string which will use default value
		 * 
		 * @return string representation of adapter's profile
		 */
		String profile() default "";

		/**
		 * Hypervisor used for container runtime server adapter
		 * 
		 * @return CDKHypervisor value, default is empty one
		 */
		String hypervisorProperty() default "hypervisor";

		// TODO: ContainerRuntimeServerRequirementState state() default PRESENT;
		// TODO: boolean stopOnFinish() default false;

		/**
		 * Decides whether to delete server adapter from Servers view
		 * 
		 * @return if deletes adapter or not, by default it is deleted
		 */
		boolean deleteAdapterOnFinish() default true;

		/**
		 * Decides whether to keep all downloaded artifacts and store its information in
		 * properties file
		 * 
		 * @return false is default value and then no artifacts will be stored on
		 *         filesystem, true value for storing them
		 */
		boolean makeRuntimePersistent() default false;

		/**
		 * Decides if server adapter will be created during fulfill part of requirement.
		 * @return true by default
		 */
		boolean createServerAdapter() default true;

		/**
		 * Decides if first try to use already downloaded and saved binary via config.properties file, if such exists.
		 * @return true by default
		 */
		boolean useExistingBinaryFromConfig() default true;
		
		/**
		 * Use user name defined in sys. env. property. Empty by default.
		 */
		String usernameProperty() default "";

		/**
		 * Use password defined in sys. env. property. Empty by default.
		 */
		String passwordProperty() default "";
		
		/**
		 * Has priority over {@link #useExistingBinaryFromConfig()} 
		 * and allows requirement to use binary downloaded outside the running application.
		 * Cannot be used by persistent config. properties file nor can be deleted in cleanup, 
		 * this implies that this option overrides {@link #makeRuntimePersistent()}.
		 * @return empty string by default or string representation of sys. env. property.
		 */
		String useExistingBinaryInProperty() default "";
	}

	// Requirement required methods

	@Override
	public void fulfill() {
		// if already downloaded cdk is at place, use that one
		adapter = new ServerAdapter(config.version(), config.adapterName());
		WizardDialog dialog = setupNewWizardDialog(adapter);
		NewCDK3ServerWizardPage wizardPage = CDKUtils.chooseCDKWizardPage(adapter.getVersion(), dialog);
		// verify passed credentials when downloading CDK server adapter
		if (credentialsRequired()) {
			configureCredentials(config.usernameProperty(), config.passwordProperty());
			wizardPage.setCredentials(adapter.getUsername(), adapter.getPassword());
		}
		// find out hypervisor to be used
		adapter.setHypervisor(config.hypervisorProperty());
		// test if user wants to pass already prepared binary via system env. property (allows to test nightly, etc.)
		if (!isBinaryInProperty()) {
			downloadContainerRuntime(wizardPage);
		} else {
			wizardPage.setMinishiftBinary(CDKUtils.getSystemProperty(config.useExistingBinaryInProperty()));
		}
		adapter.setMinishiftBinary(Paths.get(wizardPage.getMinishiftBinaryLabeledText().getText()));
		adapter.setInstallationFolder(getServerAdapter().getMinishiftBinary().getParent());
		// setup proper hypervisor
		wizardPage.setHypervisor(getServerAdapter().getHypervisor());
		// CDK 3.2+ and Minishift 1.7+ required fields
		if (wizardPage instanceof NewCDK32ServerWizardPage) {
			getServerAdapter()
					.setMinishiftHome(Paths.get(((NewCDK32ServerWizardPage) wizardPage).getMinishiftHome().getText()));

			if (!StringUtils.isEmptyOrNull(config.profile())) {
				((NewCDK32ServerWizardPage) wizardPage).setMinishiftProfile(config.profile());
			}
			adapter.setProfile(((NewCDK32ServerWizardPage) wizardPage).getMinishiftProfile().getText());
		} else if (wizardPage instanceof NewCDK3ServerWizardPage) {
			// since CDK 3 does not have Home label in wizard, we have to use default value
			// or open server editor
			adapter.setMinishiftHome(MINISHIFT_HOME_DIRECTORY);
		}
		if (config.createServerAdapter()) {
			dialog.finish();
		} else {
			dialog.cancel();
		}
	}
	
	private boolean isBinaryInProperty() {
		if (!StringUtils.isEmptyOrNull(config.useExistingBinaryInProperty()) && 
				CDKUtils.getSystemProperty(config.useExistingBinaryInProperty()) != null) {
			return true;
		} 
		return false;
	}

	/**
	 * 	Decide whether to download new container runtime or use already existing
	 *  binary from config file - usually from previous downloading
	 * @param wizardPage wizard dialog object
	 */
	private void downloadContainerRuntime(NewCDK3ServerWizardPage wizardPage) {

		if (config.useExistingBinaryFromConfig() && Files.exists(PROPERTIES_FILE)) {
			try {
				Properties properties = CDKUtils.loadProperties(PROPERTIES_FILE);
				Object binary = properties.get(adapter.getVersion().type().name());
				if (binary == null) {
					// there is no existing minishift binary stored in properties, must be
					// downloaded
					log.info("There is no binary of that type downloaded, will download it now");
					processContainerDownload(adapter, wizardPage);
				} else {
					wizardPage.setMinishiftBinary(binary.toString());
				}
			} catch (IOException e) {
				throw new CDKException("IOException while loading properties file", e);
			}
		} else {
			processContainerDownload(adapter, wizardPage);
		}
	}

	@Override
	public void setDeclaration(ContainerRuntimeServer declaration) {
		this.config = declaration;
	}

	@Override
	public ContainerRuntimeServer getDeclaration() {
		return this.config;
	}

	@Override
	public void cleanUp() {
		// if implemented, add stop on finish, delete on finish, etc
		if (config.deleteAdapterOnFinish() && config.createServerAdapter()) {
			CDKUtils.deleteCDKServerAdapter(getServerAdapter().getAdapterName());
			deleteCertificates();
		}
		if (!isBinaryInProperty()) {
			if (!config.makeRuntimePersistent()) {
				if (CDKUtils.isCDKServerType(config.version().type().serverType())) {
					CDKUtils.deleteFilesIfExist(getServerAdapter().getMinishiftBinary());
				} else {
					CDKUtils.deleteFilesIfExist(getServerAdapter().getInstallationFolder());
				}
				removeKeyFromPropertiesFile(getServerAdapter().getVersion().type().name());
				CDKUtils.deleteFilesIfExist(DOWNLOAD_DIRECTORY);
			} else {
				writeToContainersPropertiesFile();
			}
		}
	}

	// methods necessary to configure server adapter in fulfill

	protected boolean credentialsRequired() {
		String serverType = config.version().type().serverType();
		log.info("Required Container Runtime type is " + serverType);
		if (CDKUtils.isCDKServerType(serverType)) {
			if (StringUtils.isEmptyOrNull(config.usernameProperty())
					|| StringUtils.isEmptyOrNull(config.passwordProperty())) {
				log.error("Downloading CDK server adapter requires inserting system property for credentials");
				throw new CDKException("Downloading CDK server adapter requires credentials, \r\n"
						+ "Please, set requirement's parameters: usernameProperty and passwordProperty");
			} else {
				return true;
			}
		}
		return false;
	}

	protected void processContainerDownload(ServerAdapter adapter, NewCDK3ServerWizardPage wizardPage) {
		CDKUtils.initializeDownloadRutimeDialog(wizardPage);
		// we will use installation folder placed within project directory, this is by
		// default
		// because annotation attribute cannot get any variable that would be relatively
		// set and user must pass constant expression
		DownloadCDKRuntimesUtility util = new DownloadCDKRuntimesUtility(
				INSTALLATION_DIRECTORY.toAbsolutePath().toString(), DOWNLOAD_DIRECTORY.toAbsolutePath().toString(),
				true);
		util.chooseRuntimeToDownload(adapter.getVersion());
		if (!(wizardPage instanceof NewMinishiftServerWizardPage)) {
			util.processCredentials(adapter.getUsername(), adapter.getPassword());
		}
		util.acceptLicense();
		util.downloadRuntime();
		try {
			new WaitUntil(new AbstractWaitCondition() {
				@Override
				public boolean test() {
					return wizardPage.getMinishiftBinaryLabeledText().getText()
							.contains(adapter.getVersion().downloadName());
				}
			}, TimePeriod.DEFAULT);
			new WaitUntil(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)),
					TimePeriod.DEFAULT, false);
		} catch (WaitTimeoutExpiredException waitExc) {
			throw new CDKException("Expected path to have " + adapter.getVersion().downloadName() + " but was: "
					+ wizardPage.getMinishiftBinaryLabeledText().getText());
		}
	}

	protected void configureCredentials(String usernameProperty, String passwordProperty) {
		getServerAdapter().setCredentialsKeys(usernameProperty, passwordProperty);
	}

	/**
	 * Decides whether to pass warning dialog with override option
	 * 
	 * @param forceOverride decides whether to override existing folder for cdk
	 *                      config files
	 */
	public void configureCDKServerAdapter(boolean forceOverride) {
		if (CDKUtils.isCDKServerType(config.version().type().serverType())) {
			log.info("Server adapter " + getServerAdapter().getAdapterName() + " is CDK server type");
			try {
				((CDKServer) getServerAdapter().getServer()).setupCDK();
			} catch (CDKServerException cdkExc) {
				if (cdkExc.getMessage().contains("Menu item 'Setup CDK' is not available")) {
					return;
				} else {
					throw cdkExc;
				}
			}
			try {
				if (forceOverride) {
					CDKUtils.confirmOverwritingOfSetupCDK(CDKLabel.Shell.WARNING_FOLDER_EXISTS);
				} else {
					CDKUtils.handleWarningDialog(CDKLabel.Shell.WARNING_FOLDER_EXISTS, CancelButton.class);
				}
			} catch (WaitTimeoutExpiredException exc) {
				log.info("Override warning dialog did not pop up");
			}
		} else {
			log.info("Server adapter is not of CDK server type");
		}
	}

	public void writeToContainersPropertiesFile() {
		Properties properties = new Properties();
		properties.setProperty(getServerAdapter().getVersion().type().name(),
				getServerAdapter().getMinishiftBinary().toAbsolutePath().toString());
		if (!Files.exists(PROPERTIES_FILE, LinkOption.NOFOLLOW_LINKS)) {
			CDKUtils.appendProperties(PROPERTIES_FILE, properties);
		} else {
			// check for existing minishift binary in properties file
			try {
				Properties existingProperties = CDKUtils.loadProperties(PROPERTIES_FILE);
				if (existingProperties.containsKey(getServerAdapter().getVersion().type().name())) {
					log.info("Required container runtime server type " + getServerAdapter().getVersion().type().name()
							+ " is already in properties file");
				} else {
					// append to existing properties
					CDKUtils.appendProperties(PROPERTIES_FILE, properties);
				}
			} catch (IOException e) {
				throw new CDKException("IOException while loading properties file " + PROPERTIES_FILE.toString()
						+ ", with exception: " + e.getMessage());
			}
		}
	}

	public void removeKeyFromPropertiesFile(Object key) {
		if (Files.exists(PROPERTIES_FILE, LinkOption.NOFOLLOW_LINKS)) {
			Properties properties;
			try {
				properties = CDKUtils.loadProperties(PROPERTIES_FILE);
				properties.remove(key);
				CDKUtils.writeProperties(PROPERTIES_FILE, properties);
			} catch (IOException e) {
				throw new CDKException("IOException while loading properties file " + PROPERTIES_FILE.toString()
						+ ", with exception: " + e.getMessage());
			}

		}
	}

	/**
	 * Starts server adapter defined in getServerAdapter abstract method and checks
	 * server's state, method's parameter accepts lambda expression that expects or
	 * consumes method to be run before cdk is started. Was designed to set up
	 * server adapter launching arguments.
	 */
	public void startServerAdapter(Runnable cond) {
		startServerAdapter(cond, false);
	}

	public void startServerAdapter(Runnable cond, boolean rethrow) {
		startServerAdapter(getServerAdapter().getServer(), cond, rethrow);
	}

	public void startServerAdapter(Server server, Runnable cond, boolean rethrow) {
		log.info("Starting server adapter");
		ServerOperationHandler.getInstance().handleOperation(() -> server.start(), cond, rethrow);
		assertEquals(ServerState.STARTED, getServerAdapter().getServer().getLabel().getState());
	}

	/**
	 * Starts server adapter only if not running yet
	 */
	public void startServerAdapterIfNotRunning(Runnable cond, boolean wait) {
		startServerAdapterIfNotRunning(getServerAdapter().getServer(), cond, wait);
	}

	/**
	 * Starts server adapter only if not running yet
	 */
	public void startServerAdapterIfNotRunning(Server server, Runnable cond, boolean wait) {
		if (getServerAdapter().getServer().getLabel().getState().equals(ServerState.STARTED)) {
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
	public void restartServerAdapter() {
		restartServerAdapter(getServerAdapter().getServer());
	}

	public void restartServerAdapter(Server server) {
		log.info("Restarting server adapter");
		ServerOperationHandler.getInstance().handleOperation(() -> server.restart(), () -> {
		});
		assertEquals(ServerState.STARTED, getServerAdapter().getServer().getLabel().getState());
	}

	/**
	 * Stops server adapter defined in getServerAdapter abstract method and checks
	 * server's state
	 */
	public void stopServerAdapter() {
		stopServerAdapter(getServerAdapter().getServer());
	}

	public void stopServerAdapter(Server server) {
		log.info("Stopping server adapter");
		ServerOperationHandler.getInstance().handleOperation(() -> server.stop(), () -> {
		});
		assertEquals(ServerState.STOPPED, getServerAdapter().getServer().getLabel().getState());
	}

	public ServerAdapter getServerAdapter() {
		return this.adapter;
	}

	// server can be configured when there is same server type ID as requirement's
	// version
	// and can be specified by its server adapter name (same as adapterName)
	public Server getConfiguredServer() {
		ServersView2 view = new CDKServersView();
		view.open();
		// maybe add a check for server type specified by CDKVersion
		return view.getServer(getServerAdapter().getAdapterName());
	}

	public boolean isContainerRuntimeServerPresent() {
		return getConfiguredServer() != null;
	}

	protected static WizardDialog setupNewWizardDialog(ServerAdapter adapter) {
		WizardDialog dialog = CDKUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);

		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, adapter.getVersion().serverName());
		if (!StringUtils.isEmptyOrNull(adapter.getAdapterName())) {
			page.setName(adapter.getAdapterName());
		} else {
			// setting default server adapter name
			adapter.setAdapterName(new LabeledText(page, "Server name:").getText());
		}
		dialog.next();
		return dialog;
	}

	/**
	 * Deletes all Openshift SSL certificates that were accepted from Preferences
	 * -> JBossTools -> OpenShift -> SSL Certificates
	 */
	private void deleteCertificates() {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();

		OpenShift3SSLCertificatePreferencePage preferencePage = new OpenShift3SSLCertificatePreferencePage(dialog);
		dialog.select(preferencePage);
		preferencePage.deleteAll();
		preferencePage.apply();
		dialog.ok();
	}

}
