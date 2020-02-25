/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.io.IOException;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

/**
 * @author Red Hat Developer
 */
public class WatchHandler extends ComponentHandler {

	@Override
	public Object execute(ComponentElement component, Shell shell) throws ExecutionException {
		try {
			component.getRoot().getOdo().watch(component.getParent().getParent().getWrapped().getMetadata().getName(), component.getParent().getWrapped().getName(), component.getWrapped().getPath(), component.getWrapped().getName());
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
		return null;
	}
}
