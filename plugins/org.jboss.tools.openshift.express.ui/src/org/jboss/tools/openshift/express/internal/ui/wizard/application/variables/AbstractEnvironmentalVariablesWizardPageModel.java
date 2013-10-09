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

import java.util.HashMap;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author Martes G Wigglesworth
 * 
 */
public abstract class AbstractEnvironmentalVariablesWizardPageModel extends ObservableUIPojo {

	/**
	 * Constructs a new instance of
	 * AbstractEnvironmentalVariablesWizardPageModel
	 */
	public AbstractEnvironmentalVariablesWizardPageModel() {
		pageTitle = null;
		userConnection = null;
		variablesDB = null;
	}

	/**
	 * @return the pageTitle
	 */
	public String getPageTitle() {
		return pageTitle;
	}

	private String description;
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description= description;
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
	public HashMap<String, String> getVariablesDB() {
		return variablesDB;
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
	public void setVariablesDB(HashMap<String, String> variablesDB) {
		this.variablesDB = variablesDB;
	};

	/**
	 * 
	 * @param newKey
	 */
	protected String add(String newKey, String newValue)
	{
		return this.variablesDB.put(newKey, newValue);
	}

	/**
	 * 
	 * @param target
	 */
	protected String delete(String key)
	{
		return this.variablesDB.remove(key);
	}

	/**
	 * 
	 * @param target
	 * @param value
	 */
	protected String updateKey(String target, String updateValue)
	{
		return this.variablesDB.put(target, updateValue);
	}

	private String pageTitle;
	private Connection userConnection;
	private HashMap<String, String> variablesDB;
}
