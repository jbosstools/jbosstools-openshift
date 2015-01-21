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
package org.jboss.tools.openshift.express.internal.ui.wizard.markers;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

/**
 * @author André Dietisheim
 */
public class ConfigureMarkersWizard extends Wizard {

	private IProject project;
	private ConfigureMarkersWizardPage configureMarkersPage;

	public ConfigureMarkersWizard(IProject project) {
		this.project = project;
		setNeedsProgressMonitor(true);
		setWindowTitle(NLS.bind("Configure OpenShift Markers for project {0}", project.getName()));
	}

	@Override
	public boolean performFinish() {
		try {
			Collection<IOpenShiftMarker> removedMarkers = configureMarkersPage.getRemovedMarkers();
			Collection<IOpenShiftMarker> addedMarkers = configureMarkersPage.getAddedMarkers();
			AddRemoveMarkersJob job = new AddRemoveMarkersJob(removedMarkers, addedMarkers, project);
			IStatus result = WizardUtils.runInWizard(job, job.getDelegatingProgressMonitor(), getContainer());
			return result.isOK();
		} catch (Exception e) {
			ExpressUIActivator.log(e);
			return false;
		}
	}

	@Override
	public void addPages() {
		addPage(this.configureMarkersPage = new ConfigureMarkersWizardPage(project, this));
	}

	private class AddRemoveMarkersJob extends AbstractDelegatingMonitorJob {

		private Collection<IOpenShiftMarker> markersToAdd;
		private Collection<IOpenShiftMarker> markersToRemove;
		private IProject project;

		public AddRemoveMarkersJob(Collection<IOpenShiftMarker> removedMarkers,
				Collection<IOpenShiftMarker> addedMarkers, IProject project) {
			super(NLS.bind("Adding/Removing markers in project {0}", project.getName()));
			this.project = project;
			this.markersToAdd = addedMarkers;
			this.markersToRemove = removedMarkers;
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			MultiStatus multiStatus = 
					new MultiStatus(ExpressUIActivator.PLUGIN_ID, 0, "Error(s) occurred while adding/removing marker(s)",null);
			Repository repository = EGitUtils.getRepository(project);
			removeMarkers(markersToRemove, project, monitor, multiStatus);
			addMarkers(markersToAdd, project, repository, monitor, multiStatus);
			return multiStatus;
		}

		private IStatus removeMarkers(Collection<IOpenShiftMarker> removedMarkers, IProject project,
				IProgressMonitor monitor, MultiStatus multiStatus) {
			monitor.beginTask(NLS.bind("Removing markers from project {0}", project.getName()), removedMarkers.size());
			for (IOpenShiftMarker marker : removedMarkers) {
				try {
					monitor.subTask("Removing marker {0}...");
					monitor.internalWorked(1);
					marker.removeFrom(project, monitor);
				} catch (CoreException e) {
					multiStatus.add(ExpressUIActivator.createErrorStatus(
							NLS.bind("Could not remove marker {0}", marker.getName()), e));
				}
			}
			return multiStatus;
		}

		private IStatus addMarkers(Collection<IOpenShiftMarker> markersToAdd, IProject project,
				Repository repository, IProgressMonitor monitor, MultiStatus multiStatus) {
			monitor.beginTask(NLS.bind("Adding markers to project {0}", project.getName()), markersToAdd.size());
			for (IOpenShiftMarker marker : markersToAdd) {
				try {
					monitor.subTask("Adding marker {0}...");
					monitor.internalWorked(1);
					IResource markerFile = marker.addTo(project, monitor);
					if (repository != null) {
						EGitUtils.addToRepository(Collections.singletonList(markerFile), monitor);
					}
					
				} catch (CoreException e) {
					multiStatus.add(ExpressUIActivator.createErrorStatus(
							NLS.bind("Could not add marker {0}", marker.getName()), e));
				}
			}
			return multiStatus;
		}

	}
}
