/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.egit.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.jboss.tools.common.util.FileUtil;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.internal.test.util.TestProject;
import org.jboss.tools.openshift.egit.internal.test.util.TestRepository;
import org.jboss.tools.openshift.egit.internal.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EGitUtilsTest {

	private static final String REPO_BRANCH = "myBranch";
	private static final String GIT_EMAIL = "dummyUser@redhat.com";
	private static final String GIT_USER = "dummyUser";
	private static final String REPO2_REMOTE_NAME = "openshift";
	
	protected final TestUtils testUtils = new TestUtils();

	private TestProject testProject;
	private TestRepository testRepository;
	private TestRepository testRepositoryClone;
	private TestProject testProject2;
	private TestRepository testRepository2;

	@Before
	public void setUp() throws Exception {
		Activator.getDefault().getRepositoryCache().clear();

		this.testProject = new TestProject(true);
		this.testRepository = createTestRepository(testProject);

		this.testRepositoryClone = cloneRepository(testRepository);
		
		this.testProject2 = new TestProject(true);
		this.testRepository2 = createTestRepository(testProject2);
	}

	private TestRepository createTestRepository(TestProject project) throws IOException, Exception {
		TestRepository testRepository = new TestRepository(TestUtils.createGitDir(project));
		testRepository.createMockSystemReader(ResourcesPlugin.getWorkspace().getRoot().getLocation());
		testRepository.setUserAndEmail(GIT_USER, GIT_EMAIL);
		testRepository.connect(project.getProject());
		testRepository.add(project.getFile(".project"));
		testRepository.initialCommit();
		return testRepository;
	}

	private TestRepository cloneRepository(TestRepository repository) throws URISyntaxException,
			InvocationTargetException, InterruptedException, IOException {
		File workspaceDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		File clonedRepositoryFile =
				new File(workspaceDir, "clonedRepository-" + String.valueOf(System.currentTimeMillis()));
		return repository.cloneRepository(clonedRepositoryFile);
	}

	private IFile commitFile(String fileName, String fileContent, TestProject project, TestRepository repository) throws Exception {
		Assert.assertNotNull(fileName);
		Assert.assertNotNull(fileContent);
		Assert.assertNotNull(repository);
		Assert.assertNotNull(project);

		IFile file = testUtils.addFileToProject(project.getProject(), fileName, fileContent);
		repository.add(file);
		repository.commit("someCommit");
		return file;
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		testRepositoryClone.dispose();
		testRepository2.dispose();
		Activator.getDefault().getRepositoryCache().clear();
		
		testProject.dispose();
		testProject2.dispose();		
	}

	@Test
	public void shouldReturnRepositoryExists() {
		// given
		File repositoryDir = testRepository.getRepository().getDirectory().getParentFile();
		// when
		boolean exists = EGitUtils.isRepository(repositoryDir);
		// then
		assertTrue(exists);
	}
	
	@Test
	public void shouldReturnRepositoryDoesntExists() {
		// given
		File repositoryDir =  new File(System.getProperty("java.io.tmpdir"));
		// when
		boolean exists = EGitUtils.isRepository(repositoryDir);
		// then
		assertFalse(exists);
	}

	@Test
	public void canCommitFileInProject() throws Exception {
		String fileName = "a.txt";
		String fileContent = "adietish@redhat.com";

		IFile file = testUtils.addFileToProject(
				testProject.getProject(),
				fileName,
				fileContent);
		testRepository.add(file);

		EGitUtils.commit(testProject.getProject(), null);

		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository.getRepository(),
				new String[] { testUtils.getPathInRepository(file), fileContent });
	}
	
	@Test
	public void fileAddedToCloneIsInOriginAfterPush() throws Exception {
		String fileName = "b.txt";
		String fileContent = "adietish@redhat.com";

		File file = testRepositoryClone.createFile(fileName, fileContent);
		testRepositoryClone.addAndCommit(file, "adding a file");

		EGitUtils.push(testRepositoryClone.getRepository(), null);

		// does origin contain file added to clone?
		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository.getRepository(),
				fileName,
				fileContent);
	}

	@Test
	public void fileAddedToCloneIsInRemoteAfterPush() throws Exception {
		String fileName = "c.txt";
		String fileContent = "adietish@redhat.com";
		File file = testRepositoryClone.createFile(fileName, fileContent);
		testRepositoryClone.addAndCommit(file, "adding a file");

		testRepositoryClone.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());
		EGitUtils.pushForce(REPO2_REMOTE_NAME, testRepositoryClone.getRepository(), null);

		// repo2 must contain file added to clone
		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository2.getRepository(),
				fileName,
				fileContent);
	}

	@Test
	public void forcedPushRemovesFileInRemote() throws Exception {
		String fileName = "a.txt";
		String fileContent = "adietish@redhat.com";
		File file = testRepository.createFile(fileName, fileContent);
		testRepository.addAndCommit(file, "adding a file");

		File file2 = testRepository2.createFile("b.txt", "bingobongo");
		testRepository2.addAndCommit(file2, "adding a file");
		
		testRepository.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());
		EGitUtils.pushForce(REPO2_REMOTE_NAME, testRepository.getRepository(), null);

		// repo2 mustn't contain "b.txt"
		testUtils.assertRepositoryMisses(
				testRepository2.getRepository(),
				file2.getName());
		// repo2 must contain "a.txt"
		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository2.getRepository(),
				fileName,
				fileContent);
	}
	
	@Test(expected=CoreException.class)
	public void pushFailsOnNonFastForward() throws Exception {
		String fileName = "a.txt";
		String fileContent = "adietish@redhat.com";
		File file = testRepository.createFile(fileName, fileContent);
		testRepository.addAndCommit(file, "adding a file");

		File file2 = testRepository2.createFile("b.txt", "bingobongo");
		testRepository2.addAndCommit(file2, "adding a file");
		
		testRepository.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());
		EGitUtils.push(REPO2_REMOTE_NAME, testRepository.getRepository(), null);
	}

	@Test
	public void shouldReturnThatCloneIsAhead() throws Exception {
		assertFalse(EGitUtils.isAhead(testRepositoryClone.getRepository(), Constants.DEFAULT_REMOTE_NAME, null));

		String fileName = "c.txt";
		String fileContent = "adietish@redhat.com";
		File file = testRepositoryClone.createFile(fileName, fileContent);
		testRepositoryClone.addAndCommit(file, "adding a file");
		
		assertTrue(EGitUtils.isAhead(testRepositoryClone.getRepository(), Constants.DEFAULT_REMOTE_NAME, null));
	}
	
	@Test
	public void shouldReturnThatRemoteIsAhead() throws Exception {
		String fileName = "a.txt";
		String fileContent = "adietish@redhat.com";
		File file = testRepository.createFile(fileName, fileContent);
		testRepository.addAndCommit(file, "adding a file");
		testRepository.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());

		assertTrue(EGitUtils.isAhead(testRepository.getRepository(), REPO2_REMOTE_NAME, null));
	}

	@Test
	public void shouldNotBeAheadAfterPush() throws Exception {
		String fileName = "a.txt";
		String fileContent = "adietish@redhat.com";
		File file = testRepository.createFile(fileName, fileContent);
		testRepository.addAndCommit(file, "adding a file");
		testRepository.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());

		assertTrue(EGitUtils.isAhead(testRepository.getRepository(), REPO2_REMOTE_NAME, null));

		EGitUtils.pushForce(REPO2_REMOTE_NAME, testRepository.getRepository(), null);
		
		assertFalse(EGitUtils.isAhead(testRepository.getRepository(), REPO2_REMOTE_NAME, null));	
	}

	@Test
	public void shouldReturnThatCloneIsAheadOfRemote() throws Exception {
		testRepositoryClone.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());
		new Git(testRepositoryClone.getRepository()).push().setRemote(REPO2_REMOTE_NAME).setForce(true).call();
		assertFalse(EGitUtils.isAhead(testRepositoryClone.getRepository(), REPO2_REMOTE_NAME, null));

		String fileName = "c.txt";
		String fileContent = "adietish@redhat.com";
		File file = testRepositoryClone.createFile(fileName, fileContent);
		testRepositoryClone.addAndCommit(file, "adding a file");
		assertTrue(EGitUtils.isAhead(testRepositoryClone.getRepository(), REPO2_REMOTE_NAME, null));
		
		new Git(testRepositoryClone.getRepository()).push().setForce(true).call();
		assertTrue(EGitUtils.isAhead(testRepositoryClone.getRepository(), REPO2_REMOTE_NAME, null));
	}

	@Test
	public void shouldUserFetchSpecInConfig() throws Exception {
		testRepository.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());

		// add custom fetch-spec in config (fetch = refs/heads/master:refs/remotes/bingo/master)
		StoredConfig config = testRepository.getRepository().getConfig();
		config.getString(ConfigConstants.CONFIG_KEY_REMOTE, REPO2_REMOTE_NAME, "fetch");
		String remoteTrackingBranchRef = "refs/remotes/bingo/master"; 
		String fetchSpec = "refs/heads/master:" + remoteTrackingBranchRef;
		config.setString(ConfigConstants.CONFIG_KEY_REMOTE, REPO2_REMOTE_NAME, "fetch", fetchSpec);
		config.save();

		EGitUtils.isAhead(testRepository.getRepository(), REPO2_REMOTE_NAME, null);

		// was remote tracking branch created?
		assertTrue(testRepository.getRepository().getAllRefs().containsKey(remoteTrackingBranchRef));
	}

	@Test
	public void shouldCheckoutBranch() throws Exception {
		// given file committed in custom branch
		String branch = testRepository.getCurrentBranchObjectId();

		testRepository.createAndCheckoutBranch(Constants.HEAD, REPO_BRANCH);
		String fileName = "a.txt";
		String fileContent = "adietish@redhat.com";
		IFile file = commitFile(fileName, fileContent, testProject, testRepository);
		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository.getRepository(),
				new String[] { testUtils.getPathInRepository(file), fileContent });

		testRepository.checkoutBranch(branch);
		testUtils.assertRepositoryMisses(testRepository.getRepository(), testUtils.getPathInRepository(file));

		// when checkout custom branch
		CheckoutResult result = EGitUtils.branch(REPO_BRANCH, testProject.getProject(), null);

		// then file should be present
		assertTrue(result.getStatus() == CheckoutResult.Status.OK);
		assertTrue(testRepository.isCurrentBranch(REPO_BRANCH));
		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository.getRepository(),
				new String[] { testUtils.getPathInRepository(file), fileContent });
	}
	
	@Test
	public void canGetRepoForProject() throws Exception {
		Repository repository = EGitUtils.getRepository(testProject.getProject());
		assertNotNull(repository);
		assertEquals(testRepository.getRepository(), repository);
	}

	@Test
	public void canGetRepoForFile() throws Exception {
		File repoFile = testRepository.getGitDir().getParentFile();
		Repository repository = EGitUtils.getRepository(repoFile);
		assertNotNull(repository);
		// not the same repository instance
		assertEquals(testRepository.getRepository().getDirectory(), repository.getDirectory());
	}

	@Test
	public void canAddRemoteRepo() throws Exception {
		Repository repository = testRepository.getRepository();
		String remoteName = "redhat";
		String gitUri = "www.redhat.com";
		EGitUtils.addRemoteTo(remoteName, gitUri, repository);

		StoredConfig config = repository.getConfig();
		Set<String> subsections = config.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);
		assertEquals(1, subsections.size());
		assertTrue(subsections.contains(remoteName));
		assertEquals(gitUri, config.getString(ConfigConstants.CONFIG_REMOTE_SECTION, remoteName, ConfigConstants.CONFIG_KEY_URL));
	}
	
	@Test
	public void untrackedWhileDontIncludeUntrackedIsNotDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		testRepository.createFile("a.txt", "protoculture");
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), false, new NullProgressMonitor()));
	}
	
	@Test
	public void untrackedWhileIncludeUntrackedIsDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		testRepository.createFile(testProject.getProject(), "a.txt", "42");
		assertTrue(EGitUtils.isDirty(testRepository.getRepository(), true, new NullProgressMonitor()));
	}
	
	@Test
	public void addedButNotCommittedIsDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		File file = testRepository.createFile("a.txt", "protoculture");
		testRepository.add(file);
		assertTrue(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
	}
	
	@Test
	public void modifiedButNotCommittedIsDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		File file = testRepository.createFile("a.txt", "protonica");
		assertTrue(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		testRepository.addAndCommit(file, "commit-by-junit-tests");
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		FileUtil.writeFileDefault(file, "atrix");
		assertTrue(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
	}

	@Test
	public void removedButNotCommittedIsDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		File file = testRepository.createFile("a.txt", "protonica");
		testRepository.addAndCommit(file, "commit-by-junit-tests");
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		file.delete();
		assertTrue(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
	}

	@Test
	public void newUntrackedFilesAreCounted() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		testRepository.createFile(testProject.getProject(), "a.txt", "42");
		testRepository.createFile(testProject.getProject(), "b.txt", "84");
		int numOfChanges = EGitUtils.countChanges(testRepository.getRepository(), true, new NullProgressMonitor());
		assertEquals(2, numOfChanges);
	}
	
	@Test
	public void newUntrackedFileIsNotCountedIfIgnoreUntracked() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository(), new NullProgressMonitor()));
		testRepository.createFile(testProject.getProject(), "a.txt", "42");
		int numOfChanges = EGitUtils.countChanges(testRepository.getRepository(), false, new NullProgressMonitor());
		assertEquals(0, numOfChanges);
	}
	
	@Test
	public void canGetSingleRemoteConfig() throws CoreException, MalformedURLException, URISyntaxException, IOException {
		String remoteName = "repo2";
		
		testRepository.addRemoteTo(remoteName, testRepository2.getRepository());
		List<RemoteConfig> allRemoteConfigs = EGitUtils.getAllRemoteConfigs(testRepository.getRepository());
		assertNotNull(allRemoteConfigs);
		assertEquals(1, allRemoteConfigs.size());
		RemoteConfig repo2Config = EGitUtils.getRemoteConfig(remoteName, allRemoteConfigs);
		assertNotNull(repo2Config);
	}

	@Test
	public void canGetFromSeveralRemoteConfig() throws CoreException, MalformedURLException, URISyntaxException, IOException {
		String repo2RemoteName = "repo2";
		
		testRepositoryClone.addRemoteTo(repo2RemoteName, testRepository2.getRepository());
		List<RemoteConfig> allRemoteConfigs = EGitUtils.getAllRemoteConfigs(testRepositoryClone.getRepository());
		assertNotNull(allRemoteConfigs);
		// clone already has repo1 as origin
		assertEquals(2, allRemoteConfigs.size());
		RemoteConfig repo2Config = EGitUtils.getRemoteConfig(repo2RemoteName, allRemoteConfigs);
		assertNotNull(repo2Config);
	}

	@Test
	public void canCheckIfHasRemote() throws CoreException, MalformedURLException, URISyntaxException, IOException {
		String repo2RemoteName = "repo2";
		
		testRepositoryClone.addRemoteTo(repo2RemoteName, testRepository2.getRepository());
		assertTrue(
				EGitUtils.hasRemote(repo2RemoteName, testRepository2.getUri().toString(), testRepositoryClone.getRepository()));
	}
	
	@Test
	public void shouldReturnHostFromFullGitUrl() {
		//pre-conditions
		//operation
		String host = EGitUtils.getGitHost("ssh://516e82ca4382ec2174000098@eap2-honkabonka2.rhcloud.com/~/git/eap2.git/");
		// verification
		assertNotNull(host);
		assertEquals("eap2-honkabonka2.rhcloud.com", host);
	}

	public void shouldReturnHostFromHostOnlyGitUrl() {
		//pre-conditions
		//operation
		String host = EGitUtils.getGitHost("ssh://eap2-honkabonka2.rhcloud.com/~/git/eap2.git/");
		// verification
		assertNotNull(host);
		assertEquals("eap2-honkabonka2.rhcloud.com", host);
	}

	public void shouldReturnHostFromHostWithoutPathGitUrl() {
		//pre-conditions
		//operation
		String host = EGitUtils.getGitHost("ssh://adietish@eap2-honkabonka2.rhcloud.com");
		// verification
		assertNotNull(host);
		assertEquals("eap2-honkabonka2.rhcloud.com", host);
	}

	public void shouldReturnNullFromInvalidGitUrl() {
		//pre-conditions
		//operation
		String host = EGitUtils.getGitHost("://516e82ca4382ec2174000098@eap2-honkabonka2.rhcloud.com/~/git/eap2.git/");
		// verification
		assertNull(host);
	}

	@Test
	public void shouldReturnUserFromFullGitUrl() {
		//pre-conditions
		//operation
		String user = EGitUtils.getGitUsername("ssh://516e82ca4382ec2174000098@eap2-honkabonka2.rhcloud.com/~/git/eap2.git/");
		// verification
		assertNotNull(user);
		assertEquals("516e82ca4382ec2174000098", user);
	}

	@Test
	public void shouldConsiderSSHGitUrlValid() {
		//pre-conditions
		//operation
		assertTrue(EGitUtils.isValidGitUrl("ssh://516e82ca4382ec2174000098@eap2-honkabonka2.rhcloud.com/~/git/eap2.git/"));
		// verification
	}
	
	@Test
	public void shouldConsiderSCPGitUrlValid() {
		//pre-conditions
		//operation
		assertTrue(EGitUtils.isValidGitUrl("git@eap2-honkabonka2.rhcloud.com:openshift/eap2.git/"));
		// verification
	}
	
	@Test
	public void originShouldBeDefaultRemoteRepo() throws Exception {
		//pre-conditions
		testRepository.addRemoteTo("git", "git://git.stuff/");
		testRepository.addRemoteTo("foo", "https://foo.bar/");
		testRepository.addRemoteTo("bar", "http://bar.foo/");
		
		//Get 1st remote after remotes were ordered alphabetically
		//operation
		String defaultRepo = EGitUtils.getDefaultRemoteRepo(testProject.getProject());
		// verification
		assertEquals("http://bar.foo/", defaultRepo);

		//Check origin is always default
		testRepository.addRemoteTo("origin", "http://origin/");
		//operation
		defaultRepo = EGitUtils.getDefaultRemoteRepo(testProject.getProject());
		// verification
		assertEquals("http://origin/", defaultRepo);
	}
	
	@Test
	public void getRemoteHttpRepos() throws Exception {
		//pre-conditions
		testRepository.addRemoteTo("git", "git://git.stuff/");
		testRepository.addRemoteTo("foo", "https://foo.bar/");
		testRepository.addRemoteTo("bar", "http://bar.foo/");
		testRepository.addRemoteTo("origin", "http://origin/");
		
		//operation
		List<String> repos = EGitUtils.getRemoteGitRepos(testProject.getProject());
		
		// verification
		assertEquals(3, repos.size());
		assertEquals("http://origin/", 	repos.get(0));
		assertEquals("http://bar.foo/", repos.get(1));
		assertEquals("https://foo.bar/", repos.get(2));
	}
}
