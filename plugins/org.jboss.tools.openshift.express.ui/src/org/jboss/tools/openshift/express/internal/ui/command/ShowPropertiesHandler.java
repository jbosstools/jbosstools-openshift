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
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class ShowPropertiesHandler extends AbstractHandler {

	private static final String PROPERTIES_VIEW_ID = "org.eclipse.ui.views.PropertySheet";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(PROPERTIES_VIEW_ID);
		} catch (PartInitException e) {
			Logger.error("Failed to show properties view", e);
		}
		return Status.OK_STATUS;
	}

}
