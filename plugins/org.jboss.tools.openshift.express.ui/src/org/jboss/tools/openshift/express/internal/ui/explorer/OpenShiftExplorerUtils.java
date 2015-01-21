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
package org.jboss.tools.openshift.express.internal.ui.explorer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftExplorerUtils {

	public static ExpressConnection getConnectionFor(ISelection selection) {
		if (selection == null
				|| !ITreeSelection.class.isAssignableFrom(selection.getClass())) {
			return null;
		}
		
		return getConnectionFor((ITreeSelection) selection);
	}

	/**
	 * Returns the connection for the given selected explorer item. Only works
	 * if there's only 1 item selected and if the root elements in the explorer
	 * are connections (since it relies on the first path segment to be a
	 * connection).
	 * 
	 * @param selection
	 * @return
	 */
	public static ExpressConnection getConnectionFor(ITreeSelection selection) {
		if (selection == null
				|| selection.size() < 1) {
			return null;
		}

		Object selectedElement = selection.getFirstElement();
		TreePath[] paths = selection.getPathsFor(selectedElement);
		if (paths == null
				|| paths.length == 0) {
			return null;
		}
		Object firstPathSegment = paths[0].getFirstSegment();
		Assert.isLegal(firstPathSegment instanceof ExpressConnection);
		return (ExpressConnection) firstPathSegment;
	}
}
