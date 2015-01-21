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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy;
import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy.ApplicationRequirement;
import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy.EmbeddableCartridgeDiff;
import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy.IApplicationProperties;
import org.jboss.tools.openshift.express.internal.core.util.CartridgeToStringConverter;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.CreateApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.WaitForApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog.LogEntry;
import org.jboss.tools.openshift.express.internal.ui.wizard.LogEntryFactory;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftEndpointException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.client.cartridge.query.LatestVersionOf;

/**
 * A UI strategy that is able to add and remove embedded cartridges while
 * fullfilling requirements and resolving conflicts (ex. mutual exclusivity
 * etc.)
 * <p>
 * The basic assumption in this strategy is that the UI is adding/removing
 * cartridges from the model. This strategy will then correct the result and
 * add/remove further depdencies/conflicting cartridges and possibly do/undo
 * additions/removals if the users refuses these outcomes.
 * <p>
 * TODO: replaced this manual code by a generic dependency-tree analysis
 * mechanism as soon as OpenShift completed design of cartridge metamodel
 * 
 * @author Andre Dietisheim
 */
public class FullfillRequirementsCheckStrategy extends AbstractCheckEmbeddableCartridgeStrategy {

	private static final int RESULT_IGNORE = 2;
	private static final int RESULT_CANCEL = 0;
	private static final int RESULT_APPLY = 1;

	private EmbedCartridgeStrategy strategy;

	public FullfillRequirementsCheckStrategy(EmbeddedCartridgesWizardPageModel pageModel, IWizardPage wizardPage) {
		super(pageModel, wizardPage);
		this.strategy = createEmbedCartridgeStrategy(pageModel.getDomain());
	}

	private EmbedCartridgeStrategy createEmbedCartridgeStrategy(IDomain domain) {
		IOpenShiftConnection connection = domain.getUser().getConnection();
		EmbedCartridgeStrategy embedCartridgeStrategy =
				new EmbedCartridgeStrategy(
						new ArrayList<ICartridge>(connection.getEmbeddableCartridges()),
						new ArrayList<ICartridge>(connection.getStandaloneCartridges()), 
						domain.getApplications());
		return embedCartridgeStrategy;
	}
	
	@Override
	protected void add(ICartridge cartridge, CheckStateChangedEvent event) {
		try {
			ApplicationRequirement missingRequirement = 
					strategy.getMissingRequirement(cartridge, getPageModel());
			if (missingRequirement != null) {
				if (!shouldAddToInappropriateApplication(missingRequirement, cartridge, getPageModel())) {
					// revert cartidge addition (propagated by direct binding of UI/model)
					getPageModel().uncheckEmbeddedCartridge(cartridge);
					return;
				}
			}
			EmbeddableCartridgeDiff additionalOperations = 
					strategy.add(cartridge, getPageModel().getCheckedCartridges());
			if (additionalOperations.hasChanges()) {
				executeAdditionalOperations(true, cartridge, additionalOperations);
			}
		} catch (OpenShiftException e) {
			ExpressUIActivator.log("Could not process embeddable cartridges", e);
		}
	}
	
	@Override
	protected void remove(ICartridge cartridge, CheckStateChangedEvent event) {
		try {
			EmbeddableCartridgeDiff additionalOperations = 
					strategy.remove(cartridge, getPageModel().getCheckedCartridges());
			if (additionalOperations.hasChanges()) {
				getPageModel().uncheckEmbeddedCartridge(cartridge);
				executeAdditionalOperations(false, cartridge, additionalOperations);
			} else if (getPageModel().isEmbedded(cartridge)) {
				executeRemove(cartridge);
			}
		} catch (OpenShiftException e) {
			ExpressUIActivator.log("Could not process embeddable cartridges", e);
		}
	}

	private boolean shouldAddToInappropriateApplication(ApplicationRequirement requirement, ICartridge cartridge,
			IApplicationProperties applicationProperties) throws OpenShiftException {
		String title = NLS.bind("Inappropriate application {0}", applicationProperties.getApplicationName());
		String message = requirement.getMessage(cartridge, applicationProperties)
				+ NLS.bind("\n\nAdding may fail, are you sure that you want to add cartridge {0}?",
						cartridge.getName());
		switch (openQuestionDialog(title, message)) {
			default:
			case 0:
				return false;
			case 1:
				return true;
		}
	}

	private void executeRemove(ICartridge cartridge) {
		if(!MessageDialog.openQuestion(getShell(), 
				NLS.bind("Remove cartridge {0}", cartridge.getName()), 
						NLS.bind(
								"You are about to remove cartridge {0}.\n"
										+ "Removing a cartridge is not reversible and can cause you to loose the data you have stored in it."
										+ "\nAre you sure?", cartridge.getName()))) {
			// revert removal (propagated by direct binding UI/model)
			getPageModel().checkEmbeddedCartridge(cartridge);
		}
	}

	private void executeAdditionalOperations(boolean add, ICartridge cartridge, 
			EmbeddableCartridgeDiff additionalOperations) {
		int result = openAdditionalOperationsDialog(
				NLS.bind("{0} Cartridges", add ? "Add" : "Remove"),
				createEmbeddingOperationMessage(add, additionalOperations));
		switch (result) {
		case RESULT_APPLY:
			executeAdditionalOperations(cartridge, additionalOperations);
			break;
		case RESULT_CANCEL:
			undoAdditionOrRemoval(add, cartridge);
			break;
		case RESULT_IGNORE:
			// user has chosen to ignore additional requirements
		}
	}

	public int openAdditionalOperationsDialog(String title, String message) {
		MessageDialog dialog = new MessageDialog(getShell(),
				title, null, message, MessageDialog.QUESTION, new String[] { "Cancel", "Apply", "Ignore" }, RESULT_APPLY);
		return dialog.open();
	}
	
	private String createEmbeddingOperationMessage(boolean adding, EmbedCartridgeStrategy.EmbeddableCartridgeDiff diff) {
		StringBuilder builder = new StringBuilder();
		builder.append(NLS.bind("If you want to {0} {1}, it is suggested you:", 
				adding ? "add" : "remove",
				new CartridgeToStringConverter().toString(diff.getCartridge())));
		builder.append(diff.toString());
		if (diff.hasRemovals()) {
			builder.append("\n\nRemoving cartridges is not reversible and may cause you to loose the data you have stored in it.");
		}
		builder.append("\n\nDo you want to Apply or Ignore these suggestions??");
		return builder.toString();
	}

	private void executeAdditionalOperations(ICartridge cartridge, EmbeddableCartridgeDiff diff) {
		if (createApplications(diff.getApplicationAdditions(), getPageModel().getDomain())) {
			uncheckEmbeddableCartridges(diff.getRemovals());
			checkEmbeddableCartridges(diff.getAdditions());
		} else {
			getPageModel().uncheckEmbeddedCartridge(cartridge);
		}
	}

	private void undoAdditionOrRemoval(boolean add, ICartridge cartridge) throws OpenShiftException {
		if (add) {
			getPageModel().uncheckEmbeddedCartridge(cartridge);
		} else {
			getPageModel().checkEmbeddedCartridge(cartridge);
		}
	}

	private void uncheckEmbeddableCartridges(List<ICartridge> removals) throws OpenShiftException {
		for (ICartridge embeddableCartridge : removals) {
			getPageModel().uncheckEmbeddedCartridge(embeddableCartridge);
		}
	}

	private void checkEmbeddableCartridges(List<ICartridge> additions) throws OpenShiftException {
		for (ICartridge embeddableCartridge : additions) {
			getPageModel().checkEmbeddedCartridge(embeddableCartridge);
		}
	}

	private boolean createApplications(List<ICartridge> applicationAdditions, IDomain domain) {
		for (ICartridge cartridge : applicationAdditions) {
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
			IGearProfile gear = getFirstAvailableGearProfile(domain);
			if (gear == null) {
				IStatus status = new Status(IStatus.ERROR, ExpressUIActivator.PLUGIN_ID, "Could not find any available gear profiles.");
				new ErrorDialog(getShell(), "Error", "Could create jenkins application.", status, -1).open();
				return false;
			}
			
			IStandaloneCartridge jenkinsCartridge = LatestVersionOf.jenkins().get(domain.getUser());
			CreateApplicationJob createJob =
					new CreateApplicationJob(
							name, ApplicationScale.NO_SCALE, gear, jenkinsCartridge, domain);
			WizardUtils.runInWizard(
					createJob, createJob.getDelegatingProgressMonitor(), getContainer());
			IStatus result = createJob.getResult();
			if (JobUtils.isOk(result)) {
				IApplication application = createJob.getApplication();
				openLogDialog(application, createJob.isTimeouted(result));

				AbstractDelegatingMonitorJob job = new WaitForApplicationJob(application, getShell());
				IStatus waitStatus = WizardUtils.runInWizard(
						job, job.getDelegatingProgressMonitor(), getContainer());
				return JobUtils.isOk(waitStatus);
			}
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, ExpressUIActivator.PLUGIN_ID, e.getMessage());
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
	
	private void openLogDialog(final IApplication application, final boolean isTimeouted) {
		final LogEntry[] entries = LogEntryFactory.create(application, isTimeouted);
		if (entries.length == 0) {
			return;
		}
		getShell().getDisplay().syncExec(new Runnable() {
			
			@Override
			public void run() {
				new CreationLogDialog(getShell(), entries).open();
			}
		});
	}
}
