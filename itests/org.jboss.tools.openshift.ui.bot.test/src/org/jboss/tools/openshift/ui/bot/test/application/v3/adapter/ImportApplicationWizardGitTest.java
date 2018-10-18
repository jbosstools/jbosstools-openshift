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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.group.DefaultGroup;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.wizard.importapp.GitCloningWizardPage;
import org.jboss.tools.openshift.reddeer.wizard.importapp.ImportApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.Test;

/**
 * @author jnovak@redhat.com
 */
public class ImportApplicationWizardGitTest extends ImportApplicationBase{
	
	private File gitFolder = new File(System.getProperty("user.home") + File.separatorChar + "git");
	private File projectFolder = new File(gitFolder, OpenShiftResources.NODEJS_GIT_NAME);
	
	@Test
	public void testNotGitRepo() {
		assertTrue("Failed to create test project non git folder!", projectFolder.mkdir());
		
		service.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));
		
		ImportApplicationWizard importWizard = new ImportApplicationWizard();
		assertProjectExistsErrorInWizard(importWizard);
		
		CheckBox useExistingRepositoryCheckBox = new CheckBox(
				new DefaultGroup("Clone destination"), 
				"Do not clone - use existing repository"
		);		
		assertFalse("Empty non git folder should not be able to choose!", 
				useExistingRepositoryCheckBox.isEnabled());
		
		importWizard.cancel();
	}
	
	@Test
	public void testNoRepoRemote() {
		createRepo();

		service.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));
		
		ImportApplicationWizard importWizard = new ImportApplicationWizard();
		assertProjectExistsErrorInWizard(importWizard);
		
		GitCloningWizardPage gitCloningWizardPage = new GitCloningWizardPage();
		gitCloningWizardPage.useExistingRepository(true);
		assertGitRemoteErrorInWizzard(importWizard);
		
		importWizard.cancel();		
	}
	
	@Test
	public void testNotMatchingRepoRemote() {
		Git repo = createRepo();
		setRemote(repo);

		service.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));
		
		ImportApplicationWizard importWizard = new ImportApplicationWizard();
		assertProjectExistsErrorInWizard(importWizard);
		
		GitCloningWizardPage gitCloningWizardPage = new GitCloningWizardPage();
		gitCloningWizardPage.useExistingRepository(true);
		assertGitRemoteErrorInWizzard(importWizard);
		
		importWizard.cancel();	
	}	
	
	@Test(expected=OpenshiftTestInFailureException.class)
	public void testNotExistingBranch() {
		Git repo = createRepo();
		setRemote(repo, getOriginURL());
		performCommit(repo);
		renameMaster(repo);
		
		service.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.IMPORT_APPLICATION).select();
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.IMPORT_APPLICATION));
		
		ImportApplicationWizard importWizard = new ImportApplicationWizard();
		assertProjectExistsErrorInWizard(importWizard);
		
		GitCloningWizardPage gitCloningWizardPage = new GitCloningWizardPage();
		gitCloningWizardPage.useExistingRepository(true);
		try {
			assertGitRemoteErrorInWizzard(importWizard);
		}catch(AssertionError err){
			importWizard.cancel();
			throw new OpenshiftTestInFailureException("https://issues.jboss.org/browse/JBIDE-24646", err);
		}
		
		importWizard.cancel();
	}	
	
	private void assertProjectExistsErrorInWizard(ImportApplicationWizard importWizard) {
		assertTrue("There should be an error in the wizard! (There already is a folder named...)",
				importWizard.getMessage().trim().startsWith("There already is a folder named"));
	}
	
	private void assertGitRemoteErrorInWizzard(ImportApplicationWizard importWizard) {
		assertTrue("There should be an error in the wizard! (The reused git repository has no remote to...)",
				importWizard.getMessage().trim().startsWith("The reused git repository has no remote to "));
	}
	
	private Git createRepo() {
		try {
			return Git.init().setDirectory(projectFolder).call();
		} catch (IllegalStateException | GitAPIException e) {
			e.printStackTrace();
			fail();
			return null;			
		}
	}
	
	private void setRemote(Git repo) {
		setRemote(repo, "http://github.com/user/repo");
	}	
	
	private void setRemote(Git repo, String remoteURL) {
		try {			
			StoredConfig config = repo.getRepository().getConfig();
			config.setString("remote", "origin", "url", remoteURL);
			config.save();
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	private String getOriginURL() {
		project.select();
		project.openProperties();
		project.selectTabbedProperty("Builds");
		PropertySheet propertiesView = new PropertySheet();
		new DefaultTable().getItem(0).select();
		String originURL = propertiesView.getProperty("Misc", "Source Repo").getPropertyValue();
		return originURL;
	}
	
	private void performCommit(Git repo) {		
		try {
			File commitFile = new File(projectFolder, "commitFile.txt");
			boolean fileCreated = commitFile.createNewFile();
			assertTrue("Failed to create commit file!", fileCreated);
			
			repo.add().addFilepattern(".").call();
			repo.commit().setMessage("Init commit. Required for master creation.").call();
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
			fail();	
		}
	}
	
	private void renameMaster(Git repo) {
		try {
			repo.branchRename().setNewName("foo").call();
		} catch (GitAPIException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	

}
