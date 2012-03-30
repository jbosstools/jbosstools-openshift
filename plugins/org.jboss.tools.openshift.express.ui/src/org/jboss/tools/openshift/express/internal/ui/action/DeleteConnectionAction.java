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
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

/**
 * @author Xavier Coulon
 */
public class DeleteConnectionAction extends AbstractAction {

	public DeleteConnectionAction() {
		super(OpenShiftExpressUIMessages.DELETE_CONNECTION_ACTION, true);
	}

	@Override
	public void run() {
		if (selection instanceof ITreeSelection
				&& ((ITreeSelection) selection).getFirstElement() instanceof UserDelegate) {
			final ITreeSelection treeSelection = (ITreeSelection) selection;
			final UserDelegate user = (UserDelegate) treeSelection.getFirstElement();
			UserModel.getDefault().removeUser(user);
		}
	}

}