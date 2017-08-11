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
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.reddeer.core.util.Display;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.wizard.importapp.GitCloningWizardPage;
import org.jboss.tools.openshift.reddeer.wizard.importapp.ImportApplicationWizard;
import org.jboss.tools.openshift.reddeer.wizard.server.ServerSettingsWizard;
import org.jboss.tools.openshift.reddeer.wizard.server.ServerSettingsWizardPage;
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.Test;

/**
 * @author jnovak@redhat.com
 */
public class ImportApplicationWizardTest extends ImportApplicationBase {	
	
	@Test
	public void testImportOpenShift3AppViaOpenshiftView() {
		ImportApplicationWizard importWizard = 
				new ImportApplicationWizard().openFromOpenshiftView(service);
		
		importWizard.finish();
		assertProjectExistsInProjectView(OpenShiftResources.NODEJS_GIT_NAME);
	}
	
	@Test(expected=OpenshiftTestInFailureException.class)
	public void testImportOpenShift3AppViaServerAdapterSettings() {
		ServerSettingsWizard serverWizard = new ServerSettingsWizard().openFromOpenshiftView(service);
		
		ImportApplicationWizard importWizard = new ServerSettingsWizardPage().importProject();
		importWizard.next();
		importWizard.finish();
		
		new DefaultShell(serverWizard.getTitle());
		assertProjectNameFilled();
		serverWizard.cancel();
		
		assertProjectExistsInProjectView(OpenShiftResources.NODEJS_GIT_NAME);
	}
	
	@Test
	public void testImportOpenShift3AppWizardGitAPI() {
		ImportApplicationWizard importWizard = 
				new ImportApplicationWizard().openFromOpenshiftView(service);
		
		String gitUrl = getGitUrlFromWizard();		
		
		importWizard.cancel();
		
		assertTrue("Git URL error. Retrieved git URL: '" + gitUrl + "'", 
				gitUrl != null && gitUrl.startsWith("https://github.com"));
	}
	
	@Test
	public void testImportOpenShift3AppViaOpenshiftViewTwice() {
		testImportOpenShift3AppViaOpenshiftView();
		
		ImportApplicationWizard importWizard = 
				new ImportApplicationWizard().openFromOpenshiftView(service);		
		GitCloningWizardPage gitWizardPage = new GitCloningWizardPage();
		
		assertTrue("There should be an error in the wizard (There already is a project with specified name)",
				gitWizardPage.projectExists());
		
		gitWizardPage.useExistingRepository(true);
		importWizard.finishAndOverrideExisting();
		
		assertProjectExistsInProjectView(OpenShiftResources.NODEJS_GIT_NAME);
	}

	private void assertProjectNameFilled() {
		String inputProjectName = new LabeledText("Eclipse Project: ").getText();
		if(!OpenShiftResources.NODEJS_GIT_NAME.equals(inputProjectName)) {
			new CancelButton().click();
			throw new OpenshiftTestInFailureException("https://issues.jboss.org/browse/JBDS-4446");
		}
	}
	
	private void assertProjectExistsInProjectView(String projectName) {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		assertTrue("Project '" + projectName + "' was not found in the project explorer!", 
				projectExplorer.containsProject(projectName));		
	}
	
	@SuppressWarnings("restriction")
	private String getGitUrlFromWizard() {
		Shell swtWidget = new DefaultShell(OpenShiftLabel.Shell.IMPORT_APPLICATION).getSWTWidget();
		return Display.syncExec(() -> {
			try {
				WizardDialog dialog = (WizardDialog)swtWidget.getData();
				Method getWizardMethod = dialog.getClass().getDeclaredMethod("getWizard");
				getWizardMethod.setAccessible(true);
				
				org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizard 
				wizard = (org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizard)
						getWizardMethod.invoke(dialog);
				
				return wizard.getModel().getGitUrl();
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | NullPointerException e) {
				e.printStackTrace();
				fail(e.getMessage());
				return "";
			}
		});
	}

}
