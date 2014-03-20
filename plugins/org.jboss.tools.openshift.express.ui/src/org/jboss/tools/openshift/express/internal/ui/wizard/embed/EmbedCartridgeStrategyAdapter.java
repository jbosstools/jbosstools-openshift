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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
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
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.ApplicationRequirement;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.EmbeddableCartridgeDiff;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.IApplicationProperties;
import org.jboss.tools.openshift.express.internal.core.util.EmbeddableCartridgeToStringConverter;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.job.CreateApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.WaitForApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog.LogEntry;
import org.jboss.tools.openshift.express.internal.ui.wizard.LogEntryFactory;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftEndpointException;
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

	private static final int APP_CREATE_TIMEOUT = 5 * 60 * 1000;
	private static final int APP_WAIT_TIMEOUT = 5 * 60 * 1000;

	private IEmbedCartridgesWizardPageModel model;
	private IWizardPage wizardPage;
	private IApplicationProperties applicationProperties;

	public EmbedCartridgeStrategyAdapter(IEmbedCartridgesWizardPageModel pageModel, IWizardPage wizardPage, IApplicationProperties provider) {
		this.model = pageModel;
		this.applicationProperties = provider;
		this.wizardPage = wizardPage;
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		IEmbeddableCartridge cartridge = (IEmbeddableCartridge) event.getElement();
		if(event.getChecked()) {
			add(cartridge);
		} else {
			remove(cartridge);
		}
	}
	
	private void add(IEmbeddableCartridge cartridge) {
		try {
			EmbedCartridgeStrategy embedCartridgeStrategy = createEmbedCartridgeStrategy(model.getDomain());
			ApplicationRequirement missingRequirement = 
					embedCartridgeStrategy.getMissingRequirement(cartridge, applicationProperties);
			if (missingRequirement != null) {
				if (!shouldAddToInappropriateApplication(missingRequirement, cartridge, applicationProperties)) {
					return;
				}
			}
			EmbeddableCartridgeDiff additionalOperations = 
					embedCartridgeStrategy.add(cartridge, model.getCheckedEmbeddableCartridges());
			if (additionalOperations.hasChanges()) {
				executeAdditionalOperations(true, cartridge, additionalOperations);
			}
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
		}
	}
	
	private void remove(IEmbeddableCartridge cartridge) {
		try {
			EmbedCartridgeStrategy embedCartridgeStrategy = createEmbedCartridgeStrategy(model.getDomain());
			EmbeddableCartridgeDiff additionalOperations = 
					embedCartridgeStrategy.remove(cartridge, model.getCheckedEmbeddableCartridges());
			if (additionalOperations.hasChanges()) {
				executeAdditionalOperations(false, cartridge, additionalOperations);
			} else if (model.isEmbedded(cartridge)) {
				executeRemove(cartridge);
			}
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
		}
	}

	private boolean shouldAddToInappropriateApplication(ApplicationRequirement requirement, IEmbeddableCartridge cartridge,
			IApplicationProperties applicationProperties) throws OpenShiftException {
		String title = NLS.bind("Inappropriate application {0}", applicationProperties.getApplicationName());
		String message = requirement.getMessage(cartridge, applicationProperties)
				+ NLS.bind("\n\nAdding may fail, are you sure that you want to add cartridge {0}?",
						cartridge.getName());
			// revert
		switch (openQuestionDialog(title, message)) {
			default:
			case 0:
				model.uncheckEmbeddedCartridge(cartridge);
				return false;
			case 1:
				return true;
		}
	}

	private void executeRemove(IEmbeddableCartridge cartridge) {
		if(!MessageDialog.openQuestion(getShell(), 
				NLS.bind("Remove cartridge {0}", cartridge.getName()), 
						NLS.bind(
								"You are about to remove cartridge {0}.\n"
										+ "Removing a cartridge is not reversible and can cause you to loose the data you have stored in it."
										+ "\nAre you sure?", cartridge.getName()))) {
			// revert removal
			model.checkEmbeddedCartridge(cartridge);
		}
	}

	private void executeAdditionalOperations(boolean add, IEmbeddableCartridge cartridge, 
			EmbeddableCartridgeDiff additionalOperations) {
		int result = openAdditionalOperationsDialog(
				NLS.bind("{0} Cartridges", add ? "Add" : "Remove"),
				createEmbeddingOperationMessage(add, additionalOperations));
		switch (result) {
		case 1:
			executeAdditionalOperations(cartridge, additionalOperations);
			break;
		case 0:
			undoAdditionOrRemoval(add, cartridge);
			break;
		case 2:
			// user has chosen to ignore additional requirements
		}
	}

	private EmbedCartridgeStrategy createEmbedCartridgeStrategy(IDomain domain) {
		IOpenShiftConnection connection = domain.getUser().getConnection();
		EmbedCartridgeStrategy embedCartridgeStrategy =
				new EmbedCartridgeStrategy(
						connection.getEmbeddableCartridges(),
						connection.getStandaloneCartridges(), 
						domain.getApplications());
		return embedCartridgeStrategy;
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
		builder.append(diff.toString());
		if (diff.hasRemovals()) {
			builder.append("\n\nRemoving cartridges is not reversible and may cause you to loose the data you have stored in it.");
		}
		builder.append("\n\nDo you want to Apply or Ignore these suggestions??");
		return builder.toString();
	}

	private void executeAdditionalOperations(IEmbeddableCartridge cartridge, EmbeddableCartridgeDiff diff) {
		if (createApplications(diff.getApplicationAdditions(), model.getDomain())) {
			uncheckEmbeddableCartridges(diff.getRemovals());
			checkEmbeddableCartridges(diff.getAdditions());
		} else {
			model.uncheckEmbeddedCartridge(cartridge);
		}
	}

	private void undoAdditionOrRemoval(boolean add, IEmbeddableCartridge cartridge) throws OpenShiftException {
		if (add) {
			model.uncheckEmbeddedCartridge(cartridge);
		} else {
			model.checkEmbeddedCartridge(cartridge);
		}
	}

	private void uncheckEmbeddableCartridges(List<IEmbeddableCartridge> removals) throws OpenShiftException {
		for (IEmbeddableCartridge embeddableCartridge : removals) {
			model.uncheckEmbeddedCartridge(embeddableCartridge);
		}
	}

	private void checkEmbeddableCartridges(List<IEmbeddableCartridge> additions) throws OpenShiftException {
		for (IEmbeddableCartridge embeddableCartridge : additions) {
			model.checkEmbeddedCartridge(embeddableCartridge);
		}
	}

	private boolean createApplications(List<IStandaloneCartridge> applicationAdditions, IDomain domain) {
		for (IStandaloneCartridge cartridge : applicationAdditions) {
			if (!LatestVersionOf.jenkins().matches(cartridge)) {
				throw new UnsupportedOperationException("only jenkins applications may currently be created.");
			}
			if (!createJenkinsApplication(domain)) {
				return false;
			}
		}
		return true;
	}

	private boolean createJenkinsApplication(IDomain domain) {
		final String name = openJenkinsApplicationDialog();
		if (name == null) {
			return false;
		}
		try {
			IGearProfile gear = null;
			gear = getFirstAvailableGearProfile(domain);
			if (gear == null) {
				IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Could not find any available gear profiles.");
				new ErrorDialog(getShell(), "Error", "Could create jenkins application.", status, -1).open();
				return false;
			}
			
			IStandaloneCartridge jenkinsCartridge = LatestVersionOf.jenkins().get(domain.getUser());
			CreateApplicationJob createJob =
					new CreateApplicationJob(
							name, jenkinsCartridge , ApplicationScale.NO_SCALE, gear, domain);
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
			IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getMessage());
			new ErrorDialog(getShell(), "Error", "Could not create jenkins application.", status, -1).open();		
		}
		return false;
	}

	private IGearProfile getFirstAvailableGearProfile(IDomain domain) throws OpenShiftEndpointException {
		IGearProfile gear = null;
		List<IGearProfile> gears = domain.getAvailableGearProfiles();
		if (gears != null
				&& !gears.isEmpty()) {
			gear = gears.get(0);
		}
		return gear;
	}

	private String openJenkinsApplicationDialog() {
		final JenkinsApplicationDialog dialog = new JenkinsApplicationDialog(getShell());
		if (dialog.open() != Dialog.OK) {
			return null;
		}
		return dialog.getValue();
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
	
	private int openQuestionDialog(String title, String message) {
		return new MessageDialog(getShell(),
				title, 
				null,
				message,
				MessageDialog.QUESTION, 
				new String[] { "No", "Yes" }, 0).open();
	}

	private void openLogDialog(final IApplication application, final boolean isTimeouted) {
		final LogEntry[] entries = LogEntryFactory.create(application, isTimeouted);
		if (entries.length == 0) {
			return;
		}
		wizardPage.getControl().getDisplay().syncExec(new Runnable() {
			
			@Override
			public void run() {
				new CreationLogDialog(getShell(), entries).open();
			}
		});
	}

	private Shell getShell() {
		return wizardPage.getControl().getShell();
	}

	private IWizardContainer getContainer() {
		return wizardPage.getWizard().getContainer();
	}
}
