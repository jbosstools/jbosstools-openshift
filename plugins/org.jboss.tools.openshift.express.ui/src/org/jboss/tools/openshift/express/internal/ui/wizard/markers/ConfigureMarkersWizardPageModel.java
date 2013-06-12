/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.core.marker.OpenShiftMarkers;

/**
 * @author André Dietisheim
 */
public class ConfigureMarkersWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_AVAILABLE_MARKERS = "availableMarkers";
	public static final String PROPERTY_PRESENT_MARKERS = "presentMarkers";
	public static final String PROPERTY_SELECTED_MARKER = "selectedMarker";
	
	private List<IOpenShiftMarker> availableMarkers;
	private Set<IOpenShiftMarker> presentMarkers = new HashSet<IOpenShiftMarker>();
	private IOpenShiftMarker selectedMarker;
	private IProject project;

	public ConfigureMarkersWizardPageModel(IProject project) {
		this.project = project;
	}
	
	public void loadMarkers() throws CoreException {
		OpenShiftMarkers markers = new OpenShiftMarkers(project);
		setAvailableMarkers(markers.getAll());
		setPresentMarkers(toSet(markers.getPresent()));
	}
	
	private Set<IOpenShiftMarker> toSet(List<IOpenShiftMarker> markers) {
		Set<IOpenShiftMarker> markersSet = new HashSet<IOpenShiftMarker>();
		markersSet.addAll(markers);
		return markersSet; 
	}
	
	public void setAvailableMarkers(List<IOpenShiftMarker> markers) {
		firePropertyChange(
				PROPERTY_AVAILABLE_MARKERS, this.availableMarkers, this.availableMarkers = markers);
	}

	public List<IOpenShiftMarker> getAvailableMarkers() {
		return availableMarkers;
	}

	public Set<IOpenShiftMarker> getPresentMarkers() throws CoreException {
		return presentMarkers;
	}

	public void setPresentMarkers(Set<IOpenShiftMarker> markers) throws CoreException {
		Set<IOpenShiftMarker> oldValue = getPresentMarkers();
		if (markers != presentMarkers) {
			presentMarkers.clear();
			presentMarkers.addAll(markers);
		}
		firePropertyChange(PROPERTY_PRESENT_MARKERS, oldValue, presentMarkers);
	}
	
	public void setSelectedMarker(IOpenShiftMarker marker) {
		firePropertyChange(
				PROPERTY_SELECTED_MARKER, this.selectedMarker, this.selectedMarker = marker);
	}
	
	public IOpenShiftMarker getSelectedMarker() {
		return selectedMarker;
	}

	public void addToProject(IOpenShiftMarker marker, IProgressMonitor monitor) throws CoreException {
		if (marker == null) {
			return;
		}
		marker.addTo(project, monitor);
	}

	public void removeFromProject(IOpenShiftMarker marker, IProgressMonitor monitor) throws CoreException {
		if (marker == null) {
			return;
		}
		
		marker.removeFrom(project, monitor);
	}

	public IProject getProject() {
		return project;
	}
}
