/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.junit.screenshot.CaptureScreenshotException;
import org.eclipse.reddeer.junit.screenshot.ScreenshotCapturer;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.Link;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKServerAdapterType;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.exception.CDKException;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK3ServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCRCServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewMinishiftServerWizardPage;


/**
 * Utility class for CDK reddeer test plugin
 * @author odockal
 *
 */
public final class CDKUtils {

	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win"); 
	
	public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux"); 
	
	private static final Logger log = Logger.getLogger(CDKUtils.class);

	private CDKUtils() {}
	
	public static CDKServerAdapterType getCDKServerType(String serverType) {
		for (CDKServerAdapterType type : CDKServerAdapterType.values()) {
			if (type.serverType().equals(serverType)) {
				return type;
			}
		}
		return CDKServerAdapterType.NO_CDK;
	}

	public static String getServerTypeIdFromItem(TreeItem item) {
		Object itemData = Display.syncExec(new ResultRunnable<Object>() {
			@Override
			public Object run() {
				return item.getSWTWidget().getData();
			}
		});
		if (IServer.class.isInstance(itemData)) {
			return ((IServer)itemData).getServerType().getId();
		}
		return "";
	}
	
	public static boolean isContainerRuntimeServer(Server server) {
		String type = CDKUtils.getServerTypeIdFromItem(server.getTreeItem());
		log.info("Server type id is " + type);
		return isContainerRuntimeServerType(type);
	}
	
	public static boolean isContainerRuntimeServerType(String type) {
		return Arrays.stream(CDKServerAdapterType.values()).anyMatch(e -> e.serverType().equals(type));
	}
	
	public static boolean isCDKServerType(String type) {
		return Arrays.stream(CDKServerAdapterType.values()).filter(e -> e.serverTypeName() == "CDK").anyMatch(e -> e.serverType().equals(type));
	}
	
	public static boolean isServerOfType(Server server, String type) {
		return getServerTypeIdFromItem(server.getTreeItem()).equals(type);
	}
	
	public static void deleteAllContainerRuntimeServerAdapters() {
		for (Server server : getAllServers()) {
			log.info("Found server with name " + server.getLabel().getName());
			if (CDKUtils.isContainerRuntimeServer(server)) {
				log.info("Deleting server...");
				server.delete(true);
			}
		}
	}
	
	public static void deleteCDKServerAdapter(String serverName) {
		if (getAllServers().isEmpty()) {
			log.info("There is no container server adapter");
			return;
		}
		Server server = getServer(serverName);
		if (server != null) {
			log.info(server.getLabel().getName() + " will be deleted");
			if (CDKUtils.isContainerRuntimeServer(server)) {
				log.info("Deleting server...");
				server.delete(true);
			}
		} else {
			log.info("There is no such server " + serverName + " in servers view");
		}
	}
	
	public static List<Server> getAllServers() {
		log.info("Collecting all server adapters");
		ServersView2 view = new ServersView2();
		view.open();
		return view.getServers();
	}
	
	public static Server getServer(String name) {
		for(Server server : getAllServers()) {
			if(server.getLabel().getName().equals(name)) {
				return server;
			}
		}
		return null;
	}
	
	public static void captureScreenshot(String name) {
		try {
			ScreenshotCapturer.getInstance().captureScreenshot(name);
		} catch (CaptureScreenshotException e) {
			log.error("Could not capture screenshot for " + name);
		}		
	}
	
	public static void appendProperties(Path path, Properties properties) {
		storeProperties(path, properties,
						StandardOpenOption.CREATE,
						StandardOpenOption.WRITE,
						StandardOpenOption.APPEND);
	}
	
	public static void writeProperties(Path path, Properties properties) {
		storeProperties(path, properties,
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE);
	}
	
	public static void storeProperties(Path path, Properties properties, OpenOption... options) {
		try (Writer writer = 
				Files.newBufferedWriter(path, options)) {
			properties.store(writer, "No comment");
		} catch (IOException e) {
			throw new CDKException("IOException while storing properties file " 
									+ path.toString() + ", exception: " + e.getMessage());
		}
	}
	
	public static Properties loadProperties(Path path) throws IOException {
		Properties properties = new Properties();
		InputStreamReader isr = null;
		try {
			byte [] fileArray = Files.readAllBytes(path);
			isr = new InputStreamReader(new ByteArrayInputStream(fileArray));
			properties.load(isr);
		} finally {
			if (isr != null) { 
				isr.close();
			}
		}
		return properties;
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
		NewMinishiftServerWizardPage containerPage = new NewMinishiftServerWizardPage(dialog);
		setupNewCDKWizardParts(hypervisor, path, containerPage);
		if (!profile.isEmpty()) {
			log.info("Setting minishift profile to " + profile); 
			containerPage.setMinishiftProfile(profile);
		} else {
			log.info("Keeping minishift profile default: " + containerPage.getMinishiftProfile()); 
		}
		dialog.finish();
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
		NewCDK32ServerWizardPage containerPage = new NewCDK32ServerWizardPage(dialog);
		containerPage.setCredentials(username, password);
		setupNewCDKWizardParts(hypervisor, path, containerPage);
		if (!profile.isEmpty()) {
			log.info("Setting minishift profile to " + profile); 
			containerPage.setMinishiftProfile(profile);
		} else {
			log.info("Keeping minishift profile default: " + containerPage.getMinishiftProfile()); 
		}
		dialog.finish();
	}
	
	/**
	 * Creates new CDK 3.x server adapter via ServersView -> New -> Server
	 * 
	 * @param serverAdapter server adapter name
	 * @param hypervisor hypervisor to use
	 * @param path path to minishift binary file
	 */
	public static void addNewCDK3Server(String serverAdapter, String hypervisor, String path, String username, String password) {
		NewCDKServerWizard dialog = setupFirstNewServerWizardPage(CDKLabel.Server.CDK3_SERVER_NAME, serverAdapter);
		
		// set second new server dialog page
		NewCDK3ServerWizardPage containerPage = new NewCDK3ServerWizardPage(dialog);
		containerPage.setCredentials(username, password);
		setupNewCDKWizardParts(hypervisor, path, containerPage);
		dialog.finish();
	}

	/**
	 * Creates new CDK 2.x server adapter via ServersView -> New -> Server
	 * 
	 * @param serverAdapter server adapter name
	 * @param path path to vagrantfile
	 */
	public static void addNewCDKServer(String serverAdapter, String path, String username, String password) {
		NewCDKServerWizard dialog = setupFirstNewServerWizardPage(CDKLabel.Server.CDK_SERVER_NAME, serverAdapter);
		
		// set second new server dialog page
		NewCDKServerWizardPage containerPage = new NewCDKServerWizardPage(dialog);
		containerPage.setCredentials(username, password);
		// set cdk 2.x fields
		log.info("Setting vagrant file folder"); 
		containerPage.setFolder(path);
		dialog.finish();
	}
	
	private static NewCDKServerWizard setupFirstNewServerWizardPage(String serverName, String serverAdapter) {
		NewCDKServerWizard dialog = openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		// set first dialog page
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, serverName);
		page.setHostName(CDKLabel.Server.SERVER_HOST);
		page.setName(serverAdapter);
		dialog.next();	
		return dialog;
	}
	
	private static void setupNewCDKWizardParts(String hypervisor, String path, NewCDK3ServerWizardPage containerPage) {
		if (hypervisor != null && !hypervisor.isEmpty()) {
			log.info("Setting hypervisor to " + hypervisor); 
			containerPage.setHypervisor(hypervisor);
		}
		log.info("Setting minishift binary file folder to " + path);
		containerPage.setMinishiftBinary(path);		
	}
	
	
	public static NewCDKServerWizard openNewServerWizardDialog() {
		log.info("Adding new Container Development Environment server adapter"); 
		// call new server dialog from servers view
		CDKServersView view = new CDKServersView();
		view.open();
		NewCDKServerWizard dialog = view.newCDKServer();
		new WaitWhile(new JobIsRunning(), TimePeriod.MEDIUM, false);
		return dialog;
	}
	
	public static NewCDK3ServerWizardPage chooseCDKWizardPage(CDKVersion version, WizardDialog dialog) {
		switch (version.serverName()) {
			case CDKLabel.Server.CDK3_SERVER_NAME:
				return new NewCDK3ServerWizardPage(dialog);
			case CDKLabel.Server.CDK32_SERVER_NAME:
				return new NewCDK32ServerWizardPage(dialog);
			case CDKLabel.Server.MINISHIFT_SERVER_NAME:
				return new NewMinishiftServerWizardPage(dialog);
			case CDKLabel.Server.CRC_SERVER_NAME:
				return new NewCRCServerWizardPage(dialog);
			default:
				return null;
		}
	}
	
	public static void initializeDownloadRutimeDialog(NewCDK3ServerWizardPage wizardPage) {
		Link link = wizardPage.getDownloadAndInstallLink();
		new WaitUntil(new ControlIsEnabled(link), TimePeriod.MEDIUM, false);
		link.click();
		new WaitUntil(new ShellIsAvailable(CDKLabel.Shell.DOWNLOAD_RUNTIMES), TimePeriod.MEDIUM);
	}
	
	public static void confirmOverwritingOfSetupCDK(String shellName) {
		SystemJobIsRunning runningJob = new SystemJobIsRunning(new JobMatcher("Setup CDK"));
		log.info("Confirming overwriting of minishift home content");
		handleWarningDialog(shellName, OkButton.class);
		new WaitUntil(runningJob, TimePeriod.MEDIUM);
		new WaitWhile(runningJob, TimePeriod.getCustom(30));
		new WaitUntil(new ConsoleHasNoChange(TimePeriod.MEDIUM), TimePeriod.DEFAULT);
	}
	
	public static void handleWarningDialog(String shellName, Class<? extends Button> button) {
		ShellIsAvailable dialog = new ShellIsAvailable(shellName);
		new WaitUntil(dialog);
		Button but;
		try {
			but = button.newInstance();
			but.click();
			new WaitWhile(dialog, TimePeriod.MEDIUM);
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("Could not instantiate button of class " + button.getName(), e);
			dialog.getResult().close();
		}
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
	 * Deletes given File object using Files.deleteIfExists(Path)
	 * @param file file or directory to delete
	 * @throws FileNotFoundException 
	 */
	public static void deleteRecursively(File file) throws FileNotFoundException {
		if (file.isFile()) {
			log.info("Deleting file " + file.getAbsolutePath());
			if (!file.delete()) {
				log.error("File " + file.getAbsolutePath() + " was not deleted");
			}
		} else if (file.isDirectory()) {
			if (file.listFiles().length >= 1) {
				for(File contentFile : file.listFiles()) {
					deleteRecursively(contentFile);
				}
			}
			log.info("Deleting dir " + file.getAbsolutePath());
			if (!file.delete()) {
				log.error("Directory " + file.getAbsolutePath() + " was not deleted");
			}
		} else {
			throw new FileNotFoundException("Given file " + file.getAbsolutePath() + " is not a standard file or directory");
		}
	}
	
	public static void deleteFilesIfExist(File file) {
		log.info("Deleting all content under file path " + file.getAbsolutePath());
		try {
			deleteRecursively(file);
		} catch (FileNotFoundException e) {
			log.error("Given path does not exists, do nothing");
		}
	}
	
	public static void deleteFilesIfExist(Path path) {
		deleteFilesIfExist(path.toFile());
	}

	public static String getSystemProperty(String systemProperty) {
		String property = System.getProperty(systemProperty);
		if (!(property == null || property.equals("") || property.startsWith("${"))) {  
			return property;
		}
		return null;
	}
	
}
