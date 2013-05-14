/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import java.net.SocketTimeoutException;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.EmbeddableCartridgeDiff;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.job.CreateApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.WaitForApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils.ToStringConverter;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.LogEntryFactory;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.client.cartridge.selector.LatestVersionOf;

/**
 * A UI strategy that is able to add and remove embedded cartridges while
 * fullfilling requirements and resolving conflicts (ex. mutual exclusivity
 * etc.)
 * <p>
 * TODO: replaced this manual code by a generic dependency-tree analysis
 * mechanism as soon as OpenShift completed design of cartridge metamodel
 * 
 * @author Andre Dietisheim
 */
public class EmbedCartridgeStrategyAdapter implements ICheckStateListener {

	private static final int APP_CREATE_TIMEOUT = 2 * 60 * 1000;
	private static final int APP_WAIT_TIMEOUT = 2 * 60 * 1000;

	private IEmbedCartridgesWizardPageModel pageModel;
	private IWizardPage wizardPage;

	public EmbedCartridgeStrategyAdapter(IEmbedCartridgesWizardPageModel pageModel, IWizardPage wizardPage) {
		this.wizardPage = wizardPage;
		this.pageModel = pageModel;
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		try {
			IEmbeddableCartridge cartridge = (IEmbeddableCartridge) event.getElement();
			EmbedCartridgeStrategy embedCartridgeStrategy = getEmbedCartridgeStrategy(pageModel.getDomain());
			boolean adding = event.getChecked();
			EmbeddableCartridgeDiff diff = createEmbeddableCartridgeDiff(adding, cartridge, embedCartridgeStrategy);

			if (diff.hasChanges()) {
				int result = openAdditionalOperationsDialog(
						NLS.bind("{0} Cartridges", event.getChecked() ? "Add" : "Remove"),
						createEmbeddingOperationMessage(adding, diff));
				switch (result) {
				case 1:
					executeAdditionalOperations(cartridge, diff);
					break;
				case 0:
					dontExecuteAnyOperation(event, cartridge);
					break;
				case 2:
					// user has chosen to ignore additional requirements
				}
			} else if (!adding) {
				if(!MessageDialog.openQuestion(getShell(), 
						NLS.bind("Remove cartridge {0}", cartridge.getName()), 
								NLS.bind(
										"You are about to remove cartridge {0}.\n"
												+ "Removing a cartridge is not reversible and can cause you to loose the data you have stored in it."
												+ "\nAre you sure?", cartridge.getName()))) {
					// revert removal
					pageModel.selectEmbeddedCartridges(cartridge);
				}
			}
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
		} catch (SocketTimeoutException e) {
			OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
		}
	}

	private EmbedCartridgeStrategy getEmbedCartridgeStrategy(IDomain domain) throws SocketTimeoutException {
		IOpenShiftConnection connection = domain.getUser().getConnection();
		EmbedCartridgeStrategy embedCartridgeStrategy =
				new EmbedCartridgeStrategy(
						connection.getEmbeddableCartridges(),
						connection.getStandaloneCartridges(), 
						domain.getApplications());
		return embedCartridgeStrategy;
	}

	private EmbeddableCartridgeDiff createEmbeddableCartridgeDiff(
			boolean add, IEmbeddableCartridge cartridge, EmbedCartridgeStrategy embedCartridgeStrategy)
			throws OpenShiftException, SocketTimeoutException {
		if (add) {
			return embedCartridgeStrategy.add(cartridge, pageModel.getSelectedEmbeddableCartridges());
		} else {
			return embedCartridgeStrategy.remove(cartridge, pageModel.getSelectedEmbeddableCartridges());
		}
	}

	public int openAdditionalOperationsDialog(String title, String message) {
		MessageDialog dialog = new MessageDialog(getShell(),
				title, null, message, MessageDialog.QUESTION, new String[] { "Cancel", "Apply", "Ignore" }, 0);
		return dialog.open();
	}
	
	private String createEmbeddingOperationMessage(boolean adding, EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff) {
		StringBuilder builder = new StringBuilder();
		builder.append(NLS.bind("If you want to {0} {1}, it is suggested you:", 
				adding ? "add" : "remove",
				new EmbeddableCartridgeToStringConverter().toString(diff.getCartridge())));
		if (diff.hasApplicationAdditions()) {
			builder.append(NLS.bind("\n- Create {0}",
					StringUtils.toString(diff.getApplicationAdditions(), new CartridgeToStringConverter())));
		}
		if (diff.hasRemovals()) {
			builder.append(NLS.bind("\n- Remove {0}",
					StringUtils.toString(diff.getRemovals(), new EmbeddableCartridgeToStringConverter())))
					.append("\n\nRemoving cartridges is not reversible and may cause you to loose the data you have stored in it.");
		
		}
		if (diff.hasAdditions()) {
			builder.append(NLS.bind("\n- Add {0}",
					StringUtils.toString(diff.getAdditions(), new EmbeddableCartridgeToStringConverter())));
		}
		builder.append("\n\nDo you want to Apply or Ignore these suggestions??");
		return builder.toString();
	}

	protected void executeAdditionalOperations(IEmbeddableCartridge cartridge, EmbeddableCartridgeDiff diff)
			throws SocketTimeoutException {
		if (createApplications(diff.getApplicationAdditions())) {
			unselectEmbeddableCartridges(diff.getRemovals());
			selectEmbeddableCartridges(diff.getAdditions());
		} else {
			pageModel.unselectEmbeddedCartridges(cartridge);
		}
	}

	private void dontExecuteAnyOperation(CheckStateChangedEvent event, IEmbeddableCartridge cartridge) throws SocketTimeoutException, OpenShiftException {
		if (event.getChecked()) {
			pageModel.unselectEmbeddedCartridges(cartridge);
		} else {
			pageModel.selectEmbeddedCartridges(cartridge);
		}
	}

	private void unselectEmbeddableCartridges(List<IEmbeddableCartridge> removals) throws SocketTimeoutException,
			OpenShiftException {
		for (IEmbeddableCartridge embeddableCartridge : removals) {
			pageModel.unselectEmbeddedCartridges(embeddableCartridge);
		}
	}

	private void selectEmbeddableCartridges(List<IEmbeddableCartridge> additions) throws SocketTimeoutException,
			OpenShiftException {
		for (IEmbeddableCartridge embeddableCartridge : additions) {
			pageModel.selectEmbeddedCartridges(embeddableCartridge);
		}
	}

	private boolean createApplications(List<IStandaloneCartridge> applicationAdditions) {
		for (IStandaloneCartridge cartridge : applicationAdditions) {
			if (!LatestVersionOf.jenkins().matches(cartridge)) {
				throw new UnsupportedOperationException("only jenkins applications may currently be created.");
			}
			if (!createJenkinsApplication(cartridge)) {
				return false;
			}
		}
		return true;
	}

	private boolean createJenkinsApplication(final IStandaloneCartridge cartridge) {
		final String name = openJenkinsApplicationDialog();
		if (name == null) {
			return false;
		}
		try {
			IDomain domain = pageModel.getDomain();
			IStandaloneCartridge jenkinsCartridge = LatestVersionOf.jenkins().get(domain.getUser());
			CreateApplicationJob createJob =
					new CreateApplicationJob(name, jenkinsCartridge , ApplicationScale.NO_SCALE,
							IGearProfile.SMALL, domain);
			WizardUtils.runInWizard(
					createJob, createJob.getDelegatingProgressMonitor(), getContainer(), APP_CREATE_TIMEOUT);
			IStatus result = createJob.getResult();
			if (JobUtils.isOk(result)) {
				IApplication application = createJob.getApplication();
				openLogDialog(application, createJob.isTimeouted(result));

				AbstractDelegatingMonitorJob job = new WaitForApplicationJob(application, getShell());
				IStatus waitStatus = WizardUtils.runInWizard(
						job, job.getDelegatingProgressMonitor(), getContainer(), APP_WAIT_TIMEOUT);
				return JobUtils.isOk(waitStatus);
			}

		} catch (Exception e) {
			// ignore
			e.printStackTrace();
		}
		return false;
	}

	private String openJenkinsApplicationDialog() {
		final JenkinsApplicationDialog dialog = new JenkinsApplicationDialog(getShell());
		if (dialog.open() != Dialog.OK) {
			return null;
		}
		return dialog.getValue();
	}

	private Shell getShell() {
		return wizardPage.getControl().getShell();
	}

	private IWizardContainer getContainer() {
		return wizardPage.getWizard().getContainer();
	}

	private void openLogDialog(final IApplication application, final boolean isTimeouted) {
		wizardPage.getControl().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), 
						LogEntryFactory.create(application, isTimeouted)).open();
			}
		});
	}

	private static class JenkinsApplicationDialog extends InputDialog {

		public JenkinsApplicationDialog(Shell shell) {
			super(
					shell,
					"New Jenkins application",
					"To embed Jenkins into your application, you first have to create a separate Jenkins application. "
							+ "Please provide a name for this new Jenkins application (letters and digits only):"
					, null, new JenkinsNameValidator());
		}

		private static class JenkinsNameValidator implements IInputValidator {

			@Override
			public String isValid(String input) {
				if (StringUtils.isEmpty(input)) {
					return "You have to provide a name for the jenkins application";
				}

				if (!StringUtils.isAlphaNumeric(input)) {
					return "The name may only contain letters and digits.";
				}
				return null;
			}
		}
	}

	private static class EmbeddableCartridgeToStringConverter implements ToStringConverter<IEmbeddableCartridge> {

		@Override
		public String toString(IEmbeddableCartridge cartridge) {
			if (cartridge == null) {
				return null;
			}
			return cartridge.getName();
		}
	}

	private static class CartridgeToStringConverter implements ToStringConverter<IStandaloneCartridge> {

		@Override
		public String toString(IStandaloneCartridge cartridge) {
			if (cartridge == null) {
				return null;
			}
			return cartridge.getName();
		}
	}
}
