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
package org.jboss.tools.cdk.ui.bot.test.server.wizard.download;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK3ServerWizardPage;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.reddeer.utils.DownloadCDKRuntimesUtility;
import org.junit.Test;

/**
 * Class covers integration testing of download runtimes wizard
 * @author odockal
 *
 */
public class DownloadRuntimesWizardTest extends DownloadContainerRuntimeAbstractTest {

	private static final CDKVersion version = CDKVersion.CDK370;
	
	private static final String INVALID_CREDENTIALS = "credentials have failed to validate";
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@Test
	public void validCredentialsValidationTest() {
		openNewServerWizardDialog(CDKLabel.Server.SERVER_TYPE_GROUP, version.serverName());
		NewCDK3ServerWizardPage wizardPage = CDKUtils.chooseCDKWizardPage(version, dialog);
		CDKUtils.initializeDownloadRutimeDialog(wizardPage);
		DownloadCDKRuntimesUtility util = new DownloadCDKRuntimesUtility(true);
		util.chooseRuntimeToDownload(version);
		util.processCredentials(USERNAME, PASSWORD);
		util.acceptLicense();
		assertTrue(util.getDownloadWizard().getTitle().contains("Download Runtime"));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
		cancelDialogAndWaitForRefreshingServers(util.getDownloadWizard());
	}
	
	@Test
	public void invalidCredentialsTest() {
		openNewServerWizardDialog(CDKLabel.Server.SERVER_TYPE_GROUP, version.serverName());
		NewCDK3ServerWizardPage wizardPage = CDKUtils.chooseCDKWizardPage(version, dialog);
		CDKUtils.initializeDownloadRutimeDialog(wizardPage);
		DownloadCDKRuntimesUtility util = new DownloadCDKRuntimesUtility(true);
		util.chooseRuntimeToDownload(version);
		util.processCredentials("invalidUsername", "invalidPassword");
		assertTrue("Does not contain word Credentials, but " + util.getDownloadWizard().getTitle(), 
				util.getDownloadWizard().getTitle().contains("Credentials"));
		String message = util.getDownloadWizard().getMessage();
		assertTrue("Dialog error message does not contain expected '" + INVALID_CREDENTIALS + "', but '" +
				message + "'",
				message.contains(INVALID_CREDENTIALS));
		assertFalse(util.getDownloadWizard().isNextEnabled());
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
		cancelDialogAndWaitForRefreshingServers(util.getDownloadWizard());
	}
	
	private void cancelDialogAndWaitForRefreshingServers(WizardDialog dialog) {
		try {
			dialog.cancel();
		} catch (WaitTimeoutExpiredException exc) {
			if (exc.getMessage().contains("Refreshing server adapter list")) {
				new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
			} else {
				throw exc;
			}
		}
	}
}
