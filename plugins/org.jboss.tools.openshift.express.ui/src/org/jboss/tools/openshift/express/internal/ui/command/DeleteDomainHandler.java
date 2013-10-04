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
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.ui.job.DestroyDomainJob;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.DestroyDomainDialog;

import com.openshift.client.IDomain;

/**
 * @author Andre Dietisheim
 */
public class DeleteDomainHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		final IDomain domain = UIUtils.getFirstElement(selection, IDomain.class);
		if (domain == null) {
			return Status.OK_STATUS;
		}
		DestroyDomainDialog dialog = new DestroyDomainDialog(domain, HandlerUtil.getActiveShell(event));
		dialog.open();
		if (dialog.isCancel()) {
			return Status.OK_STATUS;
		}

		new DestroyDomainJob(domain, dialog.isForceDelete()).schedule();
		
		return Status.OK_STATUS;
	}
}
