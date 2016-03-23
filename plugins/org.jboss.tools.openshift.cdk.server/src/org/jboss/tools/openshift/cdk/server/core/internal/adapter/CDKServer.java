/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal.adapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.core.credentials.ICredentialDomain;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;

public class CDKServer extends ServerDelegate {

	public static final String CDK_SERVER_TYPE = "org.jboss.tools.openshift.cdk.server.type";
	public static final String PROP_FOLDER = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.FOLDER";
	
	
	public static final String PROP_PASS_CREDENTIALS = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.passCredentials"; 
	public static final String PROP_USERNAME = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.username"; 
	public static final String PROP_USER_ENV_VAR = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.env.user"; 
	public static final String PROP_PASS_ENV_VAR = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.env.pass"; 


	public static final String SUB_USERNAME = "SUB_USERNAME";
	public static final String SUB_PASSWORD = "SUB_PASSWORD";
	
	public CDKServer() {
	}
	
	public void setDefaults(IProgressMonitor monitor) {
		getServerWorkingCopy().setHost("localhost"); //$NON-NLS-1$
		setAttribute(PROP_PASS_CREDENTIALS, true);
	}

	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		return Status.CANCEL_STATUS;
	}

	@Override
	public IModule[] getChildModules(IModule[] module) {
		return new IModule[0];
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		return new IModule[0];
	}

	@Override
	public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
	}
	
	public String getUsername() {
		ControllableServerBehavior beh = (ControllableServerBehavior)getServer().loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
		Object user2 = beh.getSharedData(CDKServerBehaviour.PROP_CACHED_USER);
		if( user2 instanceof String )
			return (String)user2;
		
		String user = getServer().getAttribute(PROP_USERNAME, (String)null);
		if( user == null ) {
			ICredentialDomain domain = CredentialService.getCredentialModel().getDomain(CredentialService.REDHAT_ACCESS);
			user = domain.getDefaultUsername();
		}
		return user;
	}
	
	public String getPassword() throws UsernameChangedException {
		ControllableServerBehavior beh = (ControllableServerBehavior)getServer().loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
		Object pw = beh.getSharedData(CDKServerBehaviour.PROP_CACHED_PASSWORD);
		if( pw instanceof String )
			return (String)pw;
		
		ICredentialDomain domain = CredentialService.getCredentialModel().getDomain(CredentialService.REDHAT_ACCESS);
		String user = getUsername();
		if( user != null && domain != null) {
			try {
				return domain.getCredentials(user);
			} catch(StorageException se) {
				CDKCoreActivator.getDefault().getLog().log(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, se.getMessage(), se));
			} catch(UsernameChangedException uce) {
				if( uce.getSaveCredentials()) {
					// The user has changed the username and is now requesting we save the changes
					IServerWorkingCopy wc = getServerWorkingCopy();
					if( wc == null ) {
						wc = getServer().createWorkingCopy();
						wc.setAttribute(PROP_USERNAME, uce.getUser());
						try {
							wc.save(true, new NullProgressMonitor());
						} catch(CoreException ce) {
							CDKCoreActivator.pluginLog().logError("Error persisting changed username", ce);
						}
					}
				}
				throw uce;
			}
		}
		return null;
	}
}
