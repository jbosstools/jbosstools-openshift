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

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.ui.bot.test.utils.DownloadCDKRuntimesUtility;
import org.junit.Test;

/**
 * Class covers integration testing of download runtimes wizard
 * @author odockal
 *
 */
public class DownloadRuntimesWizardTest extends DownloadContainerRuntimeAbstractTest {

	private static final CDKVersion version = CDKVersion.CDK340;
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@Test
	public void validCredentialsValidationTest() {
		chooseWizardPage(CDKLabel.Server.SERVER_TYPE_GROUP, version.serverName());
		inicializeDownloadRutimeDialog(version);
		DownloadCDKRuntimesUtility util = new DownloadCDKRuntimesUtility(true);
		util.chooseRuntimeToDownload(version);
		util.processCredentials(USERNAME, PASSWORD);
		util.acceptLicense();
		assertTrue(util.getDownloadWizard().getTitle().contains("Download Runtime"));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
		util.getDownloadWizard().cancel();
	}
	
	@Test
	public void invalidCredentialsTest() {
		chooseWizardPage(CDKLabel.Server.SERVER_TYPE_GROUP, version.serverName());
		inicializeDownloadRutimeDialog(version);
		DownloadCDKRuntimesUtility util = new DownloadCDKRuntimesUtility(true);
		util.chooseRuntimeToDownload(version);
		util.processCredentials("invalidUsername", "invalidPassword");
		assertTrue(util.getDownloadWizard().getTitle().contains("Credentials"));
		assertTrue(util.getDownloadWizard().getMessage().contains("Your credentials have failed to validate"));
		assertFalse(util.getDownloadWizard().isNextEnabled());
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG, false);
		util.getDownloadWizard().cancel();
	}
}
