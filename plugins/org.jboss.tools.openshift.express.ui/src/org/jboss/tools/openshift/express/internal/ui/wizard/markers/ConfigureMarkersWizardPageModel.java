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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.core.marker.OpenShiftMarkers;
import org.jboss.tools.openshift.express.internal.core.util.DiffUtils;

/**
 * @author André Dietisheim
 */
public class ConfigureMarkersWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_AVAILABLE_MARKERS = "availableMarkers";
	public static final String PROPERTY_CHECKED_MARKERS = "checkedMarkers";
	public static final String PROPERTY_SELECTED_MARKER = "selectedMarker";
	
	private List<IOpenShiftMarker> availableMarkers;
	private Set<IOpenShiftMarker> presentMarkers = new HashSet<>();
	private Set<IOpenShiftMarker> checkedMarkers = new HashSet<>();
	private IOpenShiftMarker selectedMarker;
	private IProject project;

	public ConfigureMarkersWizardPageModel(IProject project) {
		this.project = project;
	}
	
	public void loadMarkers() throws CoreException {
		OpenShiftMarkers markers = new OpenShiftMarkers(project);
		setAvailableMarkers(markers.getAll());
		this.presentMarkers = toSet(markers.getPresent());
		setCheckedMarkers(presentMarkers);
	}
	
	private Set<IOpenShiftMarker> toSet(List<IOpenShiftMarker> markers) {
		Set<IOpenShiftMarker> markersSet = new HashSet<>();
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

	public Set<IOpenShiftMarker> getCheckedMarkers() {
		return checkedMarkers;
	}

	public void setCheckedMarkers(Set<IOpenShiftMarker> markers) {
		Set<IOpenShiftMarker> oldValue = getCheckedMarkers();
		if (markers != checkedMarkers) {
			checkedMarkers.clear();
			checkedMarkers.addAll(markers);
		}
		firePropertyChange(PROPERTY_CHECKED_MARKERS, oldValue, checkedMarkers);
	}
	
	public void setSelectedMarker(IOpenShiftMarker marker) {
		firePropertyChange(
				PROPERTY_SELECTED_MARKER, this.selectedMarker, this.selectedMarker = marker);
	}
	
	public IOpenShiftMarker getSelectedMarker() {
		return selectedMarker;
	}

	/**
	 * Returns the markers that the user removed.
	 * 
	 * @return the markers that the user removed
	 */
	public Collection<IOpenShiftMarker> getRemovedMarkers() {
		return DiffUtils.getRemovals(presentMarkers, checkedMarkers);
	}

	/**
	 * Returns the markers that the user has added.
	 * 
	 * @return the markers that the user added
	 */
	public Collection<IOpenShiftMarker> getAddedMarkers() {
		return DiffUtils.getAdditions(presentMarkers, checkedMarkers);
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
