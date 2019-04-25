/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;

public class ChooseOpenshiftConnectionFragment extends WizardFragment {
	private IWizardHandle handle;
	private ChooseOpenshiftConnectionComposite chooseConnectionComposite;
	private PropertyChangeListener externalConnectionListener;
	private IConnection selectedConnection;

	protected void initWizardHandle() {
		// make modifications to parent
		handle.setTitle("Choose an OpenShift Connection");
		handle.setDescription("Please choose an openshift connection to use for this server adapter.");
		handle.setImageDescriptor(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM);
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public boolean isComplete() {
		return chooseConnectionComposite != null && !chooseConnectionComposite.isDisposed() && super.isComplete();
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		// do nothing
	}

	@Override
	public Composite createComposite(Composite parent, final IWizardHandle handle) {
		this.handle = handle;
		initWizardHandle();

		chooseConnectionComposite = new ChooseOpenshiftConnectionComposite(parent);
		chooseConnectionComposite.setConnectionChangeListener(this::selectedConnectionChanged);
		getContainer(getPage(handle)).addPageChangingListener(onPageChanging(handle));
		selectedConnectionChanged(chooseConnectionComposite.getConnection());
		return chooseConnectionComposite;
	}

	private void selectedConnectionChanged(IConnection newVal) {
		IConnection oldCon = selectedConnection;
		selectedConnection = newVal;
		setComplete(selectedConnection != null);
		handle.update();
		if (externalConnectionListener != null) {
			// re-fire event to external listener
			PropertyChangeEvent event = new PropertyChangeEvent(this,
					ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION, oldCon, selectedConnection);
			externalConnectionListener.propertyChange(event);
		}
	}

	private IWizardPage getPage(IWizardHandle wizardHandle) {
		return (IWizardPage) wizardHandle;
	}

	private WizardDialog getContainer(IWizardPage wizardPage) {
		return (WizardDialog) wizardPage.getWizard().getContainer();
	}

	public void addConnectionChangeListener(PropertyChangeListener connectionChangeListener) {
		this.externalConnectionListener = connectionChangeListener;
	}

	private IPageChangingListener onPageChanging(IWizardHandle wizardHandle) {
		return (PageChangingEvent event) -> {
				if (event.getCurrentPage() == getPage(wizardHandle)) {
					if (event.getTargetPage() == null
							|| event.getTargetPage().equals(getPage(wizardHandle).getNextPage())) {
						String blockedMsg = null;
						try {
							blockedMsg = runConnectionJob();
						} catch (Exception e) {
							blockedMsg = "Error while checking if we can connect to OpenShift Connection: "
									+ e.getMessage();
						}
						event.doit = (blockedMsg == null);
						handle.setMessage(blockedMsg,
								(blockedMsg == null ? IMessageProvider.NONE : IMessageProvider.ERROR));
					}
					if (selectedConnection instanceof Connection) {
						OpenShiftServerTaskModelAccessor.set((Connection) selectedConnection, getTaskModel());
					}
				}
		};
	}

	private String runConnectionJob() throws Exception {
		String blockedMsg = null;
		if (selectedConnection != null && selectedConnection.canConnect()) {
			Job connectJob = chooseConnectionComposite.getConnectJob();
			WizardUtils.runInWizard(connectJob, new DelegatingProgressMonitor(), getContainer(((WizardPage) handle)));
			boolean connected = JobUtils.isOk(connectJob.getResult());
			if (!connected) {
				blockedMsg = connectJob.getResult().getMessage();
			}
			return blockedMsg;
		}
		return blockedMsg;
	}
}
