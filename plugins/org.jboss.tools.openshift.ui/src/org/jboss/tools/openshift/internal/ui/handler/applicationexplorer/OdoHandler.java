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

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

/**
 * @author Red Hat Developers
 *
 */
public abstract class OdoHandler extends AbstractHandler {
	
	private ISelection selection;
	
	public abstract void actionPerformed(Odo odo) throws IOException;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		selection = HandlerUtil.getCurrentSelection(event);
		try {
			actionPerformed(getOdo());
		} catch (IOException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		return Status.OK_STATUS;
	}
	
	protected ComponentElement getComponent() {
		return UIUtils.getFirstElement(selection, ComponentElement.class);
	}
	
	protected ApplicationExplorerUIModel getCluster() {
		return UIUtils.getFirstElement(selection, ApplicationExplorerUIModel.class);
	}

	protected Odo getOdo() throws ExecutionException {
		if (getCluster() != null) {
			return getCluster().getOdo();
		}
		else if (getComponent() != null) {
			return getComponent().getRoot().getOdo();
		}
		throw new ExecutionException("No component nor cluster selected");
	}
}
