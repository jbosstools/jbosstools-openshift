/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.egit.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.egit.core.internal.EGitCoreActivator;
import org.jboss.tools.openshift.egit.core.internal.utils.RegexUtils;

/**
 * The Class EGitUtils.
 * 
 * @author André Dietisheim
 */
public class EGitUtils {

	private static final int CLONE_TIMEOUT = 10 * 1024;
	private static final int PUSH_TIMEOUT = 10 * 1024;
	//	private static final RefSpec DEFAULT_PUSH_REF_SPEC = new RefSpec("refs/heads/*:refs/remotes/origin/*"); //$NON-NLS-1$
	private static final String DEFAULT_REFSPEC_SOURCE = Constants.HEAD; // HEAD
	private static final String DEFAULT_REFSPEC_DESTINATION = Constants.R_HEADS + Constants.MASTER; // refs/heads/master
	private static final String EGIT_TEAM_PROVIDER_ID = "org.eclipse.egit.core.GitProvider";

	private EGitUtils() {
		// inhibit instantiation
	}

	/**
	 * Returns <code>true</code> if the given project is associated to any team
	 * provider (git, svn, cvs, etc.). Returns <code>false</code> otherwise.
	 * 
	 * @param project
	 *            the project to check
	 * @return <code>true</code> if the project is associated with any team
	 *         provider.
	 */
	public static boolean isShared(IProject project) {
		return RepositoryProvider.getProvider(project) != null;
	}

	/**
	 * Returns <code>true</code> if the given project is associated to the egit
	 * team provider. Returns <code>false</code> otherwise.
	 * 
	 * @param project
	 *            the project to check
	 * @return <code>true</code> if the project is associated with the git team
	 *         provider.
	 */
	public static boolean isSharedWithGit(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		return provider != null
				&& EGIT_TEAM_PROVIDER_ID.equals(provider.getID());
	}

	/**
	 * Returns <code>true</code> if the given project exists and has a .git
	 * folder in it.
	 * 
	 * @param project
	 * @return
	 */
	public static boolean hasDotGitFolder(IProject project) {
		if (project == null
				|| !project.exists()) {
			return false;
		}
		return new File(project.getLocation().toOSString(), Constants.DOT_GIT).exists();
	}

	/**
	 * Shares the given project. A repository is created within the given
	 * project and the project is connected to the freshly created repository.
	 * 
	 * @param project
	 * @param monitor
	 * @return
	 * @throws CoreException
	 * @throws GitAPIException 
	 */
	public static Repository share(IProject project, IProgressMonitor monitor) throws CoreException {
		Repository repository = createRepository(project, monitor);
		connect(project, repository, monitor);
		addToRepository(project, repository, monitor);
		commit(project, monitor);
		// checkout("master", repository);
		return repository;
	}

	/**
	 * Creates a repository for the given project. The repository is created in
	 * the .git directory within the given project. The project is not connected
	 * with the new repository
	 * 
	 * @param project
	 *            the project to create the repository for.
	 * @param monitor
	 *            the monitor to report the progress to
	 * @return
	 * @throws CoreException
	 * 
	 * @see #connect(IProject, Repository, IProgressMonitor)
	 */
	public static Repository createRepository(IProject project, IProgressMonitor monitor) throws CoreException {
		try {
			InitCommand init = Git.init();
			init.setBare(false).setDirectory(project.getLocation().toFile());
			Git git = init.call();
			return git.getRepository();
		} catch (Exception e) {
			// ugly workaround to allow us to use egit 2.0 while staying
			// compatible
			throw new CoreException(EGitCoreActivator.createErrorStatus(
					NLS.bind("Could not initialize a git repository at {0}: {1}",
							getRepositoryPathFor(project),
							e.getMessage()), e));
		}
	}

	public static File getRepositoryPathFor(IProject project) {
		return new File(project.getLocationURI().getPath(), Constants.DOT_GIT);
	}

	public static void addToRepository(IProject project, Repository repository, IProgressMonitor monitor)
			throws CoreException {
		AddToIndexOperation add = new AddToIndexOperation(Collections.singletonList(project));
		add.execute(monitor);
	}

	/**
	 * Connects the given project to the repository within it.
	 * 
	 * @param project
	 *            the project to connect
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws CoreException
	 */
	public static void connect(IProject project, IProgressMonitor monitor) throws CoreException {
		connect(project, getRepositoryPathFor(project), monitor);
	}

	/**
	 * Connects the given project to the given repository.
	 * 
	 * @param project
	 *            the project to connect
	 * @param repository
	 *            the repository to connect the project to
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws CoreException
	 */
	private static void connect(IProject project, Repository repository, IProgressMonitor monitor) throws CoreException {
		connect(project, repository.getDirectory(), monitor);
	}

	private static void connect(IProject project, File repositoryFolder, IProgressMonitor monitor) throws CoreException {
		new ConnectProviderOperation(project, repositoryFolder).execute(monitor);
	}

	public static void cloneRepository(String uri, String remoteName, File destination, IProgressMonitor monitor)
			throws URISyntaxException, InvocationTargetException, InterruptedException {
		cloneRepository(uri, remoteName, destination, null, monitor);
	}

	public static void cloneRepository(String uri, String remoteName, File destination, PostCloneTask postCloneTask,
			IProgressMonitor monitor)
			throws URISyntaxException, InvocationTargetException, InterruptedException {
		URIish gitUri = new URIish(uri);
		CloneOperation cloneOperation =
				new CloneOperation(gitUri, true, null, destination, Constants.HEAD, remoteName, CLONE_TIMEOUT);
		if (postCloneTask != null) {
			cloneOperation.addPostCloneTask(postCloneTask);
		}
		cloneOperation.run(monitor);
		// RepositoryUtil repositoryUtil =
		// Activator.getDefault().getRepositoryUtil();
		// repositoryUtil.addConfiguredRepository(new File(destination,
		// Constants.DOT_GIT));
	}

	/**
	 * Merges the given uri to HEAD in the given repository. The given branch is
	 * the branch in the local repo the fetched HEAD is fetched to.
	 * 
	 * @param branch
	 * @param uri
	 * @param repository
	 * @param monitor
	 * @throws CoreException
	 * @throws InvocationTargetException
	 */
	public static void mergeWithRemote(URIish uri, String branch, Repository repository, IProgressMonitor monitor)
			throws CoreException, InvocationTargetException {
		RefSpec ref = new RefSpec().setSource(Constants.HEAD).setDestination(branch);
		fetch(uri, Collections.singletonList(ref), repository, monitor);
		merge(branch, repository, monitor);
	}

	/**
	 * Merges current master with the given branch. The strategy used is Resolve
	 * since egit does still not support the Recusive stragety.
	 * 
	 * @param branch
	 *            the branch to merge
	 * @param repository
	 *            the repository the branch is in
	 * @param monitor
	 *            the monitor to report the progress to
	 * @return the result of the merge
	 * @throws CoreException
	 * 
	 * @see MergeStrategy#RESOLVE
	 * @link 
	 *       http://www.eclipse.org/forums/index.php/mv/msg/261278/753913/#msg_753913
	 * @link https://bugs.eclipse.org/bugs/show_bug.cgi?id=354099
	 * @link https://bugs.eclipse.org/bugs/show_bug.cgi?id=359951
	 */
	private static MergeResult merge(String branch, Repository repository, IProgressMonitor monitor)
			throws CoreException {
		MergeOperation merge = new MergeOperation(repository, branch, MergeStrategy.RESOLVE.getName());
		merge.execute(monitor);
		return merge.getResult();
	}

	/**
	 * Fetches the source ref(s) (from the given ref spec(s)) from the given uri
	 * to the given destination(s) (in the given ref spec(s)) to the given
	 * repository.
	 * 
	 * @param uri
	 *            the uri to fetch from
	 * @param fetchRefsRefSpecs
	 *            the references with the sources and destinations
	 * @param repository
	 *            the repository to fetch to
	 * @param monitor
	 *            the monitor to report progress to
	 * @return
	 * @throws InvocationTargetException
	 * @throws CoreException
	 */
	private static Collection<Ref> fetch(URIish uri, List<RefSpec> fetchRefsRefSpecs, Repository repository,
			IProgressMonitor monitor)
			throws InvocationTargetException, CoreException {
		FetchOperation fetch = new FetchOperation(repository, uri, fetchRefsRefSpecs, 10 * 1024, false);
		fetch.run(monitor);
		FetchResult result = fetch.getOperationResult();
		return result.getAdvertisedRefs();
	}

	/**
	 * Commits the changes within the given project to it's configured
	 * repository. The project has to be connected to a repository.
	 * 
	 * @param project
	 *            the project whose changes shall be committed
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws CoreException
	 * 
	 * @see #connect(IProject, Repository)
	 */
	private static RevCommit commit(IProject project, String commitMessage, IProgressMonitor monitor)
			throws CoreException {
		Repository repository = getRepository(project);
		Assert.isLegal(repository != null, "Cannot commit project to repository. ");
		return commit(project, commitMessage, repository, monitor);
	}

	public static RevCommit commit(IProject project, IProgressMonitor monitor) throws CoreException {
		return commit(project, "Commit from JBoss Tools", monitor);
	}

	private static RevCommit commit(IProject project, String commitMessage, Repository repository,
			IProgressMonitor monitor)
			throws CoreException {
		Assert.isLegal(project != null, "Could not commit project. No project provided");
		Assert.isLegal(
				repository != null,
				MessageFormat
						.format("Could not commit. Project \"{0}\" is not connected to a repository (call #connect(project, repository) first)",
								project.getName()));
		/**
		 * TODO: add capability to commit selectively
		 */
		UserConfig userConfig = getUserConfig(repository);
		CommitOperation op = new CommitOperation(
				null,
				null,
				null,
				getFormattedUser(userConfig.getAuthorName(), userConfig.getAuthorEmail()),
				getFormattedUser(userConfig.getCommitterName(), userConfig.getCommitterEmail()),
				commitMessage);
		op.setCommitAll(true);
		op.setRepository(repository);
		op.execute(monitor);
		return op.getCommit();
	}

	public static List<URIish> getRemoteURIs(IProject p) throws CoreException {
		RemoteConfig rc = getRemoteConfig(p);
		if (rc != null) {
			return rc.getURIs();
		}
		return new ArrayList<URIish>();
	}

	public static RemoteConfig getRemoteConfig(IProject project) throws CoreException {
		Repository rep = getRepository(project);
		if (rep != null) {
			return getRemoteConfig(rep);
		}
		return null;
	}

	/**
	 * Pushes the current branch of the given repository to the remote
	 * repository that it originates from.
	 * 
	 * @param repository
	 *            the repository that shall be pushed
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws CoreException
	 *             core exception is thrown if the push could not be executed
	 */
	public static PushOperationResult push(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		return push(repository, getRemoteConfig(repository), false, monitor);
	}

	/**
	 * Pushes the given repository to the remote repo with the given name.
	 * 
	 * @param remote
	 * @param repository
	 * @param monitor
	 * @throws CoreException
	 * 
	 * @see git config file: "[remote..."
	 * @see #getAllRemoteConfigs(Repository)
	 * @see RemoteConfig#getName()
	 */
	public static PushOperationResult push(String remote, Repository repository, IProgressMonitor monitor)
			throws CoreException {
		RemoteConfig remoteConfig = getRemoteByName(remote, repository);
		return push(repository, remoteConfig, false, monitor);
	}

	public static PushOperationResult pushForce(String remote, Repository repository, IProgressMonitor monitor)
			throws CoreException {
		RemoteConfig remoteConfig = getRemoteByName(remote, repository);
		return push(repository, remoteConfig, true, monitor);
	}

	private static PushOperationResult push(Repository repository, RemoteConfig remoteConfig,
			boolean force, IProgressMonitor monitor) throws CoreException {
		try {
			if (remoteConfig == null) {
				throw new CoreException(createStatus(null, "Repository \"{0}\" has no remote repository configured",
						repository.toString()));
			}
			PushOperation op = createPushOperation(remoteConfig, repository, force);
			op.run(monitor);
			PushOperationResult pushResult = op.getOperationResult();
			if (hasFailedEntries(pushResult)) {
				throw new CoreException(
						EGitCoreActivator.createErrorStatus(
								NLS.bind("Could not push repository {0}: {1}",
										repository.toString(), getErrors(pushResult))
								, null));
			}
			return pushResult;
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(createStatus(e, "Could not push repo {0}", repository.toString()));
		}
	}

	private static String getErrors(PushOperationResult pushResult) {
		StringBuilder builder = new StringBuilder();
		for (RemoteRefUpdate failedUpdate : getFailedUpdates(pushResult)) {
			builder.append(MessageFormat.format(
					"push from {0} to {1} was {2}", failedUpdate.getSrcRef(), failedUpdate.getRemoteName(),
					failedUpdate.getStatus()));
		}
		return builder.toString();

	}

	private static PushOperation createPushOperation(RemoteConfig remoteConfig, Repository repository, boolean force)
			throws CoreException {

		Collection<URIish> pushURIs = getPushURIs(remoteConfig);
		Collection<RefSpec> pushRefSpecs = createForceRefSpecs(force, getPushRefSpecs(remoteConfig));
		PushOperationSpecification pushSpec = createPushSpec(pushURIs, pushRefSpecs, repository);
		return new PushOperation(repository, pushSpec, false, PUSH_TIMEOUT);
	}

	/**
	 * Creates a push operation specification for the given push uris to the
	 * given push operation specification.
	 * 
	 * @param pushURIs
	 *            the push uri's
	 * @param pushRefSpecs
	 *            the push ref specs
	 * @param repository
	 *            the repository
	 * @return the push operation specification
	 * @throws CoreException
	 *             the core exception
	 */
	private static PushOperationSpecification createPushSpec(Collection<URIish> pushURIs,
			Collection<RefSpec> pushRefSpecs,
			Repository repository) throws CoreException {
		try {
			PushOperationSpecification pushSpec = new PushOperationSpecification();
			for (URIish uri : pushURIs) {
				Collection<RemoteRefUpdate> remoteRefUpdates =
						Transport.open(repository, uri).findRemoteRefUpdatesFor(pushRefSpecs);
				pushSpec.addURIRefUpdates(uri, remoteRefUpdates);
			}
			return pushSpec;
		} catch (NotSupportedException e) {
			throw new CoreException(createStatus(e, "Could not connect repository \"{0}\" to a remote",
					repository.toString()));
		} catch (IOException e) {
			throw new CoreException(createStatus(e,
					"Could not convert remote specifications for repository \"{0}\" to a remote",
					repository.toString()));
		}
	}

	/**
	 * Gets the push uris from the given remoteConfig.
	 * 
	 * @param remoteConfig
	 *            the remote config
	 * @return the push ur is
	 */
	private static Collection<URIish> getPushURIs(RemoteConfig remoteConfig) {
		List<URIish> pushURIs = new ArrayList<URIish>();
		for (URIish uri : remoteConfig.getPushURIs()) {
			pushURIs.add(uri);
		}
		if (pushURIs.isEmpty()
				&& !remoteConfig.getURIs().isEmpty()) {
			pushURIs.add(remoteConfig.getURIs().get(0));
		}
		return pushURIs;
	}

	/**
	 * Gets the push RefSpecs from the given remote configuration. If none is
	 * defined, a default refspec is returned with
	 * {@link #DEFAULT_REFSPEC_SOURCE} and {@link #DEFAULT_REFSPEC_DESTINATION}.
	 * 
	 * @param config
	 *            the remote config to get the push specs from
	 * @return the push specs to use for the given remote configuration.
	 */
	private static List<RefSpec> getPushRefSpecs(RemoteConfig config) {
		List<RefSpec> pushRefSpecs = new ArrayList<RefSpec>();
		List<RefSpec> remoteConfigPushRefSpecs = config.getPushRefSpecs();
		if (!remoteConfigPushRefSpecs.isEmpty()) {
			pushRefSpecs.addAll(remoteConfigPushRefSpecs);
		} else {
			// default is to push current HEAD to remote MASTER
			pushRefSpecs.add(new RefSpec()
					.setSource(DEFAULT_REFSPEC_SOURCE).setDestination(DEFAULT_REFSPEC_DESTINATION));
		}
		return pushRefSpecs;
	}

	private static Collection<RefSpec> createForceRefSpecs(boolean forceUpdate, Collection<RefSpec> refSpecs) {
		List<RefSpec> newRefSpecs = new ArrayList<RefSpec>();
		for (RefSpec refSpec : refSpecs) {
			newRefSpecs.add(refSpec.setForceUpdate(forceUpdate));
		}
		return newRefSpecs;
	}

	public static boolean hasFailedEntries(PushOperationResult pushOperationResult) {
		return !getFailedUpdates(pushOperationResult).isEmpty();
	}

	public static Collection<RemoteRefUpdate> getFailedUpdates(PushOperationResult pushOperationResult) {
		List<RemoteRefUpdate> allFailedRefUpdates = new ArrayList<RemoteRefUpdate>();
		for (URIish uri : pushOperationResult.getURIs()) {
			allFailedRefUpdates.addAll(getFailedUpdates(uri, pushOperationResult));
		}
		return allFailedRefUpdates;
	}

	public static Collection<RemoteRefUpdate> getFailedUpdates(URIish uri, PushOperationResult pushOperationResult) {
		return getFailedUpdates(pushOperationResult.getPushResult(uri));
	}

	private static Collection<RemoteRefUpdate> getFailedUpdates(PushResult pushResult) {
		List<RemoteRefUpdate> failedRefUpdates = new ArrayList<RemoteRefUpdate>();
		if (pushResult == null
				|| pushResult.getRemoteUpdates() == null) {
			return failedRefUpdates;
		}
		for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
			if (org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK
				!= update.getStatus()) {
				failedRefUpdates.add(update);
			}
		}
		return failedRefUpdates;
	}

	/**
	 * Gets the repository that is configured to the given project.
	 * 
	 * @param project
	 *            the project
	 * @return the repository
	 */
	public static Repository getRepository(IProject project) {
		Assert.isLegal(project != null, "Could not get repository. No project provided");

		RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
		if (repositoryMapping == null) {
			return null;
		}
		return repositoryMapping.getRepository();
	}

	/**
	 * Gets the UserConfig from the given repository. The UserConfig of a repo
	 * holds the default author and committer.
	 * 
	 * @param repository
	 *            the repository
	 * @return the user configuration
	 * @throws CoreException
	 * 
	 * @see PersonIdent(Repository)
	 * @see CommittHelper#calculateCommitInfo
	 */
	private static UserConfig getUserConfig(Repository repository) throws CoreException {
		Assert.isLegal(repository != null, "Could not get user configuration. No repository provided.");

		if (repository.getConfig() == null) {
			throw new CoreException(createStatus(null,
					"no user configuration (author, committer) are present in repository \"{0}\"",
					repository.toString()));
		}
		return repository.getConfig().get(UserConfig.KEY);
	}

	private static String getFormattedUser(String name, String email) {
		return new StringBuilder().append(name).append(" <").append(email).append('>').toString();
	}

	/**
	 * Returns the configuration of the remote repository that is set to the
	 * given repository.
	 * <code>null</null> if none was configured or if there's no remote repo configured.
	 * 
	 * @param repository
	 *            the repository to get the remote repo configuration from
	 * @return the configurtion of the remote repository
	 * @throws CoreException
	 *             the core exception
	 */
	private static RemoteConfig getRemoteConfig(Repository repository) throws CoreException {
		Assert.isLegal(repository != null, "Could not get configuration. No repository provided.");

		String currentBranch = getCurrentBranch(repository);
		String remote = getRemoteName(currentBranch, repository);
		return getRemoteByName(remote, repository);
	}

	/**
	 * Returns the remote config for the given remote in the given repository
	 * 
	 * @param remote
	 * @param repository
	 * @return
	 * @throws CoreException
	 */
	public static RemoteConfig getRemoteByName(String remote, Repository repository) throws CoreException {
		Assert.isLegal(repository != null, "Could not get configuration. No repository provided.");

		List<RemoteConfig> allRemotes = getAllRemoteConfigs(repository);
		return getRemoteConfig(remote, allRemotes);
	}

	private static String getCurrentBranch(Repository repository) throws CoreException {
		String branch = null;
		try {
			branch = repository.getBranch();
		} catch (IOException e) {
			throw new CoreException(createStatus(e, "Could not get current branch on repository \"{0}\"",
					repository.toString()));
		}
		return branch;
	}

	/**
	 * Gets the remote config with the given name from the list of remote
	 * configs. Returns <code>null</code> if it was not found.
	 * 
	 * @param remoteName
	 *            the remote name
	 * @param remoteRepositories
	 *            the remote repositories
	 * @return the remote config
	 * 
	 * @see #getAllRemoteConfigs(Repository)
	 */
	public static RemoteConfig getRemoteConfig(String name, List<RemoteConfig> remoteConfigs) {
		Assert.isLegal(name != null);
		RemoteConfig remoteConfig = null;
		for (RemoteConfig config : remoteConfigs) {
			if (name != null && config.getName().equals(name)) {
				remoteConfig = config;
				break;
			}
		}
		return remoteConfig;
	}

	/**
	 * Returns all the remote configs from the given repository.
	 * 
	 * @param repository
	 *            the repository to retrieve the remote configs of
	 * @return the remote configs that are available on the repository
	 * @throws CoreException
	 */
	public static List<RemoteConfig> getAllRemoteConfigs(Repository repository) throws CoreException {
		try {
			return RemoteConfig.getAllRemoteConfigs(repository.getConfig());
		} catch (URISyntaxException e) {
			throw new CoreException(createStatus(e, "Could not get all remote repositories for repository \"{0}\"",
					repository.toString()));
		}
	}

	/**
	 * Returns the first configured remote in the given repository whose url matches the given pattern.
	 * 
	 * @param pattern
	 * @param repository
	 * @return
	 * @throws CoreException
	 */
	public static RemoteConfig getRemoteByUrl(Pattern pattern, Repository repository) throws CoreException {
		for (RemoteConfig config : getAllRemoteConfigs(repository)) {
			if (hasRemoteUrl(pattern, config)) {
				return config;
			}
		}
		return null;
	}

	public static boolean hasRemoteUrl(Pattern pattern, RemoteConfig config) {
		for (URIish uri : config.getURIs()) {
			Matcher matcher = pattern.matcher(uri.toString());
			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if the given repository has a remote with the
	 * given name,
	 * 
	 * @param name
	 *            the remote name that we're looking for
	 * @param repository
	 *            the repository to look at
	 * @return true if the given repo has a remote with the given name
	 * @throws CoreException
	 */
	public static boolean hasRemote(String name, Repository repository) throws CoreException {
		return getRemoteByName(name, repository) != null;
	}

	/**
	 * Returns <code>true</code> if the given repository has a remote config
	 * with the given name and url.
	 * 
	 * @param name
	 *            the name that the remote config shall match
	 * @param url
	 *            the url that the remote config shall match
	 * @param repository
	 *            the repository that is searched
	 * @return
	 * @throws CoreException
	 */
	public static boolean hasRemote(String name, String url, Repository repository) throws CoreException {
		RemoteConfig remoteConfig = getRemoteByName(name, repository);
		if (remoteConfig == null) {
			return false;
		}

		return hasRemoteUrl(Pattern.compile(RegexUtils.toPatternString(url)), remoteConfig);
	}

	/**
	 * Returns <code>true</code> if the given repository has several configured
	 * remotes
	 * 
	 * @param repository
	 * @return
	 * @throws CoreException
	 * 
	 * @see git config file: "[remote..."
	 * @see #getAllRemoteConfigs
	 * @see RemoteConfig#getAllRemoteConfigs
	 * 
	 */
	public static boolean hasMultipleRemotes(Repository repository) throws CoreException {
		return getAllRemoteConfigs(repository).size() > 1;
	}

	/**
	 * Returns the name of the remote repository for the given branch. If
	 * there's no current branch or no remote configured to it, the default
	 * remote is returned ("origin").
	 * 
	 * @param branch
	 *            the branch
	 * @param repository
	 *            the repository
	 * @return the remote name
	 */
	private static String getRemoteName(String branch, Repository repository) {
		String remoteName = null;
		if (ObjectId.isId(branch)) {
			remoteName = Constants.DEFAULT_REMOTE_NAME;
		} else {
			remoteName = repository.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_REMOTE_SECTION);
			if (remoteName == null) {
				remoteName = Constants.DEFAULT_REMOTE_NAME;
			}
		}

		return remoteName;
	}

	public static void addRemoteTo(String remoteName, String uri, Repository repository)
			throws MalformedURLException, URISyntaxException, IOException {
		addRemoteTo(remoteName, new URIish(uri), repository);
	}

	/**
	 * Adds the given uri of a remote repository to the given repository by the
	 * given name.
	 * 
	 * @param remoteName
	 *            the name to use for the remote repository
	 * @param uri
	 *            the uri of the remote repository
	 * @param repository
	 *            the repository to add the remote to
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void addRemoteTo(String remoteName, URIish uri, Repository repository)
			throws URISyntaxException, MalformedURLException,
			IOException {
		StoredConfig config = repository.getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, remoteName);
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();
	}

	private static IStatus createStatus(Exception e, String message, String... arguments) throws CoreException {
		IStatus status = null;
		if (e == null) {
			status = new Status(IStatus.ERROR, EGitCoreActivator.PLUGIN_ID, NLS.bind(message, arguments));
		} else {
			status = new Status(IStatus.ERROR, EGitCoreActivator.PLUGIN_ID, NLS.bind(message, arguments), e);
		}
		return status;
	}

	/**
	 * Returns <code>true</code> if the given repository has uncommitted
	 * changes. Uncommitted changes taken into account are
	 * <ul>
	 * <li>freshly added (but uncommitted resources)</li>
	 * <li>changed (but uncommitted)</li>
	 * <li>modified (but uncommitted resources)</li>
	 * <li>removed (but uncommitted resources)</li>
	 * </ul>
	 * 
	 * @param repository
	 *            the repository to check for uncommitted changes
	 * @return
	 * @throws IOException
	 * @throws NoWorkTreeException
	 */
	public static boolean isDirty(Repository repository)
			throws NoWorkTreeException, IOException {
		try {
			boolean hasChanges = false;
			org.eclipse.jgit.api.Status repoStatus = new Git(repository)
					.status().call();
			hasChanges |= !repoStatus.getAdded().isEmpty();
			hasChanges |= !repoStatus.getChanged().isEmpty();
			hasChanges |= !repoStatus.getModified().isEmpty();
			hasChanges |= !repoStatus.getRemoved().isEmpty();
			hasChanges |= !repoStatus.getConflicting().isEmpty();
			hasChanges |= !repoStatus.getMissing().isEmpty();
			return hasChanges;
		} catch (Exception e) {
			// ugly workaround to allow us to use egit 2.0 while staying
			// compatible
			if (e instanceof NoWorkTreeException) {
				throw (NoWorkTreeException) e;
			} else if (e instanceof IOException) {
				throw (IOException) e;
			}
			throw new RuntimeException(e);
		}
	}

	public static int countCommitableChanges(IProject project, IServer server, IProgressMonitor monitor) throws CoreException {
		try {
			Repository repo = getRepository(project);
			if( repo == null ) {
				throw new CoreException(new Status(IStatus.ERROR, EGitCoreActivator.PLUGIN_ID, "Project " + project + " has no repository associated with it."));
			}
			Set<String> commitable = getCommitableChanges(repo, server, monitor);
			return commitable.size();
		} catch (IOException ioe) {
			throw new CoreException(new Status(IStatus.ERROR, EGitCoreActivator.PLUGIN_ID, "Unable to count commitable resources", ioe));
		}
	}

	private static Set<String> getCommitableChanges(Repository repo, IServer server, IProgressMonitor monitor)
			throws IOException {
		
		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(monitor);
		IndexDiff indexDiff = new IndexDiff(repo, Constants.HEAD,
				IteratorService.createInitialIterator(repo));
		indexDiff.diff(jgitMonitor, 0, 0,
				NLS.bind("Repository: {0}", repo.getDirectory().getPath()));
		Set<String> set = new HashSet<String>();
		if (commitAddedResources(server))
			set.addAll(indexDiff.getAdded());
		if (commitChangedResources(server))
			set.addAll(indexDiff.getChanged());
		if (commitConflictingResources(server))
			set.addAll(indexDiff.getConflicting());
		if (commitMissingResources(server))
			set.addAll(indexDiff.getMissing());
		if (commitModifiedResources(server))
			set.addAll(indexDiff.getModified());
		if (commitRemovedResources(server))
			set.addAll(indexDiff.getRemoved());
		if (commitUntrackedResources(server))
			set.addAll(indexDiff.getUntracked());

		return set;
	}

	/*
	 * Current behaviour is to commit only: added, changed, modified, removed
	 * 
	 * These can be customized as properties on the server one day, if we wish,
	 * such that each server can have custom settings, or, they can be global
	 * settings
	 */
	public static boolean commitAddedResources(IServer server) {
		return true;
	}

	public static boolean commitChangedResources(IServer server) {
		return true;
	}

	public static boolean commitConflictingResources(IServer server) {
		return false;
	}

	public static boolean commitMissingResources(IServer server) {
		return false;
	}

	public static boolean commitModifiedResources(IServer server) {
		return true;
	}

	public static boolean commitRemovedResources(IServer server) {
		return true;
	}

	public static boolean commitUntrackedResources(IServer server) {
		return false;
	}

}
