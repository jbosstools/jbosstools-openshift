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
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.webhooks.WebHooksDialog;

import com.openshift.restclient.model.IBuildConfig;

/**
 * Handler for displaying web hooks of an {@link IBuildConfig}.
 * 
 * @author Fred Bricon
 */
public class ShowWebHooksHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = UIUtils.getCurrentSelection(event);
		IBuildConfig buildConfig = UIUtils.getFirstElement(selection, IBuildConfig.class);
		if(buildConfig == null) {
			return Status.OK_STATUS;
		}
		WebHooksDialog dialog = new WebHooksDialog(HandlerUtil.getActiveShell(event), buildConfig);
		dialog.open();
		return Status.OK_STATUS;
	}


}
