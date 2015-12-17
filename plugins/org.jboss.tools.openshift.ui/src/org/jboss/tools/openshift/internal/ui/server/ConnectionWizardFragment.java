/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage.Direction;

/**
 * A wizard fragment that provides the connection wizard page as wst wizard
 * fragment
 * 
 * @author Andre Dietisheim
 * 
 * @see WizardFragment
 */
public class ConnectionWizardFragment extends WizardFragment {

	private WrappedConnectionWizardPage connectionPage;

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public boolean isComplete() {
		return connectionPage != null 
				&& connectionPage.isPageComplete();
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		this.connectionPage = createConnectionWizardPage(parent, handle);
		updateWizardHandle(handle, connectionPage);
		getContainer(getPage(handle)).addPageChangingListener(onPageChanging(handle));
		return (Composite) connectionPage.getControl();
	}

	private IPageChangingListener onPageChanging(IWizardHandle wizardHandle) {
		return new IPageChangingListener() {

			@Override
			public void handlePageChanging(PageChangingEvent event) {
				if (event.getCurrentPage() == getPage(wizardHandle)){
					if (event.getTargetPage() == null
							|| event.getTargetPage().equals(getPage(wizardHandle).getNextPage())) {
						connectionPage.onPageWillGetDeactivated(Direction.FORWARDS, event);							
					} else {
						connectionPage.onPageWillGetDeactivated(Direction.BACKWARDS, event);
					}
					
					IConnection connection = connectionPage.getConnection();
					if (connection instanceof Connection) {
						OpenShiftServerTaskModelAccessor.set((Connection) connection, getTaskModel());
					}
				}
			}
		};
	}

	private WrappedConnectionWizardPage createConnectionWizardPage(Composite parent, IWizardHandle handle) {
		WrappedConnectionWizardPage connectionPage = new WrappedConnectionWizardPage(handle);
		connectionPage.createControl(parent);
		return connectionPage;
	}

	private void updateWizardHandle(IWizardHandle handle, ConnectionWizardPage connectionPage) {
		handle.setTitle(connectionPage.getTitle());
		handle.setDescription(connectionPage.getDescription());
		handle.setImageDescriptor(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM);
	}

	private IWizardPage getPage(IWizardHandle wizardHandle) {
		return (IWizardPage) wizardHandle;
	}
	
	private WizardDialog getContainer(IWizardPage wizardPage) {
		return (WizardDialog) wizardPage.getWizard().getContainer();
	}

	private class WrappedConnectionWizardPage extends ConnectionWizardPage {

		private IWizardHandle wizardHandle;

		private WrappedConnectionWizardPage(IWizardHandle wizardHandle) {
			super(((IWizardPage) wizardHandle).getWizard(), new ConnectionWizardModel(Connection.class), Connection.class);
			this.wizardHandle = wizardHandle;
		}

		@Override
		public void setPageComplete(boolean complete) {
			super.setPageComplete(complete);
			wizardHandle.update();
		}

		@Override
		public void setErrorMessage(String newMessage) {
			((WizardPage) wizardHandle).setErrorMessage(newMessage);
		}

		@Override
		public void setMessage(String newMessage, int newType) {
			wizardHandle.setMessage(newMessage, newType);
		}

		public void onPageWillGetDeactivated(Direction direction, PageChangingEvent event) {
			onPageWillGetDeactivated(direction, event, null);
		}
	}
}
