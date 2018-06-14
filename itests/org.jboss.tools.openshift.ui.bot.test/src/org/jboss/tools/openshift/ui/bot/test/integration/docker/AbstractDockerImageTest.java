/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.integration.docker;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.matcher.RegexMatcher;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.ui.browser.BrowserEditor;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.condition.TreeContainsItem;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.core.StringContains;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.widget.ShellWithButton;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;

/**
 * 
 * @author jkopriva@redhat.com
 */
public class AbstractDockerImageTest extends AbstractTest {

	private static DockerExplorerView dockerExplorer;

	private static final String connectionsURI = System.getProperty("use.cdk").equals("true")?System.getProperty("openshift.server").replaceAll("\\:[0-9]*$",
			":2376"):"http://127.0.0.1:2375";
	private static final String pathToCertificate = System.getProperty("user.home") + "/.minishift/certs";
	protected static String DOCKER_CONNECTION = connectionsURI;

	public static void createDockerConnection() {
		dockerExplorer = new DockerExplorerView();
		dockerExplorer.open();
		//TODO will be replaced with CleanDockerExplorerRequirement when it will be available in docker.reddeer
		List<String> connectionsNames = dockerExplorer.getDockerConnectionNames();
		if (!connectionsNames.isEmpty()) {
			for (String connectionName : connectionsNames) {
				DockerConnection dockerConnection = dockerExplorer.getDockerConnectionByName(connectionName);
				if (dockerConnection != null) {
					dockerConnection.removeConnection();
				}
			}
		}
		if (System.getProperty("use.cdk").equals("true")) {
			dockerExplorer.createDockerConnectionURI(connectionsURI, connectionsURI, pathToCertificate);
		}else {
			dockerExplorer.createDockerConnectionURI(connectionsURI, connectionsURI, null);
		}
	}

	/**
	 * Opens a Deploy Image to OpenShift wizard from context menu of a docker image
	 */
	protected void openDeployToOpenShiftWizardFromDockerExplorer(String imageName, String imageTag) {
		dockerExplorer.getDockerConnectionByName(DOCKER_CONNECTION).getImage(imageName, imageTag).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DEPLOY_TO_OPENSHIFT).select();

		new DefaultShell(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT);
	}

	/**
	 * Closes Deploy Image to OpenShift wizard.
	 */
	protected void closeWizard() {
		new CancelButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT));
	}

	/**
	 * Selects project in OpenShift explorer view.
	 * 
	 * @param projectName
	 * @param projectDisplayName
	 */
	protected void selectProject(String projectName, OpenShiftConnectionRequirement openshiftConnectionRequirement) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection(openshiftConnectionRequirement.getConnection()).refresh();
		explorer.getOpenShift3Connection(openshiftConnectionRequirement.getConnection()).getProject(projectName)
				.select();
	}

	protected String getRouteURL(String routeName, String projectName,
			OpenShiftConnectionRequirement openshiftConnectionRequirement) {
		String url = "";
		url = openshiftConnectionRequirement.getHost().replaceAll(".*\\/|\\:[0-9]*$", "");
		url = "http://" + routeName + "-" + projectName + "." + url + ".nip.io/";
		return url;
	}

	/**
	 * If hello world docker image does not exist, this method will pull it.
	 */
	protected static void pullImageIfDoesNotExist(String imageName, String imageTag) {
		DockerExplorerView dockerExplorer = new DockerExplorerView();
		DockerConnection dockerConnection = dockerExplorer.getDockerConnectionByName(DOCKER_CONNECTION);
		dockerConnection.getTreeItem().expand();
		new WaitWhile(new JobIsRunning());
		new WaitWhile(new TreeContainsItem(dockerConnection.getTreeItem().getParent(),
				dockerConnection.getTreeItem().getText(), "Loading..."), TimePeriod.LONG);

		if (dockerConnection.getImage(imageName, imageTag) == null) {
			dockerConnection.pullImage(imageName, imageTag);
		}
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	protected static void createProject(String projectName,
			OpenShiftConnectionRequirement openshiftConnectionRequirement) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		OpenShift3Connection connection = explorer
				.getOpenShift3Connection(openshiftConnectionRequirement.getConnection());
		connection.createNewProject(projectName);
		connection.refresh();
	}

	/**
	 * Proceeds through the image if the first wizard page has correct details -
	 * connection, project and image name.
	 */
	protected void proceedThroughDeployImageToOpenShiftWizard() {
		new WaitUntil(new ControlIsEnabled(new NextButton()), TimePeriod.DEFAULT, false);

		assertTrue("Next button should be enabled if all details are set correctly", new NextButton().isEnabled());

		new NextButton().click();

		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);

		new NextButton().click();

		if (!new CheckBox("Add Route").isChecked()) {
			new CheckBox("Add Route").click();
		}

		new FinishButton().click();

		new ShellWithButton("Deploy Image to OpenShift", "OK");
		new OkButton().click();

		new WaitWhile(new ShellIsAvailable("Deploy Image to OpenShift"), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	/**
	 * Verifies whether an application pod has been created and application is
	 * running successfully.
	 */
	protected void verifyDeployedDockerImageInBrowser(String projectName, String podName, String expectedTextInBrowser,
			OpenShiftConnectionRequirement openshiftConnectionRequirement) {
		new OpenShiftExplorerView().getOpenShift3Connection(openshiftConnectionRequirement.getConnection()).refresh();
		try {
			new WaitUntil(new OpenShiftResourceExists(Resource.POD, new StringContains(podName), ResourceState.RUNNING,
					projectName, openshiftConnectionRequirement.getConnection()), TimePeriod.VERY_LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("There should be a running application pod for a deployed docker image, " + "but it does not exist.");
		}
		new OpenShiftExplorerView().getOpenShift3Connection(openshiftConnectionRequirement.getConnection()).refresh();
		new OpenShiftExplorerView().getOpenShift3Connection(openshiftConnectionRequirement.getConnection())
				.getProject(projectName).getOpenShiftResources(Resource.ROUTE).get(0).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.SHOW_IN_BROWSER).select();
		try {
			new WaitUntil(new BrowserContainsText(expectedTextInBrowser), TimePeriod.VERY_LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Browser does not contain text:" + expectedTextInBrowser);
		}
	}

	public static void closeBrowser() {
		try {
			BrowserEditor browser = new BrowserEditor(new RegexMatcher(".*"));
			browser.close();
		} catch (RedDeerException ex) {
			// do nothing, browser is not opened
		}
	}
}
