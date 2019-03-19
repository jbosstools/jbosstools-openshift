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
package org.jboss.tools.openshift.ui.bot.test.application.v3.basic;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.condition.TableContainsItem;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.styledtext.DefaultStyledText;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeProjectUtils;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection
@RunWith(RedDeerSuite.class)
public class TemplateParametersTest extends AbstractTest {

	public static final String APPLICATION_DOMAIN = "APPLICATION_DOMAIN";
	public static final String APPLICATION_NAME = "APPLICATION_NAME *";
	public static final String CONTEXT_DIR = "CONTEXT_DIR";
	public static final String GENERIC_SECRET = "GENERIC_WEBHOOK_SECRET *";
	public static final String GITHUB_SECRET = "GITHUB_WEBHOOK_SECRET *";
	public static final String SOURCE_REPOSITORY_REF = "SOURCE_REPOSITORY_REF";
	public static final String SOURCE_REPOSITORY_URL = "SOURCE_REPOSITORY_URL *";
	
	
	public static final String APPLICATION_DOMAIN_VALUE = "Custom hostname for service routes.  "
			+ "Leave blank for default hostname, e.g.: <application-name>.<project>."
			+ "<default-domain-suffix>";
	public static final String APPLICATION_NAME_VALUE = "The name for the application.";
	public static final String CONTEXT_DIR_VALUE = "Path within Git project to build; empty for root project directory.";
	public static final String SOURCE_REPOSITORY_URL_VALUE = "https://github.com/jboss-developer/jboss-eap-quickstarts.git";
	public static final String PERSONAL_GIT_REPO_URI = "https://github.com/some_user/jboss-eap-quickstarts";
	public static final String SECRET_VALUE = "(generated)";
	
	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;
	
	@Before
	public void openTemplateParametersWizardPage() {
		OpenShift3NativeProjectUtils.getOrCreateProject(DatastoreOS3.PROJECT1,
				DatastoreOS3.PROJECT1_DISPLAYED_NAME, StringUtils.EMPTY, connectionReq.getConnection());
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer();
		OpenShiftUtils.selectEAPTemplate();
		
		new WaitUntil(new ControlIsEnabled(new NextButton()), TimePeriod.DEFAULT);
		
		new NextButton().click();
		
		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);
	}
	
	@Test
	public void testTemplateParameterDetails() {
		verifyParameter(CONTEXT_DIR, CONTEXT_DIR_VALUE);
		verifyParameter(APPLICATION_NAME, APPLICATION_NAME_VALUE);
		verifyParameter(CONTEXT_DIR, CONTEXT_DIR_VALUE);
	}
	
	@Test
	public void testTemplateParametersDefaultValues() {
		assertTrue("Value for " + APPLICATION_NAME + " parameter should be eap-app.",
				new DefaultTable().getItem(APPLICATION_NAME).getText(1).equals(OpenShiftResources.EAP_SERVICE));
		assertTrue("Value for " + GENERIC_SECRET + " parameter should be " + SECRET_VALUE,
				new DefaultTable().getItem(GENERIC_SECRET).getText(1).equals(SECRET_VALUE));
		assertTrue("Value for " + GITHUB_SECRET + " parameter should be " + SECRET_VALUE,
				new DefaultTable().getItem(GITHUB_SECRET).getText(1).equals(SECRET_VALUE));
		assertTrue("Value for " + SOURCE_REPOSITORY_URL + " parameters should be " + 
				SOURCE_REPOSITORY_URL_VALUE, new DefaultTable().getItem(SOURCE_REPOSITORY_URL).
					getText(1).equals(SOURCE_REPOSITORY_URL_VALUE));
	}
	
	@Test
	public void testModifyTemplateParameter() {
		new DefaultTable().getItem(SOURCE_REPOSITORY_URL).select();
		new PushButton(OpenShiftLabel.Button.EDIT).click();
		
		new DefaultShell(OpenShiftLabel.Shell.EDIT_TEMPLATE_PARAMETER);
		new DefaultText().setText(PERSONAL_GIT_REPO_URI);
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.EDIT_TEMPLATE_PARAMETER));
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		assertTrue("New value of git repo URI has not been modified successfully.",
				new DefaultTable().getItem(SOURCE_REPOSITORY_URL).getText(1).equals(PERSONAL_GIT_REPO_URI));
		
		new PushButton(OpenShiftLabel.Button.RESET).click();

		try {
			new WaitUntil(new TableContainsItem(new DefaultTable(), SOURCE_REPOSITORY_URL_VALUE, 1),
					TimePeriod.DEFAULT);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Value for git repo URI has not been reset.");
		}
	}
	
	@After
	public void closeNewApplicationWizard() {
		new CancelButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning());
	}
	
	private void verifyParameter(String parameterName, String parameterValue) {
		new DefaultTable().select(parameterName);
		try {
			new DefaultStyledText(parameterValue);
		} catch (RedDeerException ex) {
			fail("Details for " + parameterName + " have not been shown properly.");
		}
	}
}
