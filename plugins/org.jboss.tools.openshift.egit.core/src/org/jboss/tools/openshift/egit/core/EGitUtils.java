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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.egit.core.internal.EGitCoreActivator;

/**
 * The Class EGitUtils.
 * 
 * @author André Dietisheim
 */
public class EGitUtils {

	private static final RefSpec DEFAULT_PUSH_REF_SPEC =
			new RefSpec("refs/heads/*:refs/remotes/origin/*"); //$NON-NLS-1$

	private static final int PUSH_TIMEOUT = 10 * 1024;

	/**
	 * Commits the given project to it's configured repository.
	 * 
	 * @param project
	 *            the project
	 * @param monitor
	 *            the monitor
	 * @throws CoreException
	 *             the core exception
	 */
	public static void commit(IProject project, String commitMessage, IProgressMonitor monitor) throws CoreException {
		Assert.isLegal(project != null, "Cannot commit project. No project provided");
		/**
		 * TODO: add capability to commit selectively
		 */
		Repository repository = getRepository(project);
		if (repository == null) {
			throw new CoreException(createStatus(null,
					"Could not commit. Project \"{0}\" is not attached to a git repo", project.getName()));
		}
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
	}

	public static void commit(IProject project, IProgressMonitor monitor) throws CoreException {
		commit(project, "Commit from JBoss Tools", monitor);
	}
	
	/**
	 * Pushes the given repository to the remote repository it's current branch
	 * originates from.
	 * 
	 * @param repository
	 *            the repository that shall be pushed
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws CoreException
	 *             core exception is thrown if the push could not be executed
	 */
	public static void push(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		try {
			RemoteConfig remoteConfig = getRemoteConfig(repository);
			if (remoteConfig == null) {
				throw new CoreException(createStatus(null, "Repository \"{0}\" has no remote repository configured",
						repository.toString()));
			}
			PushOperation pushOperation = createPushOperation(remoteConfig, repository);
			pushOperation.run(monitor);
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(createStatus(e, "Could not push repo {0}", repository.toString()));
		}
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
	public static void push(String remote, Repository repository, IProgressMonitor monitor)
			throws CoreException {
		try {
			RemoteConfig remoteConfig = getRemoteConfig(remote, repository);
			if (remoteConfig == null) {
				throw new CoreException(createStatus(null,
						"Repository \"{0}\" has no remote repository with the name \"{1}\"",
						repository.toString(), remote));
			}
			createPushOperation(remoteConfig, repository).run(monitor);
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(createStatus(e, "Could not push repo {0}", repository.toString()));
		}
	}

	//
	// only available in EGit 1.1
	//
	// private static PushOperation createPushOperation(String remoteName,
	// Repository repository) { return new PushOperation(repository, remoteName,
	// false, PUSH_TIMEOUT); }
	//

	private static PushOperation createPushOperation(RemoteConfig remoteConfig, Repository repository)
			throws CoreException {

		PushOperationSpecification spec = new PushOperationSpecification();
		List<URIish> pushToUris = getPushURIs(remoteConfig);
		List<RefSpec> pushRefSpecs = getPushRefSpecs(remoteConfig);
		addURIRefToPushSpecification(pushToUris, pushRefSpecs, repository, spec);

		// return new PushOperation(repository, spec, false, PUSH_TIMEOUT);
		// TODO: fix pushoperation to really use the spec (currently seems like
		// it does not work so we push everything to the remote)
		return new PushOperation(repository, remoteConfig.getName(), false, PUSH_TIMEOUT);
	}

	/**
	 * Adds the given push uris to the given push operation specification.
	 * 
	 * @param urisToPush
	 *            the uris to push
	 * @param pushRefSpecs
	 *            the push ref specs
	 * @param repository
	 *            the repository
	 * @param spec
	 *            the spec
	 * @throws CoreException
	 *             the core exception
	 */
	private static void addURIRefToPushSpecification(List<URIish> urisToPush, List<RefSpec> pushRefSpecs,
			Repository repository, PushOperationSpecification spec) throws CoreException {
		for (URIish uri : urisToPush) {
			try {
				spec.addURIRefUpdates(uri,
						Transport.open(repository, uri).findRemoteRefUpdatesFor(pushRefSpecs));
			} catch (NotSupportedException e) {
				throw new CoreException(createStatus(e, "Could not connect repository \"{0}\" to a remote",
						repository.toString()));
			} catch (IOException e) {
				throw new CoreException(createStatus(e,
						"Could not convert remote specifications for repository \"{0}\" to a remote",
						repository.toString()));
			}
		}
	}

	/**
	 * Gets the push uris from the given remoteConfig.
	 * 
	 * @param remoteConfig
	 *            the remote config
	 * @return the push ur is
	 */
	private static List<URIish> getPushURIs(RemoteConfig remoteConfig) {
		List<URIish> urisToPush = new ArrayList<URIish>();
		for (URIish uri : remoteConfig.getPushURIs())
			urisToPush.add(uri);
		if (urisToPush.isEmpty() && !remoteConfig.getURIs().isEmpty())
			urisToPush.add(remoteConfig.getURIs().get(0));
		return urisToPush;
	}

	/**
	 * Gets the push RefSpecs from the given remote configuration.
	 * 
	 * @param config
	 *            the config
	 * @return the push ref specs
	 */
	private static List<RefSpec> getPushRefSpecs(RemoteConfig config) {
		List<RefSpec> pushRefSpecs = new ArrayList<RefSpec>();
		pushRefSpecs.addAll(config.getPushRefSpecs());
		if (pushRefSpecs.isEmpty()) {
			// default push to all branches
			pushRefSpecs.add(DEFAULT_PUSH_REF_SPEC);
		}
		return pushRefSpecs;
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
	 * given repository. Returns
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
		return getRemoteConfig(remote, repository);
	}

	/**
	 * Returns the remote config for the given remote in the given repository
	 * 
	 * @param remote
	 * @param repository
	 * @return
	 * @throws CoreException
	 */
	private static RemoteConfig getRemoteConfig(String remote, Repository repository) throws CoreException {
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
	 * repositories. Returns <code>null</code> if it was not found.
	 * 
	 * @param remoteName
	 *            the remote name
	 * @param remoteRepositories
	 *            the remote repositories
	 * @return the remote config
	 */
	private static RemoteConfig getRemoteConfig(String remoteName, List<RemoteConfig> remoteRepositories) {
		RemoteConfig defaultConfig = null;
		RemoteConfig configuredConfig = null;
		for (RemoteConfig config : remoteRepositories) {
			// if (config.getName().equals(Constants.DEFAULT_REMOTE_NAME))
			// defaultConfig = config;
			if (remoteName != null && config.getName().equals(remoteName))
				configuredConfig = config;
		}

		if (configuredConfig == null) {
			return defaultConfig;
		}
		return configuredConfig;
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
	 * Returns the name of the remote repository of the given branch. If there's
	 * no current branch or no remote configured to it, the default remote is
	 * returned ("origin").
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
		}

		if (remoteName == null) {
			remoteName = Constants.DEFAULT_REMOTE_NAME;
		}

		return remoteName;
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
	
	public static int countCommitableChanges(IProject project, IProgressMonitor monitor) {
		try {
			Repository repo = getRepository(project);
			
			EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(monitor);
			IndexDiff indexDiff = new IndexDiff(repo, Constants.HEAD,
					IteratorService.createInitialIterator(repo));
			indexDiff.diff(jgitMonitor, 0, 0, NLS.bind(
					UIText.CommitActionHandler_repository, repo.getDirectory().getPath()));
//			System.out.println(indexDiff.getAdded().size());
//			System.out.println(indexDiff.getChanged().size());
//			System.out.println(indexDiff.getConflicting().size());
//			System.out.println(indexDiff.getMissing().size());
//			System.out.println(indexDiff.getModified().size());
//			System.out.println(indexDiff.getRemoved().size());
//			System.out.println(indexDiff.getUntracked().size());
			
			return indexDiff.getModified().size();
		} catch( IOException ioe ) {
		}
		return -1;
	}
}
