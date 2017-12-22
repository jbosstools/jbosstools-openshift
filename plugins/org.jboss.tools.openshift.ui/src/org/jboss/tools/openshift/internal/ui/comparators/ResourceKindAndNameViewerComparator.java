/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.comparators;

import java.util.Comparator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.jboss.tools.openshift.internal.core.util.ResourceKindAndNameComparator;

import com.openshift.restclient.model.IResource;

public class ResourceKindAndNameViewerComparator extends ViewerComparator {

	private final Comparator<IResource> comparator = new ResourceKindAndNameComparator();

	@Override
	public int compare(Viewer viewer, Object resource1, Object resource2) {
		return comparator.compare((IResource) resource1, (IResource) resource2);
	}

}
