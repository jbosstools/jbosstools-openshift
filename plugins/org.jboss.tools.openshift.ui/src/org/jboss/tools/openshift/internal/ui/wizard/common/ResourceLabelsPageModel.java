/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.databinding.ObservablePojo;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class ResourceLabelsPageModel extends ObservablePojo implements IResourceLabelsPageModel {

	private Collection<String> readonlyLabels = Arrays.asList("template");
	private List<Label> labels = new ArrayList<>();
	private Label selectedLabel;

	@Override
	public List<Label> getLabels() {
		return this.labels;
	}

	@Override
	public void setLabels(List<Label> labels) {
		firePropertyChange(PROPERTY_LABELS, this.labels, this.labels = labels);
	}

	@Override
	public Collection<String> getReadOnlyLabels() {
		return readonlyLabels;
	}

	@Override
	public void setSelectedLabel(Label label) {
		firePropertyChange(PROPERTY_SELECTED_LABEL, this.selectedLabel, this.selectedLabel = label);
	}

	@Override
	public Label getSelectedLabel() {
		return this.selectedLabel;
	}

	@Override
	public void removeLabel(Label label) {
		List<Label> old = new ArrayList<>(this.labels);
		final int index = labels.indexOf(label);
		if(index > -1) {
			this.labels.remove(label);
			fireIndexedPropertyChange(PROPERTY_LABELS, index, old, Collections.unmodifiableList(labels));
		}
	}

	@Override
	public void updateLabel(Label label, String key, String value) {
		List<Label> old = new ArrayList<>(this.labels);
		final int index = labels.indexOf(label);
		if(index > -1) {
			labels.set(index, new Label(key, value));
			fireIndexedPropertyChange(PROPERTY_LABELS, index, old, Collections.unmodifiableList(labels));
		}
	}

	@Override
	public void addLabel(String key, String value) {
		List<Label> old = new ArrayList<>(this.labels);
		this.labels.add(new Label(key, value));
		fireIndexedPropertyChange(PROPERTY_LABELS, this.labels.size(), old, Collections.unmodifiableList(labels));
	}

}
