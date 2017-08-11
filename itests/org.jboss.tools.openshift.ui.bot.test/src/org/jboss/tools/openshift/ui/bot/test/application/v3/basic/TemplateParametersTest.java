/*******************************************************************************
 * Copyright (c) 2007-2016 Red Hat, Inc.
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
import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.swt.condition.TableContainsItem;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.BackButton;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.button.NextButton;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.styledtext.DefaultStyledText;
import org.jboss.reddeer.swt.impl.table.DefaultTable;
import org.jboss.reddeer.swt.impl.text.DefaultText;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.reddeer.swt.impl.tree.DefaultTreeItem;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeProjectUtils;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@RequiredBasicConnection
public class TemplateParametersTest {

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
	public static final String SOURCE_REPOSITORY_URL_VALUE = "https://github.com/jboss-developer/jboss-eap-quickstarts";
	public static final String PERSONAL_GIT_REPO_URI = "https://github.com/mlabuda/jboss-eap-quickstarts";
	public static final String SECRET_VALUE = "(generated)";
	
	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;
	
	@Before
	public void openTemplateParametersWizardPage() {
		OpenShift3NativeProjectUtils.getOrCreateProject(DatastoreOS3.PROJECT1,
				DatastoreOS3.PROJECT1_DISPLAYED_NAME, StringUtils.EMPTY, connectionReq.getConnection());
		new NewOpenShift3ApplicationWizard().openWizardFromExplorer();
		new DefaultTree().selectItems(new DefaultTreeItem(OpenShiftLabel.Others.EAP_TEMPLATE));
		
		new WaitUntil(new WidgetIsEnabled(new NextButton()), TimePeriod.NORMAL);
		
		new NextButton().click();
		
		new WaitUntil(new WidgetIsEnabled(new BackButton()), TimePeriod.LONG);
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
				new DefaultTable().getItem(APPLICATION_NAME).getText(1).equals("eap-app"));
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
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.EDIT_TEMPLATE_PARAMETER));
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		assertTrue("New value of git repo URI has not been modified successfully.",
				new DefaultTable().getItem(SOURCE_REPOSITORY_URL).getText(1).equals(PERSONAL_GIT_REPO_URI));
		
		new PushButton(OpenShiftLabel.Button.RESET).click();

		try {
			new WaitUntil(new TableContainsItem(new DefaultTable(), SOURCE_REPOSITORY_URL_VALUE, 1),
					TimePeriod.NORMAL);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Value for git repo URI has not been reset.");
		}
	}
	
	@After
	public void closeNewApplicationWizard() {
		new CancelButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
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
