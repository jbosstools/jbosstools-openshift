/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.comparators;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import com.openshift.restclient.model.IProject;

public class ProjectViewerComparator extends ViewerComparator {
	
	private static final int LAST = 1;
	private static final String DEFAULT_PROJECT = "default";
	private static final String OPENSHIFT_PROJECT = "openshift";
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if(e1 == null || e2 == null || !(e1 instanceof IProject) || !(e2 instanceof IProject)) {
			return LAST;
		}
		IProject projectOne = (IProject) e1;
		IProject projectTwo = (IProject) e2;
		
		final String name1 = projectOne.getName();
		final String name2 = projectTwo.getName();
		
		if(DEFAULT_PROJECT.equals(name1)) {
			return -1;
		}
		if(DEFAULT_PROJECT.equals(name2)) {
			return 1;
		}
		if(name1.startsWith(OPENSHIFT_PROJECT) && !name2.startsWith(OPENSHIFT_PROJECT)) {
			return -1;
		}
		if(!name1.startsWith(OPENSHIFT_PROJECT) && name2.startsWith(OPENSHIFT_PROJECT)) {
			return 1;
		}
		
		return name1.compareTo(name2);
	}

	
}
