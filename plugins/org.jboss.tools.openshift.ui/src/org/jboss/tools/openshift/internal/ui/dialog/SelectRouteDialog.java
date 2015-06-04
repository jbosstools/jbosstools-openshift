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
package org.jboss.tools.openshift.internal.ui.dialog;

import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;

import com.openshift.restclient.model.route.IRoute;

/**
 * @author André Dietisheim
 */
public class SelectRouteDialog extends ElementListSelectionDialog {

	public SelectRouteDialog(List<IRoute> routes, Shell shell) {
		super(shell, new RouteLabelProvider());
		setTitle("Select Route");
		setMessage("Select the route to open in a browser.");
		setMultipleSelection(false);
		setAllowDuplicates(false);
		setElements(routes.toArray());
	}

	public IRoute getSelectedRoute() {
		Object[] results = getResult();
		if (results == null 
				|| results.length < 1) {
			return null;
		} else {
			return (IRoute) results[0];
		}
	}
	
	private static class RouteLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return OpenShiftCommonImages.FILE;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof IRoute)) {
				return null;
			}

			IRoute route = (IRoute) element;
			return new StringBuilder()
						.append(route.getName())
						.append(" (")
						.append(route.getHost())
						.append(route.getPath())
						.append(")")
						.toString();
		}

	}

}
