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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.perspectives.DebugPerspective;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.Tree;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.condition.EditorWithTitleIsActive;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.reddeer.workbench.impl.view.WorkbenchView;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.reddeer.condition.ServerAdapterExists;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author psrna
 *
 */
@OpenPerspective(DebugPerspective.class)
@RunWith(RedDeerSuite.class)
@OCBinary
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.NODEJS_SERVICE, template = OpenShiftResources.NODEJS_TEMPLATE)
public class NodeJSAppDebugTest extends AbstractTest {

	@InjectRequirement
	private OpenShiftProjectRequirement requiredProject;
	@InjectRequirement
	private OpenShiftConnectionRequirement requiredConnection;
	@InjectRequirement
	private OpenShiftServiceRequirement requiredService;
	private OpenShift3Connection connection;
	private OpenShiftProject project;
	private ServerAdapter adapter;

	private static int BREAKPOINT_LINE = 85;
	private static String APP_URL = "http://nodejs-example-test-project.rhel-cdk.10.1.2.2.xip.io/pagecount";
	private static String VAR_db = "db";
	private static String VAR_VALUE_db = "null";
	private static String VAR_initDb = "initDb";
	private static String VAR_VALUE_initDb = "[Function]";
	private static String VAR_require = "require";
	private static String VAR_VALUE_require = "[Function]";

	@Before
	public void setUp() {
		TestUtils.setUpOcBinary();
		TestUtils.cleanupGitFolder(OpenShiftResources.NODEJS_GIT_NAME);
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		this.connection = explorer.getOpenShift3Connection(requiredConnection.getConnection());
		this.project = connection.getProject(requiredProject.getProjectName());
		this.project.expand();

		// import application
		this.project.getService(OpenShiftResources.NODEJS_SERVICE).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new DefaultShell(OpenShiftLabel.Shell.IMPORT_APPLICATION);
		new FinishButton().click();
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));

		// setup server adapter
		explorer.activate();
		this.project.getService(OpenShiftResources.NODEJS_SERVICE).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_ADAPTER_FROM_EXPLORER).select();
		new DefaultShell(OpenShiftLabel.Shell.SERVER_ADAPTER_SETTINGS);
		new FinishButton().click();

		new WaitWhile(new ShellIsAvailable(""), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);

		assertTrue("OpenShift 3 server adapter was not created.",
				new ServerAdapterExists(Version.OPENSHIFT3, OpenShiftResources.NODEJS_SERVICE, "Service").test());

		// restart in debug
		this.adapter = new ServerAdapter(Version.OPENSHIFT3, OpenShiftResources.NODEJS_SERVICE, "Service");
		this.adapter.select();
		new ContextMenuItem("Restart in Debug").select();
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);

	}

	@After
	public void clean() {
		ProjectExplorer pe = new ProjectExplorer();
		pe.open();
		pe.deleteAllProjects();
		try {
			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
			new ServerAdapter(Version.OPENSHIFT3, OpenShiftResources.NODEJS_SERVICE).delete();
		} catch (OpenShiftToolsException ex) {
			// do nothing, adapter does not exists
		}
	}

	@Test
	public void testDebuggerStopsAtBreakpoint() throws CoreException, IOException {
		ProjectExplorer pe = new ProjectExplorer();
		pe.open();
		pe.getProject(OpenShiftResources.NODEJS_GIT_NAME).getProjectItem("server.js").open();
		new WaitUntil(new EditorWithTitleIsActive("server.js"));
		TextEditor editor = new TextEditor("server.js");
		setLineBreakpoint(editor, BREAKPOINT_LINE);
		triggerDebugSession();

		try {
			new WaitUntil(new EditorWithTitleIsActive("server.js"), TimePeriod.LONG);
			new WaitUntil(new CursorPositionIsOnLine(editor, BREAKPOINT_LINE));
		} catch (WaitTimeoutExpiredException e) {
			Assert.fail("Debugger hasn't stopped on breakpoint");
		}

	}

	@Test
	public void testVariablesAvailableInView() throws CoreException, IOException {
		ProjectExplorer pe = new ProjectExplorer();
		pe.open();
		pe.getProject(OpenShiftResources.NODEJS_GIT_NAME).getProjectItem("server.js").open();
		new WaitUntil(new EditorWithTitleIsActive("server.js"));
		TextEditor editor = new TextEditor("server.js");
		setLineBreakpoint(editor, BREAKPOINT_LINE);
		triggerDebugSession();

		// test couple of js variables
		TreeItem var_db = getVariable(VAR_db);
		TreeItem var_initDb = getVariable(VAR_initDb);
		TreeItem var_require = getVariable(VAR_require);

		assertTrue("Variable '" + VAR_db + "' not found in view!", var_db != null);
		assertThat(var_db.getCell(0), is(VAR_db));
		assertThat(var_db.getCell(1), containsString(VAR_VALUE_db));

		assertTrue("Variable '" + VAR_initDb + "' not found in view!", var_initDb != null);
		assertThat(var_initDb.getCell(0), is(VAR_initDb));
		assertThat(var_initDb.getCell(1), containsString(VAR_VALUE_initDb));

		assertTrue("Variable '" + VAR_require + "' not found in view!", var_require != null);
		assertThat(var_require.getCell(0), is(VAR_require));
		assertThat(var_require.getCell(1), containsString(VAR_VALUE_require));
	}

	private void setLineBreakpoint(TextEditor editor, int lineNumber) throws CoreException {
		editor.activate();
		IResource resource = (IResource) editor.getEditorPart().getEditorInput().getAdapter(IResource.class);
		JavaScriptDebugModel.createLineBreakpoint(resource, lineNumber, -1, -1, new HashMap<String, Object>(), true);
	}

	/**
	 * getVariable from Variables view.
	 * 
	 * @param variable
	 *            name
	 * @return TreeItem
	 */
	protected TreeItem getVariable(String name) {

		WorkbenchView variables = new WorkbenchView("Variables");
		variables.activate();
		DefaultTree variablesTree = new DefaultTree();

		TreeItem var = null;
		try {
			new WaitUntil(new TreeContainsItem(variablesTree, new WithTextMatcher(name), false));
		} catch (WaitTimeoutExpiredException e) {
			// not found
			return null;
		}
		List<TreeItem> vars = variablesTree.getItems();
		for (TreeItem i : vars) {
			if (i.getText().equals(name)) {
				var = i;
			}
		}
		return var;
	}

	private void triggerDebugSession() {
		new Thread(new Runnable() {
			public void run() {
				try {
					URL url = new URL(APP_URL);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.connect();
					connection.getResponseCode();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public class CursorPositionIsOnLine extends AbstractWaitCondition {

		private TextEditor editor;
		private int line;

		/**
		 * Constructs CursorPositionIsOnLine wait condition. Condition is met
		 * when the specified editor has cursor set on a line specified in
		 * param.
		 * 
		 * @param editor
		 *            editor where to look for the cursor position
		 * @param line
		 *            - line counting starts from 1
		 */
		public CursorPositionIsOnLine(TextEditor editor, int line) {
			this.editor = editor;
			this.line = line;
		}

		@Override
		public boolean test() {
			Point p = editor.getCursorPosition();
			if (p.x + 1 == line) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public String description() {
			return "editor cursor position is on line '" + line;
		}

	}

	public class TreeContainsItem extends AbstractWaitCondition {

		private Tree tree;
		@SuppressWarnings("rawtypes")
		private Matcher matcher;
		private boolean recursive = true;

		/**
		 * Constructs TreeContainsItem wait condition. Condition is met when the
		 * specified tree contains the tree item with specified text.
		 * 
		 * @param tree
		 *            tree where to look for an item
		 * @param matcher
		 *            to match the text of the item
		 */
		@SuppressWarnings("rawtypes")
		public TreeContainsItem(Tree tree, Matcher matcher) {
			this.tree = tree;
			this.matcher = matcher;
		}

		@SuppressWarnings("rawtypes")
		public TreeContainsItem(Tree tree, Matcher matcher, boolean recursive) {
			this.tree = tree;
			this.matcher = matcher;
			this.recursive = recursive;
		}

		@Override
		public boolean test() {
			try {
				List<TreeItem> list = recursive ? tree.getAllItems() : tree.getItems();
				for (TreeItem i : list) {
					if (matcher.matches(i.getText())) {
						return true;
					}
				}
			} catch (CoreLayerException e) {
				return false;
			}
			return false;
		}

		@Override
		public String description() {
			return "tree contains item '" + matcher.toString();
		}
	}

}
