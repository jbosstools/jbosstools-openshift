/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.reddeer.junit.execution.annotation.RunIf;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.api.CTabFolder;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.ctab.DefaultCTabFolder;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.wizard.v3.BasicAuthenticationSection;
import org.jboss.tools.openshift.reddeer.wizard.v3.OAuthauthenticationSection;
import org.jboss.tools.openshift.reddeer.wizard.v3.OpenShift3ConnectionWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Implements JBIDE-24933
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@CleanOpenShiftExplorer
public class SetOCForNewConnectionTest extends AbstractTest {

	private static final String OC_URL_LINUX = "https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz";
	private static final String OC_URL_WINDOWS = "https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-windows.zip";
	private static final String OC_URL_MAC = "https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-mac.zip";
	private static final String PROPERTY_OC_LOCATION = "OC Client";
	private String pathToOC;
	Path ocTestDirectory = Paths.get("target", "octest");
	
	public String downloadOCBinary(boolean latestOC, String destinationDirectory) {
		String url = OpenShiftCommandLineToolsRequirement.getDownloadLink(latestOC);
		File downloadedOCBinary = OpenShiftCommandLineToolsRequirement.downloadAndExtractOpenShiftClient(url,
				destinationDirectory);
		pathToOC = OpenShiftCommandLineToolsRequirement.copyOCFromExtractedFolder(downloadedOCBinary,
				ocTestDirectory);
		return pathToOC;
	}

	public String downloadOCBinary(String url, String destinationDirectory) {
		File downloadedOCBinary = OpenShiftCommandLineToolsRequirement.downloadAndExtractOpenShiftClient(url,
				destinationDirectory);
		pathToOC = OpenShiftCommandLineToolsRequirement.copyOCFromExtractedFolder(downloadedOCBinary,
				ocTestDirectory);
		return pathToOC;
	}

	private String getOCUrl() {
		String url;
		if (Platform.OS_LINUX.equals(Platform.getOS())) {
			url = OC_URL_LINUX;
		} else if (Platform.getOS().startsWith(Platform.OS_WIN32)
				&& Platform.getOSArch().equals(Platform.ARCH_X86_64)) {
			url = OC_URL_WINDOWS;
		} else {
			url = OC_URL_MAC;
		}
		return url;
	}

	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3BasicConnectionOCLatest() {
		String ocClientPath = downloadOCBinary(true, ocTestDirectory.toString());
		setBasicConnection(ocClientPath);
		checkOCLocation(ocClientPath);
		checkConnectionIsWorking();
	}

	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3BasicConnectionOC370() {
		String ocClientPath = downloadOCBinary(getOCUrl(), ocTestDirectory.toString());
		setBasicConnection(ocClientPath);
		checkOCLocation(ocClientPath);
		checkConnectionIsWorking();
	}

	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3OAuthConnectionOCLatest() {
		downloadOCBinary(true, ocTestDirectory.toString());
		setOAuthConnection(pathToOC);
		checkOCLocation(pathToOC);
		checkConnectionIsWorking();
	}
	
	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3OAuthConnectionOC370() {
		String ocClientPath = downloadOCBinary(getOCUrl(), ocTestDirectory.toString());
		setOAuthConnection(ocClientPath);
		checkOCLocation(ocClientPath);
		checkConnectionIsWorking();
	}

	private void checkOCLocation(String expectedLocation) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();

		OpenShift3Connection connection = explorer.getOpenShift3Connection(DatastoreOS3.SERVER, DatastoreOS3.USERNAME);
		connection.select();

		PropertiesOverload propertiesView = new PropertiesOverload();
		propertiesView.open();
//		propertiesView.getContextMenu().getItem("Detach");
		new ContextMenuItem("Detach").select();
		DefaultShell shell = new DefaultShell();
		System.out.println(shell.getText());
		
		explorer.activate();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.PROPERTIES).select();
		
//		PropertiesOverload propertiesView = new PropertiesOverload();
		if (!propertiesView.isOpen()) {
			propertiesView.open();
		}
		propertiesView.activate();
		propertiesView.togglePinToSelection(true);
//		if (!propertiesView.isOpen()) {
//			propertiesView.open();
//		}
//		propertiesView.activate();
//		new DefaultToolItem(propertiesView.getCTabItem().getFolder(), "Pin to Selection").toggle(true);

		assertEquals(expectedLocation, propertiesView.getProperty(PROPERTY_OC_LOCATION).getPropertyValue());

		propertiesView.activate();
		new DefaultToolItem(propertiesView.getCTabItem().getFolder(), "Pin to Selection").toggle(false);
	}

	private void checkConnectionIsWorking() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();

		OpenShift3Connection connection = explorer.getOpenShift3Connection(DatastoreOS3.SERVER, DatastoreOS3.USERNAME);
		connection.select();

		connection.refresh();

		new WaitWhile(new JobIsRunning());
	}

	private void setBasicConnection(String ocPath) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		DatastoreOS3.AUTH_METHOD = AuthenticationMethod.BASIC;

		explorer.openConnectionShell();
		OpenShift3ConnectionWizard connectionWizard = new OpenShift3ConnectionWizard();
		connectionWizard.setServer(DatastoreOS3.SERVER);
		connectionWizard.switchAuthenticationSection(DatastoreOS3.AUTH_METHOD);

		BasicAuthenticationSection authSection = (BasicAuthenticationSection) connectionWizard.getAuthSection();
		authSection.setUsername(DatastoreOS3.USERNAME);
		authSection.setPassword(DatastoreOS3.PASSWORD);
		authSection.setSavePassword(false);

		connectionWizard.switchOverrideOC(true);
		connectionWizard.getOCLocationLabel().setText(ocPath);

		connectionWizard.finishAndHandleCertificate();

		assertTrue("Connection does not exist in OpenShift Explorer view",
				explorer.connectionExists(DatastoreOS3.USERNAME));
	}
	
	private void setOAuthConnection(String ocPath) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		DatastoreOS3.AUTH_METHOD = AuthenticationMethod.OAUTH;

		explorer.openConnectionShell();
		OpenShift3ConnectionWizard connectionWizard = new OpenShift3ConnectionWizard();
		connectionWizard.setServer(DatastoreOS3.SERVER);
		connectionWizard.switchAuthenticationSection(DatastoreOS3.AUTH_METHOD);

		OAuthauthenticationSection authSection = (OAuthauthenticationSection) connectionWizard.getAuthSection();
		authSection.setToken(DatastoreOS3.TOKEN);
		authSection.setSaveToken(false);

		connectionWizard.switchOverrideOC(true);
		connectionWizard.getOCLocationLabel().setText(ocPath);

		connectionWizard.finishAndHandleCertificate();

		assertTrue("Connection does not exist in OpenShift Explorer view",
				!explorer.getOpenShift3Connections().isEmpty());
	}

	@After
	public void cleanUpConnections() {
		ConnectionsRegistrySingleton.getInstance().clear();
	}
	
	private class PropertiesOverload extends PropertySheet {
		
		public void togglePinToSelection(boolean toggle){
			activate();
			
			DefaultToolItem item = new DefaultToolItem(cTabItem.getFolder(), "Pins this property view to the current selection");
			item.toggle(toggle);
		}
	}

}
