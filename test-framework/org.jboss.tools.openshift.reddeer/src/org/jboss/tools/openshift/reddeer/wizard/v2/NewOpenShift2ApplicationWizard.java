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
package org.jboss.tools.openshift.reddeer.wizard.v2;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.YesButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.condition.ServerAdapterExists;
import org.jboss.tools.openshift.reddeer.condition.v2.OpenShiftApplicationExists;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;
import org.jboss.tools.openshift.reddeer.wizard.AbstractOpenShiftApplicationWizard;
import org.jboss.tools.openshift.reddeer.wizard.page.v2.FirstWizardPage;
import org.jboss.tools.openshift.reddeer.wizard.page.v2.FourthWizardPage;
import org.jboss.tools.openshift.reddeer.wizard.page.v2.SecondWizardPage;
import org.jboss.tools.openshift.reddeer.wizard.page.v2.ThirdWizardPage;

/**
 * Creating application consist of 3 required steps:
 * - opening new application wizard.
 * - proceed through wizard and set up details
 * - post create steps (accept ssh host key, embedded cartridges dialog...)
 * 
 * @author mlabuda@redhat.com
 *
 */
public class NewOpenShift2ApplicationWizard extends AbstractOpenShiftApplicationWizard {	
	
	private String domain;
	
	public NewOpenShift2ApplicationWizard(String username, String server, String domain) {
		super(server, username);
		this.domain = domain;
	}
	
	/**
	 * Opens new application wizard from OpenShift explorer.
	 */
	public void openWizardFromExplorer() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		// workaround
		explorer.reopen();

		explorer.getOpenShift2Connection(username, server).getDomain(domain).select();
		
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_OS2_APPLICATION).select();
		
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD),
				TimePeriod.LONG);

		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD).setFocus();
	}
	
	public void importExistingApplication(String existingAppName,
			boolean createAdapter, String gitDestination) {
		FirstWizardPage firstPage = new FirstWizardPage();
		firstPage.importExistingApplication(domain, existingAppName);

		next();
		
		ThirdWizardPage thirdPage = new ThirdWizardPage();
		thirdPage.configureProjectAndServerAdapter(true, null, false);
		
		next();
		
		FourthWizardPage fourthPage = new FourthWizardPage();
		fourthPage.setGitDestination(gitDestination);
		
		finish();
	}
	
	public void createNewApplicationOnBasicCartridge(String cartridge, String appName, 
			boolean scalable, boolean smallGear, boolean createEnvironmentVariable, boolean disableMvnBuild,
			String sourceCodeURL, String embeddedCartridgeURL, boolean createAdapter,
			String deployProject, String gitDestination, String gitRemoteName, 
			String... embeddedCartridges) {
		
		FirstWizardPage first = new FirstWizardPage();
		first.createNewApplicationOnBasicCartridge(cartridge);
		
		next();
		
		proceedThroughWizard(appName, scalable, smallGear, createEnvironmentVariable, 
				sourceCodeURL, embeddedCartridgeURL, createAdapter, deployProject, disableMvnBuild, 
				gitDestination, gitRemoteName, embeddedCartridges);
	}
	
	public void createQuickstart(String quickstart, String appName,
			boolean scalable, boolean smallGear, boolean createAdapter, 
			String gitDestination) {
		
		FirstWizardPage first = new FirstWizardPage();
		first.createQuickstart(quickstart);
		
		next();
		
		proceedThroughWizard(appName, scalable, smallGear, false, 
				null, null, createAdapter, null, false, 
				gitDestination, null, (String[]) null);
	}
	
	public void createNewApplicationOnDownloadableCartridge(String URL, String appName, 
			boolean scalable, boolean smallGear, boolean createEnvironmentVariable, boolean disableMvnBuild,
			String sourceCodeURL, String embeddedCartridgeURL, boolean createAdapter,
			String deployProject, String gitDestination, String gitRemoteName, 
			String... embeddedCartridges) {

		FirstWizardPage first = new FirstWizardPage();
		first.createNewApplicationFromDownloadableCartridge(URL);
	
		next();

		proceedThroughWizard(appName, scalable, smallGear, createEnvironmentVariable, 
				sourceCodeURL, embeddedCartridgeURL, createAdapter, deployProject, disableMvnBuild, 
				gitDestination, gitRemoteName, embeddedCartridges);
	}
	
	
	private void proceedThroughWizard(String appName, boolean scalable, boolean smallGear, 
			boolean createEnvironmentVariable, String sourceCodeURL, String embeddedCartridgeURL, 
			boolean createAdapter, String deployProject, boolean disableMvnBuild, String gitDestination, 
			String gitRemoteName, String... embeddedCartridges) {
		
		SecondWizardPage secondPage = new SecondWizardPage();
		secondPage.fillApplicationDetails(domain, appName, scalable, smallGear, createEnvironmentVariable, 
				sourceCodeURL);
		secondPage.addCartridges(embeddedCartridges);
		secondPage.addCodeAnythingCartridge(embeddedCartridgeURL);
		
		next();
		
		ThirdWizardPage thirdPage = new ThirdWizardPage();
		thirdPage.configureProjectAndServerAdapter(createAdapter, deployProject, disableMvnBuild);
		
		next();
		
		FourthWizardPage fourthPage = new FourthWizardPage();
		fourthPage.setGitDestination(gitDestination);
		fourthPage.setGitRemoteName(gitRemoteName);
		
		finish();
	}
	
	/**
	 * @param cartridgeIsEmbedded true if embeddable cartridges is added, false otherwise
	 * @param acceptHostKey set true for accepting SSH host key, false otherwise
	 */
	public void postCreateSteps(boolean cartridgeIsEmbedded) {
		if (cartridgeIsEmbedded) {
			new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.EMBEDDED_CARTRIDGE), 
					TimePeriod.VERY_LONG);
		
			new DefaultShell(OpenShiftLabel.Shell.EMBEDDED_CARTRIDGE);
			new OkButton().click();
		}
		
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.ACCEPT_HOST_KEY), TimePeriod.VERY_LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.ACCEPT_HOST_KEY);
		
		new YesButton().click();

		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD),
				TimePeriod.VERY_LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
	}
			
	/**
	 * Verify that application exists on OpenShift and that there is a server 
	 * adapter for application.
	 * 
	 * @param appName application name
	 * @param project project deployed on OpenShift shown in ServersView
	 */
	public void verifyApplication(String appName, String project) {
		try {
			new WaitUntil(new OpenShiftApplicationExists(username, server, domain, appName), TimePeriod.NORMAL);
		} catch (WaitTimeoutExpiredException ex) {
			fail("OpenShift 2 application has not been created successfully, or it is not at least shown"
					+ " in OpenShift explorer view under specific domain.");
		}		
	}	
	
	public void verifyServerAdapter(String appName, String project) {
		assertTrue("There is no server adapter for application " + appName + " and project " + project,
				new ServerAdapterExists(Version.OPENSHIFT2, appName).test());
	}
}
