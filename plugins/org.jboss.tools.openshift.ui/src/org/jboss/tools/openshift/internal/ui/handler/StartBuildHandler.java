/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.job.StartBuildJob;

import com.openshift.restclient.capability.resources.IBuildTriggerable;
import com.openshift.restclient.model.IResource;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class StartBuildHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = UIUtils.getCurrentSelection(event);
		IResource buildAble = UIUtils.getFirstElement(selection, IResource.class);
		if (buildAble == null || buildAble.getCapability(IBuildTriggerable.class) == null) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Trigger Build",
					"A build or build config must be selected in order to trigger a build.");
			return null;
		}
		new StartBuildJob(buildAble).schedule();
		return null;
	}
}