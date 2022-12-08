/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
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

/**
 * @author Red Hat Developers
 *
 */
public class NamespaceElement extends AbstractOpenshiftUIElement<String, ApplicationExplorerUIModel, ApplicationExplorerUIModel> {
	
	public NamespaceElement() {
		super(null, null);
	}

	/**
	 * @param project
	 * @param parentElement
	 */
	public NamespaceElement(String project, ApplicationExplorerUIModel parentElement) {
		super(parentElement, project);
	}

}
