/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.importapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizardModel;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
/**
 * @author Fred Bricon
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportApplicationWizardModelTest {

	private ImportApplicationWizardModel model;

	@Mock private Connection connection;

	@Mock private IProject p1;
	@Mock private IProject p2;

	@Mock private IBuildConfig p1_bc1;
	@Mock private IBuildConfig p1_bc2;
	@Mock private IBuildConfig p2_bc1;
	
	@Before
	public void setUp() {
		when(p1.getResources(ResourceKind.BUILD_CONFIG)).thenReturn(Arrays.asList(p1_bc1, p1_bc2));
		when(p2.getResources(ResourceKind.BUILD_CONFIG)).thenReturn(Arrays.asList(p2_bc1));

		when(connection.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(p1, p2));

		model = new ImportApplicationWizardModel();
		model.setConnection(connection);
	}

	@Test
	public void shouldReturnBuildConfigNameForApplicationName() {
		// given
		String bcName = "gargamel";
		model.loadBuildConfigs();
		doReturn(bcName).when(p2_bc1).getName();
		model.setSelectedItem(p2_bc1);
		// when
		String appName = model.getApplicationName();
		// then
		assertThat(appName).isEqualTo(bcName);
	}
	
	@Test
	public void shouldReturnNullWhenNoBuildConfigIsSelected() {
		// given
		String bcName = "papaSmurf";
		model.loadBuildConfigs();
		doReturn(bcName).when(p2_bc1).getName();
		model.setSelectedItem(p1);
		// when
		String appName = model.getApplicationName();
		// then
		assertThat(appName).isNull();
	}

	@Test
	public void shouldReturn2ProjectsWhenNoProjectIsSelected() {
		//No project selected -> 2 projects returned
		// given
		model.setProject(null);
		// when
		model.loadBuildConfigs();
		// then
		List<ObservableTreeItem> results = model.getBuildConfigs();
		assertEquals(2, results.size());
		assertEquals(p1, results.get(0).getModel());
		assertEquals(p2, results.get(1).getModel());
	}
	
	@Test
	public void shouldReturnProject2BuildConfigWhenProject2IsSelected() {
		// p2 selected -> 1 build config returned
		// given
		model.setProject(p2);
		// when
		model.loadBuildConfigs();
		// then
		List<ObservableTreeItem> results = model.getBuildConfigs();
		assertEquals(1, results.size());
		assertEquals(p2_bc1, results.get(0).getModel());
	}

	@Test
	public void shouldReturnProject1BuildConfigsWhenProject1IsSelected() {
		//p1 selected -> 2 build configs returned
		// given
		model.setProject(p1);
		// when
		model.loadBuildConfigs();
		// then
		List<ObservableTreeItem> results = model.getBuildConfigs();
		assertEquals(2, results.size());
		assertEquals(p1_bc1, results.get(0).getModel());
		assertEquals(p1_bc2, results.get(1).getModel());
	}

	@Test
	public void shouldProjectThatWasSet() {
		// given
		model.loadBuildConfigs();
		assertThat(model.getProject()).isNull();
		// when
		model.setProject(p2);
		// then
		assertThat(model.getProject()).isEqualTo(p2);
	}

	@Test
	public void shouldUseDefaultRepositoryByDefault() {
		// then
		assertThat(model.isUseDefaultRepositoryPath()).isTrue();
	}

	@Test
	public void shouldResetRepositoryPathWhenSettingToUseDefaultRepository() {
		// given
		model.setUseDefaultRepositoryPath(false);
		model.setRepositoryPath(FileUtils.getTempDirectoryPath());
		// when
		model.setUseDefaultRepositoryPath(true);
		// then
		assertTrue(model.isUseDefaultRepositoryPath());
		assertEquals(EGitUIUtils.getEGitDefaultRepositoryPath(), model.getRepositoryPath());
	}

	@Test
	public void shouldResetUseDefaultRepositoryPathWhenSettingRepositoryPath() {
		// given
		model.setUseDefaultRepositoryPath(true);
		// when
		model.setRepositoryPath(FileUtils.getTempDirectoryPath());
		// then
		assertThat(model.isUseDefaultRepositoryPath()).isFalse();
	}

	@Test
    public void shouldHaveCustomRepoPathWhenUseDefaultPathSetToFalse() {
		// given
		model.setUseDefaultRepositoryPath(true);
		String repoPath = FileUtils.getTempDirectoryPath();
		// when
		model.setUseDefaultRepositoryPath(false);
    	model.setRepositoryPath(repoPath);
    	// then
    	assertFalse(model.isUseDefaultRepositoryPath());
        assertEquals(repoPath, model.getRepositoryPath());
    }

	@Test
	public void shouldHaveCloneDestinationOutOfRepoPathAndRepoName() {
		// given
		String repoName = "gargamel";
		String repoURI = "git@github.com:jbosstools/" + repoName + ".git";
		doReturn(repoURI).when(p1_bc2).getSourceURI();
		String repoPath = FileUtils.getTempDirectoryPath();

		model.setUseDefaultRepositoryPath(false);
		model.setProject(p1);
		model.loadBuildConfigs();
		// when
		model.setSelectedItem(p1_bc2);
		model.setRepositoryPath(repoPath);
		// then
		assertThat(model.getCloneDestination())
			.isEqualTo(new Path(repoPath).append(repoName).toFile());
	}

	@Test
	public void shouldResetReuseGitRepoIfNewCloneDestinationDoesntExist() throws IOException {
		// given
		String repoName = "gargamel";
		String repoURI = "git@github.com:jbosstools/" + repoName + ".git";
		doReturn(repoURI).when(p1_bc2).getSourceURI();
		String repoPath = FileUtils.getTempDirectoryPath();
		new File(repoPath, repoName).createNewFile();
		
		model.setProject(p1);
		model.loadBuildConfigs();
		model.setSelectedItem(p1_bc2);
		model.setRepositoryPath(repoPath);
		model.setReuseGitRepository(true);
		assertThat(model.isReuseGitRepository()).isTrue();
		File newRepoPath = FileUtils.getUserDirectory();
		// when
		model.setRepositoryPath(newRepoPath.getAbsolutePath());		
		// then
		assertThat(model.isReuseGitRepository()).isFalse();
	}

	@Test
	public void shouldReturnNullWhenCallingGetSelectedBuildConfigIfNoBuildConfigIsSelected() {
		// given
		model.setSelectedItem(p2);
		// when
		IBuildConfig bc = model.getSelectedBuildConfig();
		// then
		assertThat(bc).isNull();
	}

	@Test
	public void shouldReturnSelectedBuildConfig() {
		// given
		model.setSelectedItem(p2_bc1);
		// when
		IBuildConfig bc = model.getSelectedBuildConfig();
		// then
		assertThat(bc).isEqualTo(p2_bc1);
	}
	
	@Test
	public void shouldReturnSourceURIFromSelectedBuildConfigWhenCalledGetGitURL() {
		// given
		String repoURI = "git@github.com:jbosstools/timber.git";
		doReturn(repoURI).when(p1_bc2).getSourceURI();
		model.setSelectedItem(p1_bc2);
		// when
		String gitUrl = model.getGitUrl();
		// then
		assertThat(gitUrl).isEqualTo(repoURI);
	}

	@Test
	public void shouldReturnNullWhenCallingGetGitURLAndNoBuildConfigIsSelected() {
		// given
		String repoURI = "git@github.com:jbosstools/timber.git";
		doReturn(repoURI).when(p1_bc2).getSourceURI();
		model.setSelectedItem(null);
		// when
		String gitUrl = model.getGitUrl();
		// then
		assertThat(gitUrl).isNull();
	}
	
	@Test
	public void shouldReturnGitRefFromSelectedBuildConfig() {
		// given
		String buildSourceRef = "special-branch-42";
		ResourceMocks.createGitBuildSource(null, buildSourceRef, p1_bc2);
		model.setSelectedItem(p1_bc2);
		// when
		String gitRef = model.getGitRef();
		// then
		assertThat(gitRef).isEqualTo(buildSourceRef);
	}

	@Test
	public void shouldReturnNullGitRefWhenNoBuildConfigIsSelected() {
		// given
		String buildSourceRef = "special-branch-42";
		ResourceMocks.createGitBuildSource(null, buildSourceRef, p1_bc2);
		model.setSelectedItem(null);
		// when
		String gitRef = model.getGitRef();
		// then
		assertThat(gitRef).isNull();
	}

	@Test
	public void shouldGetGitContextDirFromSelectedBuildConfig() {
		// given
		String contextDir = "contextDir-42";
		ResourceMocks.createGitBuildSource(contextDir, null, p1_bc2);
		model.setSelectedItem(p1_bc2);
		// when
		String gitContextDir = model.getGitContextDir();
		// then
		assertThat(gitContextDir).isEqualTo(contextDir);
	}

	@Test
	public void shouldUseUserProvidedGitContextDirOverridingBuildConfigGitContextDir() {
		// given
		String resourceGitContextDir = "contextDir-42";
		ResourceMocks.createGitBuildSource(resourceGitContextDir, null, p1_bc2);
		model.setSelectedItem(p1_bc2);
		assertThat(model.getGitContextDir()).isEqualTo(resourceGitContextDir);
		String userGitContextDir = "42-contextDir";
		// when
		model.setGitContextDir(userGitContextDir);
		String gitContextDir = model.getGitContextDir();
		// then
		assertThat(gitContextDir).isEqualTo(userGitContextDir);
	}

	@Test
	public void shouldBuildConfigGitContextDirWhenNewBuildConfigIsSet() {
		// given
		String userGitContextDir = "42-contextDir";
		model.setGitContextDir(userGitContextDir);
		assertThat(model.getGitContextDir()).isEqualTo(userGitContextDir);
		String resourceGitContextDir = "contextDir-42";
		ResourceMocks.createGitBuildSource(resourceGitContextDir, null, p1_bc2);
		// when
		model.setSelectedItem(p1_bc2);
		String gitContextDir = model.getGitContextDir();
		// then
		assertThat(gitContextDir).isEqualTo(resourceGitContextDir);
	}
}