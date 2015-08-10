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
package org.jboss.tools.openshift.common.ui.wizard;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPage;
import org.jboss.tools.openshift.internal.common.ui.utils.ExtensionUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareWizard;

/**
 * A wizard to create a new OpenShift application.
 * 
 * @author Andre Dietisheim
 * 
 */
public abstract class AbstractApplicationWorkbenchWizard extends Wizard implements IWorkbenchWizard {
	
	protected class DelegatingConnectionWizardPage extends ConnectionWizardPage {
		
		private IObservableValue validationStatus;

		private DelegatingConnectionWizardPage(IWizard wizard, IConnectionAware<IConnection> wizardModel) {
			super(wizard, wizardModel);
		}

		@Override
		public boolean isPageComplete() {
			return false;
		}

		@Override
		public boolean canFlipToNextPage() {
			return hasWizard(getModel().getConnectionFactory())
					&& isValid();
		}

		private boolean isValid() {
			IStatus status = (IStatus) getValidationStatus().getValue();
			return status == null || status.isOK();
		}

		private IObservableValue getValidationStatus() {
			if (validationStatus == null) {
				this.validationStatus = 
						new AggregateValidationStatus(getDatabindingContext(), AggregateValidationStatus.MAX_SEVERITY);
			}
			return validationStatus;
		}

		@Override
		public IWizardPage getNextPage() {
			if (!connect()) {
				return null;
			}
			IConnection connection = getConnection();
			if (connection == null) {
				return null;
			}
			IConnectionAwareWizard<IConnection> wizard = getWizard(connection);
			if (wizard == null) {
				return null;
			}
			wizard.setConnection(connection);
			return wizard.getStartingPage();
		}

		private IConnectionAwareWizard<IConnection> getWizard(IConnection connection) {
			if (connection == null) {
				return null;
			}

			IConnectionAwareWizard<IConnection> wizard = AbstractApplicationWorkbenchWizard.this.getWizard(connection.getClass());
			if (wizard == null) {
				return null;
			}

			if (wizard.getPageCount() == 0) {
				// initialize wizard
				if (wizard instanceof IWorkbenchWizard) {
					// init for package explorer/Configure->New/Import OpenShift application
					((IWorkbenchWizard) wizard).init(workbench, selection);
				}
				wizard.addPages();
			}
			return wizard;
		}

		private boolean hasWizard(IConnectionFactory factory) {
			if (factory == null) {
				return false;
			}
			String host = getModel().getHost();
			if (StringUtils.isEmpty(host)){
			    return false;
			}
			IConnection connection = factory.create(host);
			if (connection == null) {
				return false;
			}
			IWizard wizard = AbstractApplicationWorkbenchWizard.this.getWizard(connection.getClass());
			if (wizard == null) {
				setErrorMessage(NLS.bind("No wizard for {0} connections present.", getModel().getConnectionFactory().getName()));
			}
			return wizard != null;
		}
	}

	private static final String ATTRIBUTE_CLASS = "class";
	private static final String ATTRIBUTE_CONNECTION = "connection";

	private Map<Class<IConnection>, IConnectionAwareWizard<IConnection>> wizardsByConnection;
	private IWorkbench workbench;
	private IStructuredSelection selection;

	protected AbstractApplicationWorkbenchWizard(String title) {
		setWindowTitle(title);
		setNeedsProgressMonitor(true);
		setForcePreviousAndNextButtons(true);
	}

	protected IConnectionAwareWizard<IConnection> getWizard(Class<? extends IConnection> connectionClass) {
		return getWizards().get(connectionClass);
	}

	private Map<Class<IConnection>, IConnectionAwareWizard<IConnection>> getWizards() {
		if (wizardsByConnection == null) {
			this.wizardsByConnection = createWizards(getWizardsExtensionId());
		}
		
		return wizardsByConnection;
	}
	
	protected Map<Class<IConnection>, IConnectionAwareWizard<IConnection>> createWizards(String extensionId) {
		HashMap<Class<IConnection>, IConnectionAwareWizard<IConnection>> wizardsByConnection = new HashMap<Class<IConnection>, IConnectionAwareWizard<IConnection>>();
 		for (IConfigurationElement configuration : ExtensionUtils.getExtensionConfigurations(extensionId)) {
			createWizard(wizardsByConnection, configuration);
		}
		return wizardsByConnection;
	}

	private void createWizard(HashMap<Class<IConnection>, IConnectionAwareWizard<IConnection>> wizardsByConnection, IConfigurationElement configuration) {
		try {
			IConnectionAwareWizard<IConnection> wizard = ExtensionUtils.createExtension(ATTRIBUTE_CLASS, configuration);
			if (wizard != null) {
				Class<IConnection> connectionClass = ExtensionUtils.getClass(configuration.getAttribute(ATTRIBUTE_CONNECTION), configuration);
				if (connectionClass != null) {
					wizardsByConnection.put(connectionClass, wizard);
				}
			}
		} catch (InvalidRegistryObjectException | IllegalStateException | IllegalArgumentException | ClassNotFoundException e) {
			OpenShiftCommonUIActivator.log(
					NLS.bind("Could not create application wizard in bundle {0} for extension {1}", // $NON-NLS-1$
							ExtensionUtils.getBundleNameFor(configuration),
							configuration.getName()),
					e); 
		}
	}

	@Override
	public boolean performFinish() {
		return false;
	}

	@Override
	public void addPages() {
		addPage(new DelegatingConnectionWizardPage(this, new ConnectionWizardModel(ConnectionsRegistrySingleton.getInstance().getRecentConnection(), null)));
	}

	protected abstract String getWizardsExtensionId();

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

}
