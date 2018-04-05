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
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK32ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDKPart;
import org.jboss.tools.cdk.reddeer.server.ui.editor.Minishift17ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.MinishiftServerEditor;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases covering setup-cdk feature, JBIDE-25869
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CDKServerAdapterSetupCDKTest extends CDKServerAdapterAbstractTest {

	private static final Logger log = Logger.getLogger(CDKServerAdapterSetupCDKTest.class);
	
	protected MinishiftServerEditor editor;
	private Server minishiftServer;
	private static final String TIME_STAMP = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Timestamp(System.currentTimeMillis()));
	private static final String USER_HOME = System.getProperty("user.home") + separator;
	private static File NON_EXISTING_DIR = new File(USER_HOME + "non_existing_folder" + TIME_STAMP);
	private static File EXISTING_DIR = new File(USER_HOME + "existing_folder" + TIME_STAMP);
	private static File MINISHIFT_HOME_DIR = new File(USER_HOME + "minishift_home" + TIME_STAMP);
	
	static {
		if (!EXISTING_DIR.exists()) {
			EXISTING_DIR.mkdirs();
		}
	}
	
	public void cleanUp() {
		if (editor != null) {
			if (!editor.isActive()) {
				editor.activate();
			}
			editor.close(false);
			editor = null;
		}
	}
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	protected String getMinishiftServerAdapter() {
		return SERVER_ADAPTER_MINISHIFT;
	}
	
	private Server getMinishiftServer() {
		return minishiftServer;
	}
	
	@BeforeClass
	public static void setupCDKServerAdapterSetupCDKTest() {
		checkDevelopersParameters();
		checkCDK32Parameters();
		addNewCDK32Server(SERVER_ADAPTER_32, MINISHIFT_HYPERVISOR, CDK32_MINISHIFT, MINISHIFT_PROFILE);
		addNewMinishiftServer(SERVER_ADAPTER_MINISHIFT, MINISHIFT_HYPERVISOR, MOCK_MINISHIFT170, MINISHIFT_PROFILE);
	}
	
	public void setCDKServerAdapterSetupCDK() {
		openServerEditor(getServerAdapter());
		editor = new CDK32ServerEditor(getServerAdapter());
		editor.activate();
		if (!((CDKPart)editor).getMinishiftHomeLabel().getText().equals(DEFAULT_MINISHIFT_HOME)) {
			changeMinishiftHomeTo(DEFAULT_MINISHIFT_HOME);
		}
		new WaitUntil(new JobIsRunning(), TimePeriod.SHORT, false);
	}
	
	public void setMinishiftServerAdapterSetupCDK() {
		minishiftServer = serversView.getServer(getMinishiftServerAdapter());
		openServerEditor(getMinishiftServerAdapter());
		editor = new Minishift17ServerEditor(getMinishiftServerAdapter());
		editor.activate();
		if (!((CDKPart)editor).getMinishiftHomeLabel().getText().equals(DEFAULT_MINISHIFT_HOME)) {
			changeMinishiftHomeTo(DEFAULT_MINISHIFT_HOME);
		}
		new WaitUntil(new JobIsRunning(), TimePeriod.SHORT, false);
	}
	
	@After
	public void tearDownCDKServerAdapterSetupCDK() {
		cleanUp();
		clearConsole();
		CDKTestUtils.deleteFilesIfExist(NON_EXISTING_DIR);
		CDKTestUtils.deleteFilesIfExist(EXISTING_DIR);
		CDKTestUtils.deleteFilesIfExist(MINISHIFT_HOME_DIR);
	}
	
	@Test
	public void testSetupCDKExistingMinishiftHomeDirectory() {
		setCDKServerAdapterSetupCDK();
		changeMinishiftHomeTo(MINISHIFT_HOME_DIR.getAbsolutePath());
		((CDKServer) getCDKServer()).setupCDK();
		new WaitUntil(new ConsoleHasNoChange(TimePeriod.MEDIUM), TimePeriod.getCustom(20));
		checkConsoleOutput(MINISHIFT_HOME_DIR.getName());
		clearConsole();
		((CDKServer) getCDKServer()).setupCDK();
		confirmOverwritingOfSetupCDK(MINISHIFT_HOME_DIR.getName(), CDKLabel.Shell.WARNING_FOLDER_EXISTS);
		verifySetupCDKCreatedFiles(MINISHIFT_HOME_DIR);
	}

	@Test
	public void testSetupCDKNonExistingDirectory() {
		setCDKServerAdapterSetupCDK();
		changeMinishiftHomeTo(NON_EXISTING_DIR.getAbsolutePath());
		((CDKServer) getCDKServer()).setupCDK();
		ShellIsAvailable wait = new ShellIsAvailable(CDKLabel.Shell.WARNING_FOLDER_EXISTS);
		new WaitUntil(wait, TimePeriod.MEDIUM, false);
		if ( wait.getResult() != null) {
			handleWarningDialog(CDKLabel.Shell.WARNING_FOLDER_EXISTS, CancelButton.class);
			fail("Dialog " + CDKLabel.Shell.WARNING_FOLDER_EXISTS + " should have not appeared");
		}
		new WaitUntil(new ConsoleHasNoChange(TimePeriod.MEDIUM), TimePeriod.DEFAULT);
		checkConsoleOutput(NON_EXISTING_DIR.getName());
		verifySetupCDKCreatedFiles(NON_EXISTING_DIR);
	}
	
	@Test
	public void testSetupCDKOnExistingDirectory() {
		setCDKServerAdapterSetupCDK();
		File existing = new File(EXISTING_DIR.getAbsolutePath());
		if (!existing.exists()) {
			existing.mkdirs();
		}
		changeMinishiftHomeTo(EXISTING_DIR.getAbsolutePath());
		((CDKServer) getCDKServer()).setupCDK();
		confirmOverwritingOfSetupCDK(EXISTING_DIR.getName(), CDKLabel.Shell.WARNING_FOLDER_EXISTS);
		checkConsoleOutput(EXISTING_DIR.getName());
		verifySetupCDKCreatedFiles(EXISTING_DIR);
	}
	
	@Test
	public void testSetupCDKOnServerAdapterStart() {
		setCDKServerAdapterSetupCDK();
		changeMinishiftHomeTo(MINISHIFT_HOME_DIR.getAbsolutePath());
		try {
			((CDKServer) getCDKServer()).select();
			new ContextMenuItem("Start").select();
			handleWarningDialog(CDKLabel.Shell.WARNING_CDK_NOT_INICIALIZED, CancelButton.class);
			handleWarningDialog(CDKLabel.Shell.PROBLEM_DIALOG, OkButton.class);
			new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
			new WaitWhile(new JobIsRunning(), TimePeriod.MEDIUM, false);
			assertEquals(ServerState.STOPPED, getCDKServer().getLabel().getState());
		} catch (CoreLayerException coreExc) {
			fail("Could not call Start context menu item over server adapter");
		}
	}
	
	@Test
	public void testSetupCDKNotCalledOnMinishiftServerAdapter() {
		setMinishiftServerAdapterSetupCDK();
		changeMinishiftHomeTo(MINISHIFT_HOME_DIR.getAbsolutePath());
		try {
			((CDKServer) getMinishiftServer()).select();
			new ContextMenuItem("Start").select();
			ShellIsAvailable shell = new ShellIsAvailable(CDKLabel.Shell.WARNING_CDK_NOT_INICIALIZED);
			new WaitUntil(shell, TimePeriod.MEDIUM, false);
			if (shell.getResult() != null) {
				fail("Starting minishift server adapter is not supposed to configure CDK");
			}
			handleWarningDialog(CDKLabel.Shell.PROBLEM_DIALOG, OkButton.class);
			new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
			new WaitWhile(new JobIsRunning(), TimePeriod.MEDIUM, false);
			assertEquals(ServerState.STOPPED, getMinishiftServer().getLabel().getState());
		} catch (CoreLayerException coreExc) {
			fail("Could not call Start context menu item over server adapter");
		}
	}
	
	private void confirmOverwritingOfSetupCDK(String filePath, String shellName) {
		log.info("Confirming overwriting of minishift home content at " + filePath);
		handleWarningDialog(shellName, OkButton.class);
		new WaitUntil(new SystemJobIsRunning(new JobMatcher("Setup CDK")), TimePeriod.MEDIUM);
		new WaitUntil(new ConsoleHasNoChange(TimePeriod.MEDIUM), TimePeriod.getCustom(20));
		new WaitWhile(new SystemJobIsRunning(new JobMatcher("Setup CDK")), TimePeriod.MEDIUM);
		checkConsoleOutput(filePath);
	}

	private void checkConsoleOutput(String filePath) {
		verifyConsoleContainsRegEx("\\bSetting up CDK 3 on host\\b");
		verifyConsoleContainsRegEx("\\b" + filePath + "\\b");
		verifyConsoleContainsRegEx("\\bCDK 3 setup complete\\b");		
	}
	
	private void handleWarningDialog(String shellName, Class<? extends Button> button) {
		ShellIsAvailable dialog = new ShellIsAvailable(shellName);
		new WaitUntil(dialog, TimePeriod.MEDIUM);
		Button but;
		try {
			but = button.newInstance();
			but.click();
			new WaitWhile(dialog, TimePeriod.MEDIUM);
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("Could not instantiate button of class " + button.getName());
			e.printStackTrace();
		}	
	}
	
	private void clearConsole() {
		log.info("Console will be cleaned up");
		ConsoleView console = new ConsoleView();
		if (console.canClearConsole()) {
			console.clearConsole();
		}
	}
	
	private void verifySetupCDKCreatedFiles(File path) {
		log.info("Verifying existence of files created by setup-cdk command");
		if (!path.isDirectory()) {
			fail("Given path " + path.getAbsolutePath() + " is not directory or does not exists");
		}
		Set<String> actualFiles = Arrays.asList(path.listFiles())
				.stream()
				.map(File::getName)
				.collect(Collectors.toSet());
		Set<String> expectedFiles = new HashSet<>(Arrays.asList("cache", "cdk", "config"));
		expectedFiles.stream().forEach(item -> {
			if (!actualFiles.contains(item)) {
				fail(item + " is not in expected minishift config. files");
			}
		});
	}
	
	private void changeMinishiftHomeTo(String changeTo) {
		log.info("Change minishift home from " + ((CDKPart)editor).getMinishiftHomeLabel().getText() + " to " + changeTo);
		((CDKPart)editor).getMinishiftHomeLabel().setText(changeTo);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);
		CDKTestUtils.performSave(editor.getEditorPart());		
	}
	
	private void openServerEditor(String editorName) {
		serversView = new CDKServersView();
		serversView.open();
		serversView.getServer(editorName).open();
	}
	
}
