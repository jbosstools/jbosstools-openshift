/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.express.internal.ui.utils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jboss.tools.common.databinding.IObservablePojo;
import org.jboss.tools.common.databinding.ObservablePojo;

public class PojoEventBridge implements PropertyChangeListener {

	private IObservablePojo destination;
	private String destinationProperty;

	public PojoEventBridge() {
	}

	public PojoEventBridge(String sourceProperty, ObservablePojo source, String destinationProperty, ObservablePojo destination) {
		listenTo(sourceProperty, source);
		forwardTo(destinationProperty, destination);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (destination == null 
				|| destinationProperty == null) {
			return;
		}
		
		destination.firePropertyChange(destinationProperty, event.getOldValue(), event.getNewValue());
	}
	
	public PojoEventBridge listenTo(String sourceProperty, IObservablePojo source) {
		source.addPropertyChangeListener(sourceProperty, this);
		return this;
	}
		
	public PojoEventBridge forwardTo(String destinationProperty, ObservablePojo destination) {
		this.destinationProperty = destinationProperty;
		this.destination = destination;
		return this;
	}
}
