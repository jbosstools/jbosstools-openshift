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
package org.jboss.tools.openshift.internal.ui.models.applicationexplorer;

import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIElement;
import io.fabric8.openshift.api.model.Project;

/**
 * @author Jeff MAURY
 *
 */
public class ProjectElement extends AbstractOpenshiftUIElement<Project, ApplicationExplorerUIModel, ApplicationExplorerUIModel> {
	
	public ProjectElement() {
		super(null, null);
	}

	/**
	 * @param project
	 * @param parentElement
	 */
	public ProjectElement(Project project, ApplicationExplorerUIModel parentElement) {
		super(parentElement, project);
	}

}
