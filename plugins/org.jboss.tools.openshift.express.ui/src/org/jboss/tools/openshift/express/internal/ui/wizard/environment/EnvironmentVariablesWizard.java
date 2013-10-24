/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.environment;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.IApplication;
import com.openshift.client.IEnvironmentVariable;

/**
 * @author Martes G Wigglesworth
 * @author Andre Dietisheim
 */
public class EnvironmentVariablesWizard extends AbstractOpenShiftWizard<EnvironmentVariablesWizardModel> {

	private Map<String, String> environmentVarriableValueByKey;

	public EnvironmentVariablesWizard(Map<String, String> environmentVariables) {
		super("Manage Application Environment Variable(s)",
				new EnvironmentVariablesWizardModel(environmentVariables));
	}

	public EnvironmentVariablesWizard(IApplication application) {
		super(NLS.bind("Manage Application Environment Variable(s) for application {0}", application.getName()),
				new EnvironmentVariablesWizardModel(application));
	}

	@Override
	public void addPages() {
		addPage(new EnvironmentVariablesWizardPage(getModel(), this));
	}

	@Override
	public boolean performFinish() {
		IApplication application = getModel().getApplication();
		if (isEditApplication(application)) {
			try {
				WizardUtils.runInWizard(new UpdateEnvironmentVariableJob(application, getModel().getVariables()), getContainer());
			} catch (InvocationTargetException e) {
				Logger.error((application == null ?
						"Could not edit environment variables"
						: NLS.bind("Could not edit environment variables for application {0}", application.getName())),
						e);
			} catch (InterruptedException e) {
				Logger.error((application == null ?
						"Could not edit environment variables"
						: NLS.bind("Could not edit environment variables for application {0}", application.getName())),
						e);
			}
		} else {
			this.environmentVarriableValueByKey = (toMap(getModel().getVariables()));
		}
		return true;
	}

	private boolean isEditApplication(IApplication application) {
		return application != null;
	}

	private Map<String, String> toMap(List<EnvironmentVariableItem> variables) {
		HashMap<String, String> environmentVariables = new LinkedHashMap<String, String>();
		for (EnvironmentVariableItem variable : variables) {
			environmentVariables.put(variable.getName(), variable.getValue());
		}
		return environmentVariables;
	}
	
	public Map<String, String> getEnvironmentVariables() {
		return environmentVarriableValueByKey;
	}

	private class UpdateEnvironmentVariableJob extends AbstractDelegatingMonitorJob {

		private IApplication application;
		private List<EnvironmentVariableItem> variables;

		public UpdateEnvironmentVariableJob(IApplication application, List<EnvironmentVariableItem> variables) {
			super("Processing environment variables...");
			this.application = application;
			this.variables = variables;
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			EnvironmentVariablesDiff diff = new EnvironmentVariablesDiff(variables, application.getEnvironmentVariables());
			remove(diff.getRemovals(), application);
			add(diff.getAdditions(), application);
			update(diff.getUpdates(), application);
			return Status.OK_STATUS;
		}

		private void add(List<EnvironmentVariableItem> toAdd, IApplication application) {
			for (EnvironmentVariableItem variable : toAdd) {
				application.addEnvironmentVariable(variable.getName(), variable.getValue());
			}
		}

		private void update(List<EnvironmentVariableItem> toUpdate, IApplication application) {
			for (EnvironmentVariableItem variable : toUpdate) {
				IEnvironmentVariable environmentVariable = application.getEnvironmentVariable(variable.getName());
				if (environmentVariable != null) {
					environmentVariable.update(variable.getValue());
				}
			}
		}

		private void remove(List<String> namesToRemove, IApplication application) {
			for (String name : namesToRemove) {
				IEnvironmentVariable environmentVariable = application.getEnvironmentVariable(name);
				if (environmentVariable != null) {
					environmentVariable.destroy();
				}
			}
		}
	}

	private class EnvironmentVariablesDiff {

		private List<String> removals = new ArrayList<String>();
		private List<EnvironmentVariableItem> additions = new ArrayList<EnvironmentVariableItem>();
		private List<EnvironmentVariableItem> updates = new ArrayList<EnvironmentVariableItem>();
		
		public EnvironmentVariablesDiff(List<EnvironmentVariableItem> editedVariables, Map<String, IEnvironmentVariable> existingVariables) {
			init(editedVariables, existingVariables);
		}

		private void init(List<EnvironmentVariableItem> editedVariables, Map<String, IEnvironmentVariable> existingVariables) {
			processAdditionsAndUpdates(editedVariables, existingVariables);
			processRemovals(editedVariables, existingVariables);
		}
		
		private void processAdditionsAndUpdates(List<EnvironmentVariableItem> editedVariables,
				Map<String, IEnvironmentVariable> existingVariables) {
			for (EnvironmentVariableItem variable : editedVariables) {
				IEnvironmentVariable existingVariable = existingVariables.get(variable.getName());
				if (existingVariable == null) {
					additions.add(variable);
				} else if(!equals(existingVariable.getValue(), variable.getValue())) {
					updates.add(variable);
				}
			}
		}

		private boolean equals(String thisValue, String thatValue) {
			if (thisValue == null) {
				return thatValue == null;
			} else {
				return thisValue.equals(thatValue);
			}
		}

		private void processRemovals(List<EnvironmentVariableItem> editedVariables,
				Map<String, IEnvironmentVariable> existingVariables) {
			for (String name : existingVariables.keySet()) {
				if (!contains(name, editedVariables)) {
					removals.add(name);
				}
			}
		}

		private boolean contains(String name, List<EnvironmentVariableItem> editedVariables) {
			for (EnvironmentVariableItem variable : editedVariables) {
				if (variable.getName().equals(name)) {
					return true;
				}
			}
			return false;
		}
		
		public List<EnvironmentVariableItem> getUpdates() {
			return updates;
		}
		
		public List<EnvironmentVariableItem> getAdditions() {
			return additions;
		}
		
		public List<String> getRemovals() {
			return removals;
		}
	}
	
}
