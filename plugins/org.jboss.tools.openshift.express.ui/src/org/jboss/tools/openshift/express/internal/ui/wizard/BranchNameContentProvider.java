/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;

/**
 * @author André Dietisheim
 */
public class BranchNameContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (!(inputElement instanceof Repository)) {
			return new Object[] {};
		}
		
		return getBranches((Repository) inputElement);
	}

	private Object[] getBranches(Repository repository) {

		Set<String> refNames = repository.getAllRefs().keySet();
		return refNames.toArray(new String[refNames.size()]);
	}

}
