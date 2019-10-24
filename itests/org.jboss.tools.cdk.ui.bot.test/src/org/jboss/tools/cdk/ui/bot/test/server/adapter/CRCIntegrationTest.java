/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.server.adapter;

import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.requirements.securestorage.SecureStorageRequirement.DisableSecureStorage;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCRCServerWizardPage;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case covering basic CRC server adapter operations.
 * @author odockal
 *
 */
@DisableSecureStorage
@CleanOpenShiftExplorer
@RemoveCDKServers
public class CRCIntegrationTest extends CDKServerAdapterAbstractTest {

	private static final Logger log = Logger.getLogger(CRCIntegrationTest.class);
	
	private static final String OPENSHIFT_CONNECTION = "developer";
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_CRC;
	}
	
	@BeforeClass
	public static void setupServerAdapter() {
		NewCDKServerWizard dialog = CDKUtils.openNewServerWizardDialog();
		try {
			NewServerWizardPage page = new NewServerWizardPage(dialog);
			page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, CDKLabel.Server.CRC_SERVER_NAME);
			page.setName(SERVER_ADAPTER_CRC);
			if (!dialog.isNextEnabled()) {
				throw new CDKServerException("Cannot create " + CDKLabel.Server.CRC_SERVER_NAME + " server adapter due to: " + dialog.getMessage());
			}
			dialog.next();
			NewCRCServerWizardPage containerPage = new NewCRCServerWizardPage(dialog);
			containerPage.setCRCBinary(CRC_BINARY);
			// here comes possibility to set profile while creating server adapter
			log.info("Setting secret pull file to: ");
			containerPage.setCRCPullServerFile(CRC_SECRET_FILE);
			new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);
			new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.MEDIUM, false);
			dialog.finish(TimePeriod.MEDIUM);
		} catch (RedDeerException coreExc) {
			new CancelButton().click();
			throw new CDKServerException("Exception occured in CDK server wizard, wizard was canceled", coreExc);
		}
	}

	@Test
	public void testCRCServerAdapter() {
		startServerAdapter(getCDKServer(), () -> {}, false);
		CDKTestUtils.verifyConsoleContainsRegEx("\\bThe OpenShift cluster is running\\b");
		OpenShift3Connection connection = null;
		try {
			connection = CDKTestUtils.findOpenShiftConnection(null, OPENSHIFT_CONNECTION);
		} catch (OpenShiftToolsException toolsExc) {
			fail(toolsExc.getMessage());
		}
		connection.select();
		handleSSLDialog();
		connection.refresh(TimePeriod.getCustom(120));
		connection.expand();
		CDKTestUtils.testOpenshiftConnection(connection);
		// should be addad after crc ga
		restartServerAdapter(getCDKServer());
		CDKTestUtils.testOpenshiftConnection(null, OPENSHIFT_CONNECTION);
		stopServerAdapter(getCDKServer());
	}	
	
	
	private void handleSSLDialog() {
		try {
			DefaultShell certificateDialog = new DefaultShell(CDKLabel.Shell.UNTRUSTED_SSL_DIALOG);
			certificateDialog.setFocus();
			log.info("SSL Certificate Dialog appeared"); 
			new PushButton(certificateDialog, CDKLabel.Buttons.YES).click(); 
			new WaitWhile(new ShellIsAvailable(certificateDialog));
		} catch (WaitTimeoutExpiredException ex) {
			String message ="WaitTimeoutExpiredException occured when handling Certificate dialog.";
			log.debug(message);
			throw new CDKServerException(message, ex);
		} catch (CoreLayerException exc) {
			String message ="CoreLayerexception occured when handling Certificate dialog." 
					+ "Dialog has not been shown"; 
			log.debug(message);
		}		
	}
}
