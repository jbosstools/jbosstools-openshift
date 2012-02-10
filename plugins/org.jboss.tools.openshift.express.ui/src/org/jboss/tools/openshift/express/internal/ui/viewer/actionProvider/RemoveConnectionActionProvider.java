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
package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.eclipse.jface.viewers.ITreeSelection;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractAction;

import com.openshift.express.client.IUser;

/**
 * @author Rob Stryker
 */
public class RemoveConnectionActionProvider extends AbstractActionProvider {

	
	
	public RemoveConnectionActionProvider() {
		super(new DeleteConnectionAction(), "group.edition");
	}
	

	public static class DeleteConnectionAction extends AbstractAction {

		public DeleteConnectionAction() {
			super("Delete Connection");
		}
		
		@Override
		public void run() {
			final ITreeSelection treeSelection = (ITreeSelection)selection;
			if (selection != null && selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IUser) {
				final IUser user = (IUser) treeSelection.getFirstElement();
				UserModel.getDefault().removeUser(user);
			}
		}

		
	}

}
