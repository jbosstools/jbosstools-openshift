package org.jboss.tools.openshift.egit.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.jboss.tools.common.util.FileUtil;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.internal.test.util.TestProject;
import org.jboss.tools.openshift.egit.internal.test.util.TestRepository;
import org.jboss.tools.openshift.egit.internal.test.util.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EGitUtilsTest {

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

		this.testRepository = new TestRepository(TestUtils.createGitDir(testProject));
		testRepository.createMockSystemReader(ResourcesPlugin.getWorkspace().getRoot().getLocation());
		testRepository.setUserAndEmail(GIT_USER, GIT_EMAIL);
		testRepository.connect(testProject.getProject());

		this.testProject2 = new TestProject(true);

		this.testRepository2 = new TestRepository(TestUtils.createGitDir(testProject2));
		testRepository2.setUserAndEmail(GIT_USER, GIT_EMAIL);
		testRepository2.connect(testProject2.getProject());
		
		this.testRepositoryClone = cloneRepository(testRepository);
//		testRepositoryClone.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());
	}

	private TestRepository cloneRepository(TestRepository repository) throws URISyntaxException,
			InvocationTargetException, InterruptedException, IOException {
		File workspaceDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		File clonedRepositoryFile =
				new File(workspaceDir, "clonedRepository-" + String.valueOf(System.currentTimeMillis()));
		return testRepository.cloneRepository(clonedRepositoryFile);
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
		EGitUtils.push(REPO2_REMOTE_NAME, testRepositoryClone.getRepository(), null);

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
	public void canGetRepoForProject() throws Exception {
		Repository repository = EGitUtils.getRepository(testProject.getProject());
		assertNotNull(repository);
		assertEquals(testRepository.getRepository(), repository);
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
	public void addedButNotCommittedIsDirty() throws IOException {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository()));
		File file = testRepository.createFile("a.txt", "protoculture");
		testRepository.add(file);
		assertTrue(EGitUtils.isDirty(testRepository.getRepository()));
	}
	
	@Test
	public void changedButNotCommittedIsDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository()));
		File file = testRepository.createFile("a.txt", "ethnica");
		testRepository.addAndCommit(file, "commit-by-junit-tests");
		assertFalse(EGitUtils.isDirty(testRepository.getRepository()));
		FileUtil.writeFileDefault(file, "depeche-mode");
		testRepository.add(file);
		assertTrue(EGitUtils.isDirty(testRepository.getRepository()));
	}

	@Test
	public void modifiedButNotCommittedIsDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository()));
		File file = testRepository.createFile("a.txt", "protonica");
		testRepository.addAndCommit(file, "commit-by-junit-tests");
		FileUtil.writeFileDefault(file, "atrix");
		assertTrue(EGitUtils.isDirty(testRepository.getRepository()));
	}

	@Test
	public void removedButNotCommittedIsDirty() throws Exception {
		assertFalse(EGitUtils.isDirty(testRepository.getRepository()));
		File file = testRepository.createFile("a.txt", "protonica");
		testRepository.addAndCommit(file, "commit-by-junit-tests");
		assertFalse(EGitUtils.isDirty(testRepository.getRepository()));
		file.delete();
		assertTrue(EGitUtils.isDirty(testRepository.getRepository()));
	}

}
