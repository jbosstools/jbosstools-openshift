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
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
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
@RemoveCDKServers
@ContainerRuntimeServer(
		version = CDKVersion.CDK360,
		usernameProperty="developers.username",
		passwordProperty="developers.password",
		useExistingBinaryFromConfig=true,
		makeRuntimePersistent=true,
		createServerAdapter=false,
		useExistingBinaryInProperty="cdk32.minishift")
@RunWith(RedDeerSuite.class)
public class CDKWrongCredentialsTest extends CDKServerAdapterAbstractTest {

	private static final String username = "myuser";
	
	private static final String password = "password";
	
	private static final String MINISHIFT_PROFILE_REG = "nonregistered";

	private static final Logger log = Logger.getLogger(CDKWrongCredentialsTest.class);
	
	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName();
	}
	
	@BeforeClass
	public static void setupCDKWrongCredentialsTest() {
		CDKUtils.addNewCDK32Server(
				serverRequirement.getServerAdapter().getAdapterName(), 
				MINISHIFT_HYPERVISOR, 
				serverRequirement.getServerAdapter().getMinishiftBinary().toAbsolutePath().toString(), 
				MINISHIFT_PROFILE_REG,
				username, password);
	}
	
	@After
	public void stopAdapter() {
		closeAllErrorDialogs();
		stopServerAdapter(getCDKServer());
	}
	
	@AfterClass
	public static void tearDownCDKWrongCredentialsTest() {
		CDKTestUtils.removeAccessRedHatCredentials(CDKLabel.Others.CREDENTIALS_DOMAIN, username);
	}
	
	/**
	 * Ignored due to non recoverable fail during second start
	 */
	@Ignore
	@Test
	public void testPassingWrongCredentials() {
		try {
			serverRequirement.configureCDKServerAdapter(false);
			startServerAdapter(getCDKServer(), () -> {
				passCredentialsIntoEnvironment(true);
			}, true);
			fail("Server adapter should not have started successfully");
		} catch (CDKServerException exc) {
			log.info("Expected CDKServerException was thrown: " + exc.getCause());
		} catch (WaitTimeoutExpiredException waitExc) {
			fail(waitExc.getMessage());
		}
		CDKTestUtils.verifyConsoleContainsRegEx("\\bNot a tty supported terminal\\b");
	}
	
	/**
	 * Unstable due to https://issues.jboss.org/browse/CDK-270
	 */
	@Test
	public void testNotPassingCredentials() {
		try {
			serverRequirement.configureCDKServerAdapter(false);
			startServerAdapter(getCDKServer(), () -> {
				passCredentialsIntoEnvironment(false);
			}, true);
			fail("Server adapter should not have started successfully");
		} catch (CDKServerException exc) {
			log.info("Expected CDKServerException was thrown: " + exc.getMessage());
		} catch (WaitTimeoutExpiredException waitExc) {
			fail("WaitTimeoutExpection occured, this was not expected. \r\n" + waitExc.getMessage());
		}
		CDKTestUtils.verifyConsoleContainsRegEx("\\bNot a tty supported terminal\\b");		
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
