/******************************************************************************* 
 * Copyright (c) 2017-2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithMnemonicTextMatcher;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.jface.exception.JFaceLayerException;
import org.eclipse.reddeer.jface.handler.TreeViewerHandler;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.clabel.DefaultCLabel;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.handler.EditorHandler;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.eclipse.ui.IEditorPart;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.exception.CDKException;
import org.jboss.tools.cdk.reddeer.ui.preferences.OpenShift3SSLCertificatePreferencePage;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShift3PreferencePage;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;

/**
 * Utilities for CDK tests
 * @author odockal
 *
 */
public class CDKTestUtils {

	private static Logger log = Logger.getLogger(CDKTestUtils.class);

	private CDKTestUtils() {}
	
	public static void checkParameterNotNull(Map<String, String> dict) {
		for (String key : dict.keySet()) {
			String value = dict.get(key);
			if (value == null) {
				throw new RedDeerException("Given key " + key + " value is null");  
			}
			log.info("Given key " + key + " value is " + value);  
		}	
	}
	
	/**
	 * Tests OpenShift connection and try to refresh it
	 * 
	 * @param connection
	 */
	public static void testOpenshiftConnection(String server, String username) {
		OpenShift3Connection connection = null;
		try {
			connection = findOpenShiftConnection(server, username);
		} catch (OpenShiftToolsException toolsExc) {
			fail(toolsExc.getMessage());
		}
		testOpenshiftConnection(connection);
	}
	
	/**
	 * Tests OpenShift connection and try to refresh it
	 * 
	 * @param connection
	 */
	public static void testOpenshiftConnection(OpenShift3Connection connection) {
		log.info("Performing test of OS 3 connection: " + connection.getTreeItem().getText());
		// usually, when server adapter is not started, openshift connection after refresh should cause 
		// problem occurred dialog
		connection.refresh(TimePeriod.getCustom(120));
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
	public static void testDockerConnection(String dockerDaemon) {
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
	 * Finds OpenShift connection based on server username parameters
	 * 
	 * @param server server name
	 * @param username connection username
	 * @return OpenShift connection object if found
	 */
	public static OpenShift3Connection findOpenShiftConnection(String server, String username) {
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
	 * Verification of successful registration of rhel image during cdk start up
	 * Checks for reg. expressions in console
	 */
	public static void verifyConsoleContainsRegEx(String regex) {
		ConsoleView view = new ConsoleView();
		view.open();
		String consoleText = view.getConsoleText();
		if (consoleText == null) {
			fail("Console does not contain any text");
		}
		Pattern pattern = Pattern.compile(regex);
		assertTrue("Console text does not contains regex: \r\n" + regex + 
				"\r\nConsole text:\r\n" + consoleText, 
				pattern.matcher(consoleText).find());
	}
	
	public static List<Server> getAllServers() {
		log.info("Collecting all server adapters"); 
		ServersView2 view = new ServersView2();
		view.open();
		return view.getServers();
	}
	
	/**
	 * We need to override save method from EditorHandler to be executed in async
	 * thread in order to be able to work with message dialog from invalid server
	 * editor location
	 * 
	 * @param editor
	 *            IEditorPart to work with during saving
	 */
	public static void performSave(final IEditorPart editor) {
		EditorHandler.getInstance().activate(editor);
		Display.asyncExec(new Runnable() {

			@Override
			public void run() {
				editor.doSave(new NullProgressMonitor());

			}
		});
		new WaitUntil(new WaitCondition() {

			@Override
			public boolean test() {
				return !editor.isDirty();
			}

			@Override
			public String description() {
				return " editor is not dirty...";
			}

			@Override
			public <T> T getResult() {
				return null;
			}

			@Override
			public String errorMessageWhile() {
				return null;
			}

			@Override
			public String errorMessageUntil() {
				return " editor is still dirty...";
			}
		}, TimePeriod.MEDIUM);
	}
	
	/**
	 * Prints out OpenShift accepted certificates
	 */
	public static void printCertificates() {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		OpenShift3SSLCertificatePreferencePage preferencePage = new OpenShift3SSLCertificatePreferencePage(dialog);
		dialog.select(preferencePage);
		preferencePage.printCertificates();
        dialog.ok();
	}
	
	public static void setOCToPreferences(String ocPath) {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		OpenShift3PreferencePage page = new OpenShift3PreferencePage(dialog);
		
		dialog.open();
		dialog.select(page);
		
		// check for accidentally opened Could not accept changes dialog and close it
		try {
			new DefaultShell("Could Not Accept Changes ");
			new PushButton("OK").click();
		} catch (CoreLayerException exc) {
			// no such shell exists which is ok
			log.info("Could not accept changes dialog did not appered");
		}
		
		page.setOCLocation(ocPath);
		dialog.activate();
		try {
			new WaitUntil(new ControlIsEnabled(new PushButton(CDKLabel.Buttons.APPLY)), TimePeriod.DEFAULT); 
		} catch (WaitTimeoutExpiredException exc) {
			CDKUtils.captureScreenshot("SettingOCToPreferencesException");
			dialog.cancel();
			fail("WaitTimeoutExpiredException occured while processing oc binary on path " + ocPath);
		}
		page.apply();
		dialog.cancel();
	}
	
	public static void assertSameMessage(final NewMenuWizard dialog, final String message) {
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.DEFAULT, false);
		String description = dialog.getMessage();
		assertTrue("Expected page description should contain text: '" + message + 
				"' but has: '" + description + "'", 
				description.contains(message));		
	}
	
	public static void assertDiffMessage(final NewMenuWizard dialog, final String message) {
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.DEFAULT, false);
		String description = dialog.getMessage();
		assertFalse("Page descrition should not contain: '" + message + "'", 
				description.contains(message));
	}
	
	public static String findFileOnPath(String path, String fileToFind) {
		log.info("Searching for " + fileToFind + " on " + path);  
		File startPath = new File(path);
		if (startPath.exists()) {
			try {
				return Files.find(startPath.toPath(), 10, 
						(p, attrs) -> fileFitsCondition(p, attrs, p.getFileName().toString().equalsIgnoreCase(fileToFind)
						&& attrs.isRegularFile()), FileVisitOption.FOLLOW_LINKS).findAny().get().toString();
			} catch (NoSuchElementException noExc) {
				throw new CDKException("There was not found any oc file in given path: " + path); 
			} catch (IOException e) {
				throw new CDKException("Could not find oc on given path: " + path); 
			}
		} else {
			throw new CDKException("Given path " + path + " does not exist");  
		}
	}
	
	private static boolean fileFitsCondition(Path p, BasicFileAttributes attrs, boolean condition) {
		log.debug("Actual path: \"" + p.getFileName().toString() + "\" and is regular file: " + attrs.isRegularFile());  
		return condition;
	}
	
	// removes access redhat com credentials used for first cdk run
	public static void removeAccessRedHatCredentials(String domain, String username) {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		dialog.select("JBoss Tools", "Credentials");  
        try {
	        new WaitUntil(new WidgetIsFound(
	        		org.eclipse.swt.custom.CLabel.class, 
	        		new WithMnemonicTextMatcher("Credentials")), 
	        		TimePeriod.MEDIUM);
	        new DefaultCLabel("Credentials"); 
	        DefaultTree tree = new DefaultTree(1);
	        TreeItem item = TreeViewerHandler.getInstance().getTreeItem(tree, new String[]{domain, username});
	        item.select();
	        new PushButton(new WithTextMatcher(CDKLabel.Buttons.REMOVE_USER)).click(); 
	        new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
        } catch (WaitTimeoutExpiredException exc) {
        	log.error("JBoss Tools - Credentials preferences page has timed out"); 
        	exc.printStackTrace();
        } catch (JFaceLayerException exc) {
        	log.error("JBoss Tools - Credentials does not contain required username to be deleted"); 
        	exc.printStackTrace();
        } finally {
        	dialog.ok();
		}
	}

}
