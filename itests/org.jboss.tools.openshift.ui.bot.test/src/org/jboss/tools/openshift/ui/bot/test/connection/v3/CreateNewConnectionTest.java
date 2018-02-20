/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
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
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.execution.annotation.RunIf;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.Browser;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.label.DefaultLabel;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.common.reddeer.utils.StackTraceUtils;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.utils.AuthenticationTokenRetrival;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.EmulatedLinkStyledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.wizard.v3.AuthenticationMethodSection;
import org.jboss.tools.openshift.reddeer.wizard.v3.BasicAuthenticationSection;
import org.jboss.tools.openshift.reddeer.wizard.v3.OpenShift3ConnectionWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 */
@SuppressWarnings("unused")
@RunWith(RedDeerSuite.class)
@OpenPerspective(JBossPerspective.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@CleanOpenShiftExplorer
public class CreateNewConnectionTest extends AbstractTest {

	private static Logger LOG = new Logger(CreateNewConnectionTest.class);
	
	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3BasicConnection() {
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

		connectionWizard.finishAndHandleCertificate();				
		
		assertTrue("Connection does not exist in OpenShift Explorer view", 
				explorer.connectionExists(DatastoreOS3.USERNAME));
	}
	
	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3OAuthConnection() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		DatastoreOS3.AUTH_METHOD = AuthenticationMethod.OAUTH;
		
		explorer.openConnectionShell();
		try {
			explorer.connectToOpenShift(DatastoreOS3.SERVER, null, DatastoreOS3.TOKEN, false, false, AuthenticationMethod.OAUTH, true);
		} catch (RedDeerException ex) {
			fail("Creating an OpenShift v3 OAuth connection failed." + ex.getCause());
		}	
		assertTrue("Connection does not exist in OpenShift Explorer view", 
				explorer.connectionExists(DatastoreOS3.USERNAME));
	}
	
	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void shouldExtractTokenInBrowserWindow() {
		openConnectionWizardAndSetDefaultServerOAuth();

		String token = new AuthenticationTokenRetrival(DatastoreOS3.USERNAME, DatastoreOS3.PASSWORD).retrieveToken();

		String tokenText = new LabeledText(OpenShiftLabel.TextLabels.TOKEN).getText();

		assertEquals(token, tokenText);

		new CancelButton().click();
	}
	
	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void invalidRegistryURLShouldReportErrorMessage() {
		openConnectionWizardAndSetDefaultServer();
		new LabeledCombo(OpenShiftLabel.TextLabels.PROTOCOL).setSelection(AuthenticationMethod.BASIC.toString());
		new LabeledText(OpenShiftLabel.TextLabels.USERNAME).setText(DatastoreOS3.USERNAME);
		new LabeledText(OpenShiftLabel.TextLabels.PASSWORD).setText(DatastoreOS3.PASSWORD);
		new PushButton(OpenShiftLabel.Button.ADVANCED_OPEN).click();
		new LabeledText(OpenShiftLabel.TextLabels.IMAGE_REGISTRY_URL).setText("invalidURL");
		new WaitUntil(new ControlIsEnabled(new CancelButton()), TimePeriod.DEFAULT);
		new WaitUntil(new ControlIsEnabled(new DefaultText(" Please provide a valid image registry (HTTP/S) URL.")), TimePeriod.DEFAULT);
		new CancelButton().click();
	}

	private void openConnectionWizardAndSetDefaultServerOAuth() {
		openConnectionWizardAndSetDefaultServer();
		new LabeledCombo(OpenShiftLabel.TextLabels.PROTOCOL).setSelection(AuthenticationMethod.OAUTH.toString());
	}

	private void openConnectionWizardAndSetDefaultServer() {
		OpenShiftExplorerView openShiftExplorerView = new OpenShiftExplorerView();
		openShiftExplorerView.open();
		openShiftExplorerView.openConnectionShell();
		new DefaultShell(OpenShiftLabel.Shell.NEW_CONNECTION);
		new LabeledCombo(OpenShiftLabel.TextLabels.SERVER_TYPE)
				.setSelection(OpenShiftLabel.Others.OPENSHIFT3);
		new LabeledCombo(OpenShiftLabel.TextLabels.SERVER).setText(DatastoreOS3.SERVER);
	}
	
	@AfterClass
	public static void cleanUpAfterTest() {
		ConnectionsRegistrySingleton.getInstance().clear();
	}

}
