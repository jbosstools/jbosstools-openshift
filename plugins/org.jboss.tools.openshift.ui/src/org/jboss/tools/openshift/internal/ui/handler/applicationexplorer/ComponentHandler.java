/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

/**
 * @author Red Hat Developers
 */
public abstract class ComponentHandler extends OdoJobHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement component = UIUtils.getFirstElement(selection, ComponentElement.class);
		if (component == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No component selected"); //$NON-NLS-1$
		}
		return execute(component, HandlerUtil.getActiveShell(event));
	}

	abstract Object execute(ComponentElement component, Shell shell) throws ExecutionException;
}
