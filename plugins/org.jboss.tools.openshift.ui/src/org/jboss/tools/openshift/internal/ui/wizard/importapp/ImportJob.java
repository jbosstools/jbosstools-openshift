/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.ImportFailedException;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.WontOverwriteException;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;

public class ImportJob extends WorkspaceJob {

	private String gitUrl;
	private File cloneDestination;
	private String gitRef;
	private Collection<String> filters;
	private boolean checkoutBranch;
	private boolean reuseGitRepository;
	
	/**
	 * Creates an import job that will import a project from an eixisting git
	 * repo at given repo location and will checkout the branch (provided in
	 * #setGitRef) if told to do so via the switch checkoutBranch.
	 * 
	 * @param gitUrl
	 * @param repoLocation
	 * @param checkoutBranch
	 */
	public ImportJob(String gitUrl, String gitRef, File repoLocation, boolean checkoutBranch) {
		this(gitUrl, gitRef, repoLocation, checkoutBranch, true);
	}

	/**
	 * A constructor to clone from a git url and then import the project.
	 * 
	 * @param gitUrl
	 * @param cloneDestination
	 * @param delegatingMonitor
	 */
	protected ImportJob(String gitUrl, String gitRef, File cloneDestination) {
		this(gitUrl, gitRef, cloneDestination, false, false);
	}

	protected ImportJob(String gitUrl, String gitRef, File cloneDestination, boolean checkoutBranch, boolean reuseGitRepository) {
		super("Importing project to workspace...");
		setRule(ResourcesPlugin.getWorkspace().getRoot());
		this.gitUrl = gitUrl;
		this.gitRef = gitRef;
		this.cloneDestination = cloneDestination;
		this.checkoutBranch = checkoutBranch;
		this.reuseGitRepository = reuseGitRepository;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		try {
			if (reuseGitRepository) {
				new ImportProjectOperation(gitUrl, gitRef, cloneDestination, filters, checkoutBranch).execute(monitor);
			} else {
				new ImportProjectOperation(gitUrl, gitRef, cloneDestination, filters).execute(monitor);
			}
			return Status.OK_STATUS;
		} catch (final WontOverwriteException e) {
			openError("Project already present", e.getMessage());
			return Status.CANCEL_STATUS;
		} catch (final ImportFailedException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(
					NLS.bind("Could not import project from {0}.", e, gitUrl));
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(
					NLS.bind("Could not import project from {0}.", e, gitUrl));
		} catch (OpenShiftException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus("Could not import project to the workspace.", e);
		} catch (URISyntaxException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus("The url of the remote git repository is not valid", e);
		} catch (InvocationTargetException e) {
			TransportException te = getTransportException(e);
			if (te != null) {
				return OpenShiftUIActivator.statusFactory().errorStatus(
						"Could not clone the repository. Authentication failed.\n"
								+ " Please make sure that you added your private key to the ssh preferences.", te);
			} else {
				return OpenShiftUIActivator.statusFactory().errorStatus(
						"An exception occurred while creating local git repository.", e);
			}
		} catch (CoreException e) {
			return StatusFactory.getMultiStatusInstance(
					0, OpenShiftUIActivator.PLUGIN_ID, "Could not import project to the workspace.", null, e.getStatus() );
		} catch (InterruptedException e) {
			if(monitor.isCanceled()) return Status.CANCEL_STATUS;
			return OpenShiftUIActivator.statusFactory().errorStatus("Could not import project to the workspace.", e);
		} catch (Exception e) {
			return OpenShiftUIActivator.statusFactory().errorStatus("Could not import project to the workspace.", e);
		} finally {
			monitor.done();
		}
	}

	protected TransportException getTransportException(Throwable t) {
		if (t instanceof TransportException) {
			return (TransportException) t;
		} else if (t instanceof InvocationTargetException) {
			return getTransportException(((InvocationTargetException) t).getTargetException());
		} else if (t instanceof Exception) {
			return getTransportException(((Exception) t).getCause());
		}
		return null;
	}
	
	protected void openError(final String title, final String message) {
		final Shell shell = UIUtils.getShell();
		if (shell == null
				|| shell.isDisposed()) {
			OpenShiftUIActivator.getDefault().getLogger().logError(message);
		} else {
			UIUtils.getShell().getDisplay().syncExec(new Runnable() {
	
				@Override
				public void run() {
					MessageDialog.openError(shell, title, message);
				}
			});
		}
	}

	public ImportJob setFilters(Collection<String> filters) {
		this.filters = filters;
		return this;
	}
}
