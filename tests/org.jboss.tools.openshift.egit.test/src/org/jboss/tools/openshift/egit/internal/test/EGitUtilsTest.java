package org.jboss.tools.openshift.egit.internal.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.Activator;
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

	private TestRepository testRepository;
	private TestRepository testRepository2;
	private TestProject testProject;
	private TestProject testProject2;
	private TestRepository clonedTestRepository;

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
		
		this.clonedTestRepository = cloneRepository(testRepository);
		clonedTestRepository.addRemoteTo(REPO2_REMOTE_NAME, testRepository2.getRepository());
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
		clonedTestRepository.dispose();
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
		testRepository.track(file);

		EGitUtils.commit(testProject.getProject(), null);

		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository.getRepository(),
				new String[] { testUtils.getPathInRepository(file), fileContent });
	}

	@Test
	public void fileAddedToCloneIsInOriginAfterPush() throws Exception {
		String fileName = "b.txt";
		String fileContent = "adietish@redhat.com";

		File file = clonedTestRepository.createFile(fileName, fileContent);
		clonedTestRepository.addAndCommit(file, "adding a file");

		EGitUtils.push(clonedTestRepository.getRepository(), null);

		// does origin contain file added to clone?
		testUtils.assertRepositoryContainsFilesWithContent(
				clonedTestRepository.getRepository(),
				fileName,
				fileContent);
	}

	@Test
	public void fileAddedToCloneIsInRemoteAfterPush() throws Exception {
		String fileName = "c.txt";
		String fileContent = "adietish@redhat.com";

		File file = clonedTestRepository.createFile(fileName, fileContent);
		clonedTestRepository.addAndCommit(file, "adding a file");

		EGitUtils.push(REPO2_REMOTE_NAME, clonedTestRepository.getRepository(), null);

		// does origin contain file added to clone?
		testUtils.assertRepositoryContainsFilesWithContent(
				testRepository2.getRepository(),
				fileName,
				fileContent);
	}

	@Test
	public void forcedPushRemovesFileInRemote() throws Exception {
		String fileName = "c.txt";
		String fileContent = "adietish@redhat.com";

		IFile fileInRepo2 = testUtils.addFileToProject(
				testProject2.getProject(),
				fileName,
				fileContent);
		testRepository2.track(fileInRepo2);

		File fileInClone = clonedTestRepository.createFile(fileName, fileContent);
		clonedTestRepository.addAndCommit(fileInClone, "adding a file");

		EGitUtils.push(REPO2_REMOTE_NAME, clonedTestRepository.getRepository(), null);

		// does origin contain file added to clone?
		testUtils.assertRepositoryContainsFilesWithContent(
				clonedTestRepository.getRepository(),
				fileName,
				fileContent);
	}

}
