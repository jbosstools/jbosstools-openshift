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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.ICompletable;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsWizardFragment extends WizardHandleAwareFragment implements ICompletable {
	static final String IS_LOADING_SERVICES = "isLoadingServices";

	private PropertyChangeListener connectionChangeListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final IWizardContainer wizardContainer = getWizardContainer();
			if(serverSettingsWizardPage == null 
					|| serverSettingsWizardPage.getModel() == null 
					|| wizardContainer == null) {
				//nothing to update;
				return;
			}

			if(ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION.equals(evt.getPropertyName())) {
				if(evt.getNewValue() == null || evt.getNewValue() instanceof Connection)  {
					Connection newConnection = (Connection)evt.getNewValue();
					if(newConnection != serverSettingsWizardPage.getModel().getConnection() && wizardContainer != null) {
						serverSettingsWizardPage.needsLoadingResources = true;
						serverSettingsWizardPage.getModel().setConnection(newConnection);
						serverSettingsWizardPage.getModel().setServiceItems(new ArrayList<>());
						serverSettingsWizardPage.setComplete(false);
						wizardContainer.updateButtons();
					}
				} else {
					//do nothing
				}
			} else if(ConnectionWizardPageModel.PROPERTY_CONNECTED_STATUS.equals(evt.getPropertyName())) {
				serverSettingsWizardPage.needsLoadingResources = true;
				serverSettingsWizardPage.getModel().setServiceItems(new ArrayList<>());
				serverSettingsWizardPage.setComplete(false);
				wizardContainer.updateButtons();
			}
		}
	};

	private ServerSettingsWizardPageWrapper serverSettingsWizardPage;

	/**
	 * Constructor.
	 */
	public ServerSettingsWizardFragment() {
		// no finishing wizard before input provided in this page
		setComplete(false);
	}

	/**
	 * @return the connection change listener to update the widgets if the
	 *         connection selection changed in the
	 *         {@link ConnectionWizardFragment}.
	 */
	public PropertyChangeListener getConnectionChangeListener() {
		return connectionChangeListener;
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		if(serverSettingsWizardPage != null) {
			serverSettingsWizardPage.updateServer();
			serverSettingsWizardPage.unhook();
		}
		super.performFinish(monitor); //only removes handle, it should be done after successful update only.
	}

	@Override
	public void performCancel(IProgressMonitor monitor) throws CoreException {
		if(serverSettingsWizardPage != null) {
			serverSettingsWizardPage.unhook();
		}
		super.performCancel(monitor);
	}

	/** 
	 * Exposing the protected method {@link WizardFragment#setComplete} in the API by the {@link ICompletable} interface
	 */
	@Override
	public void setComplete(boolean complete) {
		super.setComplete(complete);
	}

	@Override
	public boolean isComplete() {
		return this.serverSettingsWizardPage != null 
				&& !this.serverSettingsWizardPage.isLoadingResources() 
				&& !this.serverSettingsWizardPage.isNeedsLoadingResources() 
				&& this.serverSettingsWizardPage.getModel().getService() != null
				&& serverSettingsWizardPage.isPageComplete();
	}
	
	@Override
	public Composite createComposite(final Composite parent, final IWizardHandle handle) {
		setHandle(handle);
		this.serverSettingsWizardPage = createServerSettingsWizardPage(parent, handle);
		updateWizardHandle(handle, this.serverSettingsWizardPage);
		WizardFragmentUtils.getWizardDialog(handle).addPageChangingListener(onPageChanging());
		return (Composite) this.serverSettingsWizardPage.getControl();
	}
	
	private ServerSettingsWizardPageWrapper createServerSettingsWizardPage(final Composite parent, final IWizardHandle handle) {
		final ServerSettingsWizardPageWrapper serverSettingsWizardPage = new ServerSettingsWizardPageWrapper(handle, getTaskModel());
		serverSettingsWizardPage.getModel().addPropertyChangeListener(connectionChangeListener);
		serverSettingsWizardPage.createControl(parent);
		return serverSettingsWizardPage;
	}

	private void updateWizardHandle(final IWizardHandle handle, final ServerSettingsWizardPageWrapper serverSettingsWizardPage) {
		handle.setTitle(serverSettingsWizardPage.getTitle());
		handle.setDescription(serverSettingsWizardPage.getDescription());
		handle.setImageDescriptor(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM);
	}

	private IPageChangingListener onPageChanging() {
		return new IPageChangingListener() {
			@Override
			public void handlePageChanging(PageChangingEvent event) {
				if(serverSettingsWizardPage != null) {
					serverSettingsWizardPage.reloadServices();
				}
			}
		};
	}

	protected ModifyListener onFilterTextModified(final TreeViewer applicationTemplatesViewer) {
		return new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				applicationTemplatesViewer.refresh();
				applicationTemplatesViewer.expandAll();
			}
		};
	}

	
	class ServerSettingsWizardPageWrapper extends ServerSettingsWizardPage {
		private IWizardHandle wizardHandle;

		private ServerSettingsWizardPageWrapper(final IWizardHandle wizardHandle, final TaskModel taskModel) {
			super(((IWizardPage) wizardHandle).getWizard(), 
					OpenShiftServerTaskModelAccessor.getServer(taskModel), 
					OpenShiftServerTaskModelAccessor.getConnection(taskModel),
					UIUtils.getFirstSelectedWorkbenchProject());
			this.wizardHandle = wizardHandle;
		}

		boolean isWizardDisposed() {
			return getContainer() == null
					|| getContainer().getShell() == null
					|| getContainer().getShell().isDisposed();
		}
		
		@Override
		public void setPageComplete(boolean complete) {
			super.setPageComplete(complete);
			if(!isWizardDisposed()) {
				wizardHandle.update();
			}
		}

		@Override
		public void setErrorMessage(String newMessage) {
			if(!isWizardDisposed()) {
				((WizardPage) wizardHandle).setErrorMessage(newMessage);
			}
		}

		@Override
		public void setMessage(String newMessage, int newType) {
			if(!isWizardDisposed()) {
				wizardHandle.setMessage(newMessage, newType);
			}
		}

		public void onPageWillGetDeactivated(Direction direction, PageChangingEvent event) {
			onPageWillGetDeactivated(direction, event, null);
		}

		void reloadServices() {
			final IWizardContainer container = getContainer();
			if(!needsLoadingResources || container == null) {
				return;
			}

			try {
				this.isLoadingResources = true;
				getTaskModel().putObject(IS_LOADING_SERVICES, isLoadingResources);
				container.updateButtons();
				WizardUtils.runInWizard(new Job("Loading services...") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						//only reload services.
						if(!isWizardDisposed()) {
							ServerSettingsWizardPageWrapper.this.model.loadResources();
							ServerSettingsWizardPageWrapper.this.needsLoadingResources = false;
						}
						return Status.OK_STATUS;
					}
				}, container);
			} catch (InvocationTargetException | InterruptedException e) {
				// swallow intentionally
			} finally {
				this.needsLoadingResources = false;
				this.isLoadingResources = false;
				getTaskModel().putObject(IS_LOADING_SERVICES, isLoadingResources);
				if(!isWizardDisposed()) {
					container.updateButtons();
				}
			}
		}
	
		void unhook() {
			uiHook = null;
			if(model != null) {
				model.dispose();
			}
			model = null;
		}

	}

}
