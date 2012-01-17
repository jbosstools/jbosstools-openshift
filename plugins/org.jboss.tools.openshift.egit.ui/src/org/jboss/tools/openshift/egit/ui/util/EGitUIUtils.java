/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.egit.ui.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

/**
 * @author Andre Dietisheim
 */
public class EGitUIUtils {

	/**
	 * A post clone task that will add the new repo (that was created when
	 * cloning) to the EGit repositories view.
	 */
	public static final PostCloneTask ADD_TO_REPOVIEW_TASK = new PostCloneTask() {

		@Override
		public void execute(Repository repository, IProgressMonitor monitor) throws CoreException {
			RepositoryUtil repositoryUtil = Activator.getDefault().getRepositoryUtil();
			repositoryUtil.addConfiguredRepository(repository.getDirectory());
		}
	};
	
	public static String getEGitDefaultRepositoryPath() {
		return Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);
	}

	/**
	 * The EGit UI plugin initializes the ssh factory to present the user a
	 * passphrase prompt if the ssh key was not read yet. If this initialization
	 * is not executed, the ssh connection to the git repo would just fail with
	 * an authentication error. We therefore have to make sure that the EGit UI
	 * plugin is started and initializes the JSchConfigSessionFactory.
	 * <p>
	 * EGit initializes the SshSessionFactory with the EclipseSshSessionFactory.
	 * The EclipseSshSessionFactory overrides JschConfigSessionFactory#configure
	 * to present a UserInfoPrompter if the key passphrase was not entered
	 * before.
	 * 
	 * @see Activator#start(org.osgi.framework.BundleContext)
	 * @see Activator#setupSSH
	 * @see JschConfigSessionFactory#configure
	 * @see EclipseSshSessionFactory#configure
	 */
	public static void ensureEgitUIIsStarted() {
		Activator.getDefault();
	}

}
