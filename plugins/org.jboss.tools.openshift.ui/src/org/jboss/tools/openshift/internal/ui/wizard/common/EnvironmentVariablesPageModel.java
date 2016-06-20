/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueItem;

/**
 * Page model to handle 
 * env variables
 * 
 * @author jeff.cantrill
 *
 */
public class EnvironmentVariablesPageModel extends ObservablePojo implements IEnvironmentVariablesPageModel{
	
	private List<EnvironmentVariable> environmentVariables = new ArrayList<>();
	private Map<String, String> imageEnvVars = new HashMap<>();
	private EnvironmentVariable selectedEnvironmentVariable = null;
	
	public  Map<String, String> getImageEnvVars(){
		return Collections.unmodifiableMap(imageEnvVars);
	}
	
	@Override
	public List<EnvironmentVariable> getEnvironmentVariables() {
		return environmentVariables;
	}
	
	@Override
	public void setEnvironmentVariables(List<EnvironmentVariable> envVars) {
		firePropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, 
				this.environmentVariables, 
				this.environmentVariables = envVars);
		this.imageEnvVars.clear();
		for (IKeyValueItem label : envVars) {
			imageEnvVars.put(label.getKey(), label.getValue());
		}
	}
	
	@Override
	public void setSelectedEnvironmentVariable(EnvironmentVariable envVar) {
		firePropertyChange(PROPERTY_SELECTED_ENVIRONMENT_VARIABLE, 
				this.selectedEnvironmentVariable, 
				this.selectedEnvironmentVariable = envVar);
	}

	@Override
	public EnvironmentVariable getSelectedEnvironmentVariable() {
		return selectedEnvironmentVariable;
	}

	@Override
	public void removeEnvironmentVariable(EnvironmentVariable envVar) {
		if(isPersistent(envVar)) {
			updateEnvironmentVariable(envVar, envVar.getKey(), DELETED);
			return;
		}
		final int i = environmentVariables.indexOf(envVar);
		if(i > -1) {
			List<EnvironmentVariable> old = new ArrayList<>(environmentVariables);
			this.environmentVariables.remove(i);
			fireIndexedPropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, i, old, Collections.unmodifiableList(environmentVariables));
		}
	}

	@Override
	public void updateEnvironmentVariable(EnvironmentVariable envVar, String key, String value) {
		if((isPersistent(envVar) || isPersistent(key)) && !key.equals(envVar.getKey())) {
			addEnvironmentVariable(key, value);
			removeEnvironmentVariable(envVar);
			return;
		}
		final int i = environmentVariables.indexOf(envVar);
		if(i > -1) {
			List<EnvironmentVariable> old = new ArrayList<>(environmentVariables);
			EnvironmentVariable prev = environmentVariables.get(i);
			environmentVariables.set(i, new EnvironmentVariable(key, value, prev.isNew()));
			fireIndexedPropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, i, old, Collections.unmodifiableList(environmentVariables));
		}
	}	
	@Override
	public void resetEnvironmentVariable(EnvironmentVariable envVar) {
		if(isPersistent(envVar)) {
			updateEnvironmentVariable(envVar, envVar.getKey(), imageEnvVars.get(envVar.getKey()));
		}
	}


	@Override
	public void addEnvironmentVariable(String key, String value) {
		if(isPersistent(key)) {
			EnvironmentVariable envVar = find(key);
			if(envVar != null) {
				updateEnvironmentVariable(envVar, key, value);
				return;
			}
		}
		List<EnvironmentVariable> old = new ArrayList<>(environmentVariables);
		this.environmentVariables.add(new EnvironmentVariable(key, value, true));
		firePropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, old, Collections.unmodifiableList(environmentVariables));
	}

	@Override
	public boolean isEnvironmentVariableModified(EnvironmentVariable envVar) {
		return envVar.isNew() || (isPersistent(envVar) && !Objects.equals(imageEnvVars.get(envVar.getKey()), envVar.getValue()));
	}
	
	@Override
	public EnvironmentVariable getEnvironmentVariable(String key) {
		return environmentVariables.stream().filter(var -> key.equals(var.getKey())).findAny().orElse(null);
	}

	public boolean isPersistent(EnvironmentVariable envVar) {
		return isPersistent(envVar.getKey());
	}

	public boolean isPersistent(String envVarKey) {
		return imageEnvVars.containsKey(envVarKey);
	}

	public EnvironmentVariable find(String key) {
		return environmentVariables.stream().filter(v -> key.equals(v.getKey())).findFirst().orElse(null);
	}

}
