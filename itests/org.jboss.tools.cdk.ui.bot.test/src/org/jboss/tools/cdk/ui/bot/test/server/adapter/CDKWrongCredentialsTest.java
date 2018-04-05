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
package org.jboss.tools.cdk.ui.bot.test.server.adapter;

import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test class verifying erroneous situation when user passes wrong credentials into cdk
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CDKWrongCredentialsTest extends CDKServerAdapterAbstractTest {

	private static final String username = "myuser";
	
	private static final String password = "password";
	
	private static final String MINISHIFT_PROFILE_REG = "nonregistered";

	private static final Logger log = Logger.getLogger(CDKWrongCredentialsTest.class);
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@BeforeClass
	public static void setupCDKWrongCredentialsTest() {
		checkCDK32Parameters();
		addNewCDK32Server(SERVER_ADAPTER_32, 
				MINISHIFT_HYPERVISOR, CDK32_MINISHIFT, MINISHIFT_PROFILE_REG,
				username, password);
	}
	
	/**
	 * Ignored due to non recoverable fail during second start
	 */
	@Ignore
	@Test
	public void testPassingWrongCredentials() {
		try {
			startServerAdapter(() -> { 
				passCredentialsIntoEnvironment(true);
				}, true);
			fail("Server adapter should not have started successfully");
		} catch (CDKServerException exc) {
			log.info("Expected CDKServerException was thrown: " + exc.getCause());
		} catch (WaitTimeoutExpiredException waitExc) {
			fail(waitExc.getMessage());
		}
		verifyConsoleContainsRegEx("\\bNot a tty supported terminal\\b");
	}
	
	/**
	 * Unstable due to https://issues.jboss.org/browse/CDK-270
	 */
	@Test
	public void testNotPassingCredentials() {
		try {
			startServerAdapter(() -> { 
				passCredentialsIntoEnvironment(false);
				}, true);
			fail("Server adapter should not have started successfully");
		} catch (CDKServerException exc) {
			log.info("Expected CDKServerException was thrown: " + exc.getMessage());
		} catch (WaitTimeoutExpiredException waitExc) {
			fail("WaitTimeoutExpection occured, this was not expected. \r\n" + waitExc.getMessage());
		}
		verifyConsoleContainsRegEx("\\bNot a tty supported terminal\\b");		
	}
	
	@After
	public void stopAdapter() {
		closeAllErrorDialogs();
		stopServerAdapter();
	}
	
	@AfterClass
	public static void tearDownCDKWrongCredentialsTest() {
		CDKTestUtils.removeAccessRedHatCredentials(CDKLabel.Others.CREDENTIALS_DOMAIN, username);
	}
	
	private void closeAllErrorDialogs() {
		try {
			new DefaultShell(CDKLabel.Shell.PROBLEM_DIALOG);
			new OkButton().click();
		} catch (CoreLayerException exc) {
			log.info("Problem Occurred dialog was not present");
		}
		try {
			new DefaultShell(CDKLabel.Shell.MULTIPLE_PROBLEMS_DIALOG);
			new OkButton().click();
		} catch (CoreLayerException exc) {
			log.info("Multiple problems have occurred dialog was not present");
		}
	}

}
