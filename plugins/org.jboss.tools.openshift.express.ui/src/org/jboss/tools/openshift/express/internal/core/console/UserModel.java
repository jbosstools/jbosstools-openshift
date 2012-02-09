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
package org.jboss.tools.openshift.express.internal.core.console;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.client.User;

public class UserModel {
	private static UserModel model;
	public static UserModel getDefault() {
		if( model == null )
			model = new UserModel();
		return model;
	}
	
	
	/** The most recent user connected on OpenShift. */
	private IUser recentUser = null;
	private HashMap<String, IUser> allUsers = new HashMap<String,IUser>();
	
	/**
	 * Create a user for temporary external use
	 * 
	 * @param username
	 * @param password
	 * @return
	 * @throws OpenShiftException
	 * @throws IOException
	 */
	public IUser createUser(String username, String password) throws OpenShiftException, IOException {
		IUser u = new User(username, password, OpenShiftUIActivator.PLUGIN_ID + " " + 
				OpenShiftUIActivator.getDefault().getBundle().getVersion());
		return u;
	}

	public void addUser(IUser user) {
		try {
			allUsers.put(user.getRhlogin(), recentUser);
			this.recentUser = user;
		} catch(OpenShiftException ose ) {
			// TODO 
		}
	}
	
	public IUser getRecentUser() {
		return recentUser;
	}
	
	public IUser findUser(String username) {
		try {
			for( int i = 0; i < allUsers.size(); i++ ) {
				if( allUsers.get(i).getUUID().equals(username))
					return allUsers.get(i);
			}
		} catch(OpenShiftException ose) {
			
		}
		return null;
	}
	
	public IUser[] getUsers() {
		Collection<IUser> c = allUsers.values();
		IUser[] rets = (IUser[]) c.toArray(new IUser[c.size()]);
		return rets;
	}
	
	public void load() {
		// TODO
	}
	
	public void save() {
		// TODO
		// save the passwords in secure storage, save the username list somewhere else
	}
	
}
