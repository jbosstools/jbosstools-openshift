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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.net.SocketTimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.OpenShiftException;

/**
 * A UI strategy that is able to add and remove embedded cartridges while
 * fullfilling requirements and resolving conflicts (ex. mutual exclusivity
 * etc.)
 * 
 * @author Andre Dietisheim
 */
public class EmbedCartridgeListener implements ICheckStateListener {

	private CheckboxTableViewer viewer;
	private IEmbedCartridgesWizardPageModel pageModel;
	private IWizardPage wizardPage;

	public EmbedCartridgeListener(CheckboxTableViewer viewer, 
			IEmbedCartridgesWizardPageModel pageModel, IWizardPage wizardPage) {
		this.viewer = viewer;
		this.wizardPage = wizardPage;
		this.pageModel = pageModel;
	}

	public void checkStateChanged(CheckStateChangedEvent event) {
		try {
			IEmbeddableCartridge cartridge = (IEmbeddableCartridge) event.getElement();
			if (event.getChecked()) {
				addCartridge(cartridge);
			} else {
				removeCartridge(cartridge);
			}
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
		} catch (SocketTimeoutException e) {
			OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
		}
	}

	private void addCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException {
		if (IEmbeddableCartridge.PHPMYADMIN_34.equals(cartridge)) {
			addPhpMyAdmin();
		} else if (IEmbeddableCartridge.JENKINS_14.equals(cartridge)) {
			addJenkins(cartridge);
		} else if (IEmbeddableCartridge.MYSQL_51.equals(cartridge)) {
			addMySql();
		} else if (IEmbeddableCartridge.POSTGRESQL_84.equals(cartridge)) {
			addPostgreSql();
		} else if (IEmbeddableCartridge.ROCKMONGO_11.equals(cartridge)) {
			addRockMongo();
		} else if (IEmbeddableCartridge._10GEN_MMS_AGENT_01.equals(cartridge)) {
			add10gen();
		} else {
			pageModel.selectEmbeddedCartridges(cartridge);
		}
	}

	private void removeCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException {
		if (IEmbeddableCartridge.MYSQL_51.equals(cartridge)) {
			removeMySQL();
		} else if (IEmbeddableCartridge.MONGODB_20.equals(cartridge)) {
			removeMongoDb();
		} else {
			pageModel.unselectEmbeddedCartridges(cartridge);
		}
	}

	private void addJenkins(final IEmbeddableCartridge cartridge) throws OpenShiftException,
			SocketTimeoutException {
		if (pageModel.hasApplicationOfType(ICartridge.JENKINS_14)) {
			pageModel.getSelectedEmbeddableCartridges().add(cartridge);
		} else {
			final JenkinsApplicationDialog dialog = new JenkinsApplicationDialog(getShell());
			if (dialog.open() == Dialog.OK) {
				createJenkinsApplication(cartridge, dialog.getValue());
			} else {
				viewer.setChecked(cartridge, false);
			}
		}
	}

	private void createJenkinsApplication(final IEmbeddableCartridge cartridge, final String name) {
		try {
			WizardUtils.runInWizard(new Job(NLS.bind("Creating jenkins application \"{0}\"...", name)) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						IApplication jenkinsApplication = pageModel.createJenkinsApplication(name, monitor);
						pageModel.getSelectedEmbeddableCartridges().add(cartridge);
						openLogDialog(jenkinsApplication);
						return Status.OK_STATUS;
					} catch (Exception e) {
						getShell().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								viewer.setChecked(cartridge, false);
							}
						});
						return OpenShiftUIActivator.createErrorStatus("Could not load embeddable cartridges", e);
					}
				}

			}, getContainer());
		} catch (Exception e) {
			// ignore
		}
	}

	private void addPhpMyAdmin() throws OpenShiftException, SocketTimeoutException {
		if (!viewer.getChecked(IEmbeddableCartridge.MYSQL_51)) {
			if (MessageDialog.openQuestion(getShell(), "Embed phpMyAdmin Cartridge",
					"To embed phpMyAdmin, you'd also have to embed MySQL. \n\nAlso embed MySQL?")) {
				viewer.setChecked(IEmbeddableCartridge.MYSQL_51, true);
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.PHPMYADMIN_34);
			} else {
				viewer.setChecked(IEmbeddableCartridge.PHPMYADMIN_34, false);
			}
		} else {
			pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.PHPMYADMIN_34);
		}
	}

	private void addMySql()
			throws OpenShiftException, SocketTimeoutException {
		if (viewer.getChecked(IEmbeddableCartridge.POSTGRESQL_84)) {
			if (MessageDialog
					.openQuestion(getShell(), "Remove PostgreSQL Cartridge",
							"MySQL and PostgreSQL are mutually exclusive. To embed MySQL, you have to remove PostgreSQL. \n\nRemove PostgreSQL?")) {
				viewer.setChecked(IEmbeddableCartridge.POSTGRESQL_84, false);
				pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.POSTGRESQL_84);
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
			} else {
				viewer.setChecked(IEmbeddableCartridge.MYSQL_51, false);
				pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
			}
		} else {
			pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
		}
	}

	private void addPostgreSql()
			throws OpenShiftException, SocketTimeoutException {
		if (viewer.getChecked(IEmbeddableCartridge.MYSQL_51)) {
			if (MessageDialog
					.openQuestion(getShell(), "Remove MySQL Cartridge",
							"MySQL and PostgreSQL are mutually exclusive. To embed PostgreSQL, you have to remove MySQL.\n\nRemove MySQL?")) {
				viewer.setChecked(IEmbeddableCartridge.MYSQL_51, false);
				pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.POSTGRESQL_84);
			} else {
				viewer.setChecked(IEmbeddableCartridge.POSTGRESQL_84, false);
			}
		} else {
			pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.POSTGRESQL_84);
		}
	}

	private void removeMySQL() throws OpenShiftException, SocketTimeoutException {
		if (viewer.getChecked(IEmbeddableCartridge.PHPMYADMIN_34)) {
			if (MessageDialog.openQuestion(getShell(), "Remove phpmyadmin cartridge",
					"If you remove the mysql cartridge, you'd also have to remove phpmyadmin.\n\nRemove phpMyAdmin and MySQL?")) {
				pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.PHPMYADMIN_34);
				pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
				viewer.setChecked(IEmbeddableCartridge.PHPMYADMIN_34, false);
			} else {
				viewer.setChecked(IEmbeddableCartridge.MYSQL_51, true);
			}
		} else {
			pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
		}
	}

	private void addRockMongo() throws OpenShiftException, SocketTimeoutException {
		if (!viewer.getChecked(IEmbeddableCartridge.MONGODB_20)) {
			if (MessageDialog.openQuestion(getShell(), "Embed MongoDB Cartridge",
					"To embed RockMongo, you'd also have to embed MongoDB. \n\nAlso embed MongoDB?")) {
				viewer.setChecked(IEmbeddableCartridge.MONGODB_20, true);
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.MONGODB_20);
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.ROCKMONGO_11);
			} else {
				viewer.setChecked(IEmbeddableCartridge.MONGODB_20, false);
			}
		} else {
			pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.PHPMYADMIN_34);
		}
	}

	private void removeMongoDb() throws OpenShiftException, SocketTimeoutException {
		if (viewer.getChecked(IEmbeddableCartridge.ROCKMONGO_11)) {
			if (MessageDialog.openQuestion(getShell(), "Remove MongoDB cartridge",
					"If you remove the MongoDB cartridge, you'd also have to remove RockMongo.")) {
				pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.ROCKMONGO_11);
				viewer.setChecked(IEmbeddableCartridge.ROCKMONGO_11, false);
			} else {
				viewer.setChecked(IEmbeddableCartridge.MONGODB_20, true);
			}
		} else {
			pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.MONGODB_20);
		}
	}

	private void add10gen() throws OpenShiftException, SocketTimeoutException {
		if (!viewer.getChecked(IEmbeddableCartridge.MONGODB_20)) {
			if (MessageDialog.openQuestion(getShell(), "Embed 10gen Cartridge",
					"To embed 10gen cartridge, you'd also have to embed MongoDB. \n\nAlso embed MongoDB?")) {
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.MONGODB_20);
				viewer.setChecked(IEmbeddableCartridge.MONGODB_20, true);
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge._10GEN_MMS_AGENT_01);
			} else {
				viewer.setChecked(IEmbeddableCartridge.MONGODB_20, false);
				pageModel.unselectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
				viewer.setChecked(IEmbeddableCartridge._10GEN_MMS_AGENT_01, false);
			}
		} else {
			pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.PHPMYADMIN_34);
		}
	}

	
	private Shell getShell() {
		return viewer.getControl().getShell();
	}

	private IWizardContainer getContainer() {
		return wizardPage.getWizard().getContainer();
	}

	private void openLogDialog(final IApplication application) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), application).open();
			}
		});
	}

	private static class JenkinsApplicationDialog extends InputDialog {

		public JenkinsApplicationDialog(Shell shell) {
			super(
					shell,
					"New Jenkins application",
					"To embed Jenkins into your application, you first have to create a separate Jenkins application. "
							+ "Please provide a name for this new Jenkins application (lower-case letters and digits only):"
					, null, new JenkinsNameValidator());
		}

		private static class JenkinsNameValidator implements IInputValidator {

			@Override
			public String isValid(String input) {
				if (StringUtils.isEmpty(input)) {
					return "You have to provide a name for the jenkins application";
				}

				if (!StringUtils.isAlphaNumeric(input)) {
					return "The name may only contain lower-case letters and digits.";
				}
				return null;
			}
		}
	}
}
