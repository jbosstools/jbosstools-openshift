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
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IEnvironmentVariable;
import com.openshift.client.IField;
import com.openshift.client.Message;
import com.openshift.client.Messages;
import com.openshift.client.OpenShiftException;

/**
 * @author Martes G Wigglesworth 
 * @author Martin Rieman
 *
 */
public abstract class AbstractEnvironmentalVariablesWizardPageModel extends ObservableUIPojo{

	/**
	 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
	 *
	 */
	public class EnvironmentalVariableIterator implements Iterable<Object> {

		/**
		 * Constructs a new instance of EnvironmentalVariableIterator
		 */
		public EnvironmentalVariableIterator() {
			// TODO Auto-generated constructor stub
		}

		/* (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<Object> iterator() {
			// TODO Auto-generated method stub
			return null;
		}

	}
	/**
	 * Constructs a new instance of
	 * AbstractEnvironmentalVariablesWizardPageModel
	 */
	public AbstractEnvironmentalVariablesWizardPageModel() {
		pageTitle = null;
		userConnection = null;
		variablesDB = new ArrayList<IEnvironmentVariable>();
		this.selectedVariable = null;
	}
	
	/**
	 * Fully constructs a new instance of 
	 * AbstractEnvironmentalVariablesWizardPageModel
	 */
	public AbstractEnvironmentalVariablesWizardPageModel(String pageTitle, Connection user, List<IEnvironmentVariable> variables, IEnvironmentVariable selectedVariable) {
		this.pageTitle = pageTitle;
		this.userConnection = user;
		this.variablesDB = variables;
		this.selectedVariable = selectedVariable;
	}

	public String getDescription()
	{
		return description;
	}

	/**
	 * @return the pageTitle
	 */
	public String getPageTitle() {
		return pageTitle;
	}

	/**
	 * @return the userConnection
	 */
	public Connection getUserConnection() {
		return userConnection;
	}

	/**
	 * @return the variablesDB
	 */
	public List<IEnvironmentVariable> getVariablesDB() {
		return variablesDB;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * @param string
	 */
	public void setPageTitle(String newTitle) {
		pageTitle=newTitle;
	}

	/**
	 * @param userConnection
	 *            the userConnection to set
	 */
	public void setUserConnection(Connection userConnection) {
		this.userConnection = userConnection;
	}

	/**
	 * @param variablesDB
	 *            the variablesDB to set
	 */
	public void setVariablesDB(List<IEnvironmentVariable> variablesDB) {
		this.variablesDB = variablesDB;
	};

	/**
	 * 
	 * @param newKey
	 */
	protected String add(IEnvironmentVariable envVariable)
	{
		this.variablesDB.add(envVariable);
		setSelectedVariable(envVariable);
		return envVariable.getName();
	}

	/**
	 * 
	 * @param target
	 */
	protected String delete(IEnvironmentVariable envVariable)
	{
		this.variablesDB.remove(envVariable);
		envVariable.delete();
		return "true";
	}

	/**
	 * 
	 * @param target
	 * @param value
	 */
	protected String update(IEnvironmentVariable envVariable)
	{
		this.variablesDB.get(variablesDB.lastIndexOf(selectedVariable)).update(envVariable.getValue());
		setSelectedVariable(envVariable);
		return envVariable.getName();
	}

	/**
	 * @return the selectedVariable
	 */
	public IEnvironmentVariable getSelectedVariable() {
		return selectedVariable;
	}

	/**
	 * @param selectedVariable the selectedVariable to set
	 */
	public void setSelectedVariable(IEnvironmentVariable selectedVariable) {
		firePropertyChange(PROPERTY_SELECTED_VARIABLE, this.selectedVariable, this.selectedVariable = selectedVariable);
	}
	
	public void refresh() {
		userConnection.refresh();
		restoreSelectedEnvironmentVariable();
	}

	/**
	 * 
	 */
	private void restoreSelectedEnvironmentVariable() {
		IEnvironmentVariable variableToSelect = selectedVariable;
		if (variableToSelect == null
				|| !userConnection.hasSSHKeyName(variableToSelect.getName())) {
			variableToSelect = getFirstKey();
		}
		setSelectedVariable(variableToSelect);
	}
	
	private IEnvironmentVariable getFirstKey() {
		if(getVariablesDB().size() == 0) {
			return null;
		} 
		return getVariablesDB().get(0);
	}
	
	public void removeVariable() {
		if (selectedVariable == null) {
			return;
		}
		//selectedVariable..destroy();
		selectedVariable = null;
		restoreSelectedEnvironmentVariable();
	}
	
	private String description;
	
	private String pageTitle;
	private Connection userConnection;
	private List<IEnvironmentVariable> variablesDB;
	/*
	 * Another complex data type that works on the specific object that is used
	 * in the client branch from JBIDE-15598 may make more sense.
	 */
	
	private IEnvironmentVariable selectedVariable;
	
	public static final String PROPERTY_SELECTED_VARIABLE = "selectedVariable";
	public static final String PROPERTY_VARIABLES_DB = "variablesDB";
	
	public IEnvironmentVariable fakeIEnvironmentVariable(){
		return new IEnvironmentVariable() {
			@Override public void refresh() throws OpenShiftException {}
			@Override public boolean hasCreationLog() {return false;}
			@Override public Messages getMessages() {return new Messages(new HashMap<IField, List<Message>>());}
			@Override public String getCreationLog() {return null;}
			@Override public void update(String value) throws OpenShiftException {}
			@Override public String getValue() {return "Value";}
			@Override public String getName() {return "Name";}
			@Override public void delete() throws OpenShiftException {}
		};
	}

}
