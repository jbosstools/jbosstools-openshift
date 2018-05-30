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
package org.jboss.tools.cdk.ui.bot.test.utils;

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.cdk.reddeer.core.condition.ProgressBarIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.download.DownloadCDKRuntimesWizard;
import org.jboss.tools.runtime.reddeer.wizard.TaskWizardFirstPage;
import org.jboss.tools.runtime.reddeer.wizard.TaskWizardLoginPage;
import org.jboss.tools.runtime.reddeer.wizard.TaskWizardSecondPage;
import org.jboss.tools.runtime.reddeer.wizard.TaskWizardThirdPage;

/**
 * Utility class covering processing Download CDK Runtime wizard
 * @author odockal
 *
 */
public class DownloadCDKRuntimesUtility {
	
	private WizardDialog downloadWizard;
	
	public WizardDialog getDownloadWizard() {
		return downloadWizard;
	}

	public void setDownloadWizard(WizardDialog downloadWizard) {
		this.downloadWizard = downloadWizard;
	}

	private String installFolder;
	private String downloadFolder;
	private boolean removeArtifacts;
	private boolean useDefaults;
	
	private static final Logger log = Logger.getLogger(DownloadCDKRuntimesUtility.class);
	
	public DownloadCDKRuntimesUtility(String installationFolder, 
			String downloadFolder, boolean removeArtifacts) {
		this.downloadWizard = new DownloadCDKRuntimesWizard();
		this.installFolder = installationFolder;
		this.downloadFolder = downloadFolder;
		this.removeArtifacts = removeArtifacts;
		this.useDefaults = false;
	}
	
	public DownloadCDKRuntimesUtility(boolean useDefaults) {
		this.downloadWizard = new DownloadCDKRuntimesWizard();
		this.installFolder = "";
		this.downloadFolder = "";
		this.removeArtifacts = true;
		this.useDefaults = useDefaults;
	}
	
	public void chooseRuntimeToDownload(CDKVersion version) {
		log.info("Choosing runtime to download: " + version.getTypeAndVersion());
		TaskWizardFirstPage page = new TaskWizardFirstPage(downloadWizard);
		page.selectRuntime(version.getTypeAndVersion());
		downloadWizard.next();
	}
	
	public void processCredentials(String username, String password) {
		log.info("Processing credentials page");
		TaskWizardLoginPage page = new TaskWizardLoginPage(downloadWizard);
		if (!page.containsUsername(username)) {
			log.info("Adding credentials");
			page.addCredentials(username, password);
		} else {
			page.setUsername(username);
		}
		downloadWizard.next();
		log.info("Wait for credentials being validated...");
		// here should have come validation of credentials waiting
		WaitCondition barIsRunning = new ProgressBarIsRunning("Validating Credentials");
		new WaitUntil(barIsRunning, TimePeriod.MEDIUM, false);
		new WaitWhile(barIsRunning, TimePeriod.LONG, false);
	}
	
	public void acceptLicense() {
		TaskWizardSecondPage licensePage = new TaskWizardSecondPage(downloadWizard);
		log.info("Accept license");
		licensePage.acceptLicense(true);
		downloadWizard.next();
	}
	
	public void downloadRuntime() {
		log.info("Setting up download and install folder");
		if (!this.useDefaults) {
			TaskWizardThirdPage downloadPage = new TaskWizardThirdPage(downloadWizard);
			downloadPage.setInstallFolder(this.installFolder);
			downloadPage.setDownloadFolder(this.downloadFolder);
			downloadPage.setDeleteArchive(this.removeArtifacts);
		} else {
			log.info("Using default values");
			installFolder = new LabeledText(downloadWizard, "Install folder:").getText();
			downloadFolder = new LabeledText(downloadWizard, "Download folder:").getText();
			removeArtifacts = new CheckBox(downloadWizard, "Delete archive after installing").isChecked();
		}
		
		downloadWizard.finish();
		downloadWizard = null;
	}

	public boolean isUseDefaults() {
		return useDefaults;
	}

	public void setUseDefaults(boolean useDefaults) {
		this.useDefaults = useDefaults;
	}

	public String getInstallFolder() {
		return installFolder;
	}

	public void setInstallFolder(String installFolder) {
		this.installFolder = installFolder;
	}

	public String getDownloadFolder() {
		return downloadFolder;
	}

	public void setDownloadFolder(String downloadFolder) {
		this.downloadFolder = downloadFolder;
	}

	public boolean isRemoveArtifacts() {
		return removeArtifacts;
	}

	public void setRemoveArtifacts(boolean removeArtifacts) {
		this.removeArtifacts = removeArtifacts;
	}
}
