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
package org.jboss.tools.openshift.ui.bot.test.application.v3.debug;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.condition.ServerHasPublishState;
import org.eclipse.reddeer.eclipse.condition.ServerHasState;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.ProjectItem;
import org.eclipse.reddeer.eclipse.debug.ui.views.breakpoints.BreakpointsView;
import org.eclipse.reddeer.eclipse.debug.ui.views.launch.LaunchView;
import org.eclipse.reddeer.eclipse.debug.ui.views.variables.VariablesView;
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.perspectives.DebugPerspective;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerPublishState;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.junit.screenshot.CaptureScreenshotException;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.styledtext.DefaultStyledText;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.preference.page.JavaDebugPreferencePage;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.condition.SuspendedTreeItemIsReady;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(DebugPerspective.class)
@RunWith(RedDeerSuite.class)
@CleanOpenShiftExplorer
@RequiredBasicConnection
@CleanConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE_RESOURCES_PATH)
public class DebuggingEAPAppTest extends AbstractTest {

	private static Logger LOGGER = new Logger(DebuggingEAPAppTest.class);

	private static final String GIT_REPO_URL = "https://github.com/jboss-developer/jboss-eap-quickstarts";

	private static final String GIT_REPO_DIRECTORY = "target/git_repo";
	
	private static final String GIT_REPO_BRANCH = "openshift";

	private static final String PROJECT_NAME = "helloworld";

	private static String requestResponse = null;

	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;

	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	private static ServerAdapter serverAdapter;

	@BeforeClass
	public static void setupClass() {
		cloneGitRepoAndImportProject();
		
		rebuildProject();

		doNotSuspendOnUncaughtExceptions();

		toggleAutoBuild(false);

		createServerAdapter();

		disableShowConsoleWhenOutputChanges();
		
		serverAdapter = new ServerAdapter(Version.OPENSHIFT3, OpenShiftResources.EAP_SERVICE, "Service");
		try {
			restartServerInDebug(serverAdapter);
		} catch (WaitTimeoutExpiredException ex) {
			//try once again
			restartServerInDebug(serverAdapter);
		}

		cleanAndBuildWorkspace();
	}
	
	private static void restartServerInDebug(ServerAdapter serverAdapter) {
		serverAdapter.select();
		new ContextMenuItem("Restart in Debug").select();
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		waitForserverAdapterToBeInRightState();
	}

	private static void rebuildProject() {
		PackageExplorerPart pexplorer = new PackageExplorerPart();
		pexplorer.open();
		pexplorer.getProject(PROJECT_NAME).select();
		new ContextMenuItem("Maven","Update Project...").select();
		new WaitUntil(new ShellIsAvailable("Update Maven Project"),TimePeriod.LONG);
		new CheckBox("Force Update of Snapshots/Releases").toggle(true);
		new PushButton("OK").click();
		new WaitWhile(new ShellIsAvailable("Update Maven Project"),TimePeriod.DEFAULT);
		new WaitWhile(new JobIsRunning(),TimePeriod.VERY_LONG);
	}

	private static void cloneGitRepoAndImportProject() {
		OpenShiftUtils.cloneGitRepository(GIT_REPO_DIRECTORY, GIT_REPO_URL, GIT_REPO_BRANCH, true);
		OpenShiftUtils.importProjectUsingSmartImport(GIT_REPO_DIRECTORY, PROJECT_NAME);
	}

	@AfterClass
	public static void tearDownClass() {
		toggleAutoBuild(true);
		cleanProjectsAndGitRepo();
	}

	private static void cleanProjectsAndGitRepo() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		projectExplorer.deleteAllProjects(true);
		try {
			TestUtils.delete(new File(GIT_REPO_DIRECTORY));
		} catch (IOException e) {
			throw new RuntimeException("Deletion of git repo was unsuccessfull.", e);
		}
	}

	@Before
	public void setup() {
		setupBreakpoint();

		cleanAndBuildWorkspace();

		triggerDebugSession();
	}

	@Test
	public void debuggerStopsAtBreakpointTest() {

		// now it shoud be stopped in debug mode.

		checkDebugView();

		checkVariablesView();
	}

	@Test
	public void changeVariableValueTest() throws CaptureScreenshotException {

		setNewVariableValue("NewWorld", "name");

		checkNewVariableValueIsPropagatedToBrowser();
	}

	@After
	public void teardown() {
		try {
			new ShellMenuItem("Run", "Terminate").select();
		} catch (CoreLayerException ex) {
			if (ex.getMessage().contains("Menu item is not enabled")) {
				// no big deal, there is no execution running
			} else {
				throw ex;
			}
		}

		// remove all breakpoints
		BreakpointsView breakpointsView = new BreakpointsView();
		breakpointsView.open();
		breakpointsView.removeAllBreakpoints();

	}

	private static void doNotSuspendOnUncaughtExceptions() {
		WorkbenchPreferenceDialog workbenchPreferenceDialog = new WorkbenchPreferenceDialog();
		workbenchPreferenceDialog.open();
		JavaDebugPreferencePage javaDebugPreferencePage = new JavaDebugPreferencePage(workbenchPreferenceDialog);
		workbenchPreferenceDialog.select(javaDebugPreferencePage);

		javaDebugPreferencePage.setSuspendOnUncaughtExceptions(false);

		workbenchPreferenceDialog.ok();
	}

	private static void toggleAutoBuild(boolean autoBuild) {
		ShellMenuItem autoBuildMenuItem = new ShellMenuItem("Project", "Build Automatically");
		boolean isSelected = autoBuildMenuItem.isSelected();
		if (autoBuild && !isSelected) {
			autoBuildMenuItem.select();
		}
		if (!autoBuild && isSelected) {
			autoBuildMenuItem.select();
		}
	}

	private static void cleanAndBuildWorkspace() {
		new ShellMenuItem("Project", "Clean...").select();
		new DefaultShell("Clean");
		new PushButton("Clean").click();
		new WaitWhile(new JobIsRunning());
	}

	private static void waitForserverAdapterToBeInRightState() {
		new WaitUntil(new ServerHasState(new ServersView2().getServer(serverAdapter.getLabel()), ServerState.DEBUGGING), TimePeriod.VERY_LONG);
		new WaitUntil(new ServerHasPublishState(new ServersView2().getServer(serverAdapter.getLabel()),
				ServerPublishState.SYNCHRONIZED));
	}

	private void setupBreakpoint() {
		// set breakpoint where we need it.
		ProjectItem helloServiceFile = getHelloServiceFile();
		setBreakpointToLineWithText(helloServiceFile, "return \"Hello");
	}

	private void checkNewVariableValueIsPropagatedToBrowser() {

		clickResume();
		new WaitUntil(new AbstractWaitCondition() {

			@Override
			public boolean test() {
				return ((requestResponse != null) && requestResponse.contains("NewWorld"));
			}
		}, TimePeriod.LONG);

	}

	private void clickResume() {
		new WaitWhile(new AbstractWaitCondition() {

			@Override
			public boolean test() {
				ShellMenuItem resumeMenu = new ShellMenuItem("Run", "Resume");
				if (resumeMenu.isEnabled()) {
					resumeMenu.select();
					return true;
				} else {
					return false;
				}
			}
		});
	}

	private void checkDebugView() {
		LaunchView debugView = new LaunchView();
		debugView.open();

		TreeItem createHelloMessageDebugItem = ensureCorrectFrameIsSelected(debugView);

		assertTrue(createHelloMessageDebugItem.getText().contains("createHelloMessage"));
	}

	private TreeItem ensureCorrectFrameIsSelected(LaunchView debugView) {
		List<TreeItem> items;
		TreeItem createHelloMessageDebugItem;

		// get frames of suspended thread. If the desired frame is not present,
		// try reopening Debug view
		items = getSuspendedThreadTreeItem(debugView).getItems();
		if (items.size() < 2) {
			// no stack trace available. Try to close&reopen Debug view (dirty
			// hack)
			debugView.close();
			debugView = new LaunchView();
			debugView.open();
			items = getSuspendedThreadTreeItem(debugView).getItems();
		}

		final List<TreeItem> tIList = items;

		// wait for frame texts to populate.
		new WaitUntil(new AbstractWaitCondition() {

			@Override
			public boolean test() {
				return tIList.stream().peek(ti -> LOGGER.debug(ti.getText()))
						.filter(ti -> ti.getText().contains("createHelloMessage")).findFirst().isPresent();
			}
		}, TimePeriod.LONG);

		createHelloMessageDebugItem = tIList.stream().peek(ti -> LOGGER.debug(ti.getText()))
				.filter(ti -> ti.getText().contains("createHelloMessage")).findFirst().get();
		// select the item and return it
		createHelloMessageDebugItem.select();
		return createHelloMessageDebugItem;

	}

	private TreeItem getSuspendedThreadTreeItem(LaunchView debugView) {
		// get top item
		debugView.activate();
		DefaultTree parent = new DefaultTree();
		TreeItem remoteDebuggerTreeItem = parent.getItems().stream().filter(containsStringPredicate("Remote debugger"))
				.findFirst().get();

		List<TreeItem> items = remoteDebuggerTreeItem.getItems();
		TreeItem openJDKTreeItem = items.get(0);

		// this could (and will) change when run with another JDK - need
		// investigation
		assertTrue(openJDKTreeItem.getText().contains("OpenJDK"));

		// wait until we can see the suspended thread
		SuspendedTreeItemIsReady suspendedTreeItemIsReady = new SuspendedTreeItemIsReady(openJDKTreeItem);
		new WaitUntil(suspendedTreeItemIsReady, TimePeriod.VERY_LONG);
		return suspendedTreeItemIsReady.getSuspendedTreeItem();
	}

	private Predicate<TreeItem> containsStringPredicate(String string) {
		return treeItem -> treeItem.getText().contains(string);
	}

	private void checkVariablesView() {
		VariablesView variablesView = new VariablesView();
		variablesView.open();
		// wait for variables to have correct value
		new WaitUntil(new AbstractWaitCondition() {

			@Override
			public boolean test() {
				return variablesView.getValue("name").equals("World");
			}
		});
		new WaitUntil(new AbstractWaitCondition() {

			@Override
			public boolean test() {
				return variablesView.getValue("this").contains("HelloService");
			}
		});
	}

	private void triggerDebugSession() {
		Thread thread = new Thread(new Runnable() {
			@SuppressWarnings("resource")
			public void run() {
				Scanner scanner = null;
				InputStream inputStream = null;
				try {
					requestResponse = null;
					URL url = new URL(String.format("http://eap-app-%s.%s.nip.io/HelloWorld",
							projectReq.getProjectName(), getIpFromHostURL(connectionReq.getHost())));
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.connect();
					while (connection.getResponseCode() == 404 || connection.getResponseCode() == 503
							|| connection.getResponseCode() == 500) {
						connection.disconnect();
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();
					}
					inputStream = connection.getInputStream();
					scanner = new Scanner(inputStream).useDelimiter("\\A");
					requestResponse = scanner.next();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					scanner.close();
				}
			}
		});
		thread.start();
	}

	private String getIpFromHostURL(String host) {
		String[] split = host.split(":");
		return split[1].substring(2);
	}

	private ProjectItem getHelloServiceFile() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		Project project = projectExplorer.getProject(PROJECT_NAME);
		ProjectItem helloServiceFile = project.getProjectItem("Java Resources", "src/main/java",
				"org.jboss.as.quickstarts.helloworld", "HelloService.java");
		return helloServiceFile;
	}

	// Sets breakpoint to first appearance of given text.
	private void setBreakpointToLineWithText(ProjectItem file, String text) {
		file.open();
		TextEditor textEditor = new TextEditor("HelloService.java");
		textEditor.setCursorPosition(textEditor.getPositionOfText(text));
		new ShellMenuItem("Run", "Toggle Breakpoint").select();
	}

	private static void createServerAdapter() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName()).refresh();
		explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName()).getServicesWithName(OpenShiftResources.EAP_SERVICE).get(0)
				.createServerAdapter();
	}

	private static void disableShowConsoleWhenOutputChanges() {
		ConsoleView consoleView = new ConsoleView();
		consoleView.open();

		new WaitUntil(new ShowConsoleOutputToolItemIsAvailable(), TimePeriod.VERY_LONG);

		DefaultToolItem showConsoleOnChange = new DefaultToolItem(new WorkbenchShell(),
				"Show Console Output When Standard Out Changes");
		showConsoleOnChange.click();
	}

	// TODO this should be replaced once
	// https://github.com/jboss-reddeer/reddeer/issues/1668 is fixed.
	private void setNewVariableValue(String newValue, final String... variablePath) {
		new WaitWhile(new JobIsRunning());
		LaunchView debugView = new LaunchView();
		debugView.open();

		ensureCorrectFrameIsSelected(debugView);

		VariablesView variablesView = new VariablesView();
		variablesView.open();

		new WaitUntil(new AbstractWaitCondition() {

			@Override
			public boolean test() {
				try {
					TreeItem variable = new DefaultTreeItem(variablePath);
					variable.select();
					return variable.isSelected();
				} catch (Exception e) {
					return false;
				}
			}

			@Override
			public String description() {
				return "Variable is not selected";
			}
		}, TimePeriod.LONG);

		try {
			new ContextMenuItem("Change Value...").select();
		} catch (CoreLayerException e) {
			throw e;
		}
		new DefaultShell("Change Object Value");
		new DefaultStyledText().setText(newValue);
		new OkButton().click();

		new WaitWhile(new JobIsRunning());
	}

	private static class ShowConsoleOutputToolItemIsAvailable extends AbstractWaitCondition {
		@Override
		public boolean test() {
			try {
				new DefaultToolItem(new WorkbenchShell(), "Show Console Output When Standard Out Changes");
				return true;
			} catch (CoreLayerException ex) {
				return false;
			}
		}
	}
}
