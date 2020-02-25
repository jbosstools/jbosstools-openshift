/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.List;

import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class LinkModel<T> extends ComponentModel {
	public static final String PROPERTY_TARGET = "target";
	
	private T target;
	private List<T> targets;
	
	public LinkModel(Odo odo, String projectName, String applicationName, String componentName, List<T> targets) {
		super(odo, projectName, applicationName, componentName);
		setTargets(targets);
	}
	
	/**
	 * @return the target
	 */
	public T getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(T target) {
		firePropertyChange(PROPERTY_TARGET, this.target, this.target = target);
	}

	/**
	 * @return the targets
	 */
	public List<T> getTargets() {
		return targets;
	}

	/**
	 * @param targets the targets to set
	 */
	public void setTargets(List<T> targets) {
		this.targets = targets;
		if (!targets.isEmpty()) {
			setTarget(targets.get(0));
		}
	}
}
