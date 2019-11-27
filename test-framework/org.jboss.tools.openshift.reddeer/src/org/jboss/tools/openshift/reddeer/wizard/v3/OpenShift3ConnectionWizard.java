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
package org.jboss.tools.openshift.reddeer.wizard.v3;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.matcher.WithClassNameMatcher;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.jface.dialogs.TitleAreaDialog;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * Represents OpenShift connection wizard (Edit or New)
 * 
 * @author odockal
 *
 */
public class OpenShift3ConnectionWizard {

	private DefaultShell shell;

	private AuthenticationMethodSection authSection = null;

	private static final Logger log = Logger.getLogger(OpenShift3ConnectionWizard.class);
	
	public OpenShift3ConnectionWizard() {
		new OpenShift3ConnectionWizard(OpenShiftLabel.Shell.NEW_CONNECTION);
	}

	public OpenShift3ConnectionWizard(String wizardName) {
		this.shell = new DefaultShell(wizardName);
		switchAuthenticationSection(getAuthenticationMethod().getText());
	}
	
	public void switchAuthenticationSection(AuthenticationMethod method) {
		switchAuthenticationSection(method.toString());
	}

	public void switchAuthenticationSection(String method) {
		switch (method.toLowerCase()) {
		case "basic": 
			setAuthMethodToBasic();
			break;
		case "oauth": 
			setAuthMethodToOAuth();
			break;
		default:
			log.info("Unknown auth method..."); 
		}
	}
	
	/**
	 * Uses active shell to create TitleAreaDialog instance and call getMessage
	 * method to obtain page description message
	 * @return page description message
	 */
	public String getConnectionMessage() {
		new WaitWhile(new JobIsRunning(), TimePeriod.MEDIUM, false);
		return new TitleAreaDialog(getShell()).getMessage();
	}

	/**
	 * Sets Combo selection based on given combo object and string selection value
	 * 
	 * @param combo
	 * @param selection
	 */
	public void setSelection(LabeledCombo combo, String selection) {
		combo.setSelection(selection);
	}

	/**
	 * General getter for Combo select object in shell defined with label
	 * 
	 * @param label
	 * @return DefaultCCombo object
	 */
	protected LabeledCombo getCombo(String label) {
		return new LabeledCombo(label);
	}

	public LabeledCombo getConnection() {
		return getCombo(OpenShiftLabel.TextLabels.CONNECTION);
	}

	public LabeledCombo getServer() {
		return getCombo(OpenShiftLabel.TextLabels.SERVER);
	}

	public LabeledCombo getAuthenticationMethod() {
		return getCombo(OpenShiftLabel.TextLabels.PROTOCOL);
	}

	public LabeledText getTokenLabel() {
		return new LabeledText(OpenShiftLabel.TextLabels.TOKEN);
	}

	public void selectAuthenticationMethod(String method) {
		setSelection(getAuthenticationMethod(), method);
	}

	public AuthenticationMethodSection getAuthSection() {
		return authSection;
	}

	private PushButton advancedToOpen() {
		try {
			return new PushButton(OpenShiftLabel.Button.ADVANCED_OPEN);
		} catch (CoreLayerException exc) {
			return null;
		}
	}

	public void openAdvancedSection() {
		PushButton advanced = advancedToOpen();
		if (advanced != null) {
			advanced.click();
		}
	}

	public void closeAdvancedSection() {
		PushButton advanced = advancedToOpen();
		if (advanced == null) {
			new PushButton(OpenShiftLabel.Button.ADVANCED_CLOSE).click();
		}
	}

	public LabeledText getImageRegistryUrl() {
		openAdvancedSection();
		return new LabeledText(OpenShiftLabel.TextLabels.IMAGE_REGISTRY_URL);
	}

	public LabeledText getClusterNamespace() {
		openAdvancedSection();
		return new LabeledText(OpenShiftLabel.TextLabels.CLUSTER_NAMESPACE);
	}

	public DefaultText getOCLocationLabel() {
		openAdvancedSection();
		if (getAuthenticationMethod().getSelection().equals(AuthenticationMethod.OAUTH.toString())) {
			return new DefaultText(3, new WithClassNameMatcher("org.eclipse.swt.widgets.Text"));
		} else {
			return new DefaultText(4, new WithClassNameMatcher("org.eclipse.swt.widgets.Text"));
		}
	}

	public CheckBox getOverrideOCLocationButton() {
		openAdvancedSection();
		return new CheckBox(OpenShiftLabel.TextLabels.OVERRIDE_OC_LOCATION);
	}

	public void switchOverrideOC(boolean checked) {
		getOverrideOCLocationButton().toggle(checked);
	}

	public PushButton getDiscoveryButton() {
		openAdvancedSection();
		return new PushButton(OpenShiftLabel.Button.DISCOVER);
	}
	
	public void setServer(String server) {
		new LabeledCombo(OpenShiftLabel.TextLabels.SERVER).setText(server);
	}

	/**
	 * Overwriting of URL requires user confirmation, Message Dialog is shown. If
	 * url is not filled in, no dialog pops up and url is generated automatically.
	 * If it is not possible to get proper registry url (ie. adapter is stopped),
	 * then Error dialog is thrown.
	 */
	public void discover() {
		Button discover = getDiscoveryButton();
		discover.click();
		try {
			new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.REGISTRY_URL_NOT_FOUND), TimePeriod.DEFAULT);
			// Error dialog appeared, exception will be thrown
			log.error("Unable to discover a registry URL"); 
			throw new OpenShiftToolsException(OpenShiftLabel.Shell.REGISTRY_URL_NOT_FOUND);
		} catch (WaitTimeoutExpiredException exc) {
			// Error dialog was not thrown
		}
		try {
			confirmMessageDialog();
		} catch (WaitTimeoutExpiredException exc) {
			// Confirmation dialog did not appear, only spotted reason so far is that
			// it would be replacing the same URL.
			log.info("Discover action did not invoke any dialog"); 
		}
	}

	/**
	 * Waits and clicks Cancel button .
	 */
	public void cancel() {
		new WaitUntil(new ControlIsEnabled(new CancelButton()), TimePeriod.LONG);

		new CancelButton().click();
		waitForShellToClose(getShell());
	}

	/**
	 * Waits and clicks Finish button .
	 */
	public void finish() {
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.LONG);

		new FinishButton().click();
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT, false);
		// check for Wizard closing dialog
		try {
			new WaitUntil(new ShellIsAvailable("Wizard closing"), TimePeriod.MEDIUM); 
			new OkButton().click();
			new WaitWhile(new ShellIsAvailable("Wizard closing"), TimePeriod.MEDIUM, false); 
			new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT, false);
			new FinishButton().click();
		} catch (WaitTimeoutExpiredException exc) {
			// dialog not shown, continue
		}
		waitForShellToClose(getShell());
	}
	
	public void finishAndHandleCertificate() {
		finishAndHandleCertificate(false);
	}
	
	public void finishAndHandleCertificate(boolean mustAcceptCert) {
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.LONG);

		new FinishButton().click();
		accceptCertificate(mustAcceptCert);
		//OpenShift4 have 2 certificates
		accceptCertificate(mustAcceptCert);

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_CONNECTION), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	private void accceptCertificate(boolean mustAcceptCert) {
		ShellIsAvailable shellCert = new ShellIsAvailable(OpenShiftLabel.Shell.UNTRUSTED_SSL_CERTIFICATE);
		new WaitUntil(shellCert, TimePeriod.DEFAULT, false);
		if (shellCert.getResult() != null) {
			new PushButton("Yes").click();
		} else if (mustAcceptCert) {
			throw new OpenShiftToolsException("Expected " + OpenShiftLabel.Shell.UNTRUSTED_SSL_CERTIFICATE + " dialog was not offered");
		}
	}

	public DefaultShell getShell() {
		return shell;
	}
	
	private void confirmMessageDialog() {
		ShellIsAvailable confirm = new ShellIsAvailable(OpenShiftLabel.Shell.OVERWRITE_REGISTRY_URL);
		new WaitUntil(confirm, TimePeriod.MEDIUM);
		DefaultShell dialog = new DefaultShell(OpenShiftLabel.Shell.OVERWRITE_REGISTRY_URL);
		dialog.setFocus();
		new OkButton().click();
		new WaitWhile(confirm, TimePeriod.MEDIUM);
	}

	private void waitForShellToClose(DefaultShell shellToClose) {
		new WaitWhile(new ShellIsAvailable(shellToClose), TimePeriod.DEFAULT);
		try {
			new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT);
		} catch (NoClassDefFoundError e) {
			// do nothing, reddeer.workbench plugin is not available
		}
	}

	private void setAuthMethodToBasic() {
		log.info("Setting auth method to Basic"); 
		authSection = new BasicAuthenticationSection();		
	}

	private void setAuthMethodToOAuth() {
		log.info("Setting auth method to OAuth"); 
		authSection = new OAuthauthenticationSection();		
	}
	
}
