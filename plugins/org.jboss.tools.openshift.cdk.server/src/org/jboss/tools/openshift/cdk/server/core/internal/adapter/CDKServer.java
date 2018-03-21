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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.jboss.ide.eclipse.as.core.util.ServerNamingUtility;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.core.credentials.ICredentialDomain;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;

public class CDKServer extends ServerDelegate {

	public static final String CDK_SERVER_TYPE = "org.jboss.tools.openshift.cdk.server.type";
	public static final String CDK_V3_SERVER_TYPE = "org.jboss.tools.openshift.cdk.server.type.v3";
	public static final String CDK_V32_SERVER_TYPE = "org.jboss.tools.openshift.cdk.server.type.v32";
	public static final String MINISHIFT_1_7_SERVER_TYPE = "org.jboss.tools.openshift.cdk.server.type.minishift.v17";
	
	public static final String[] MINISHIFT_BASED_TYPES = new String[]{
			CDK_V3_SERVER_TYPE, CDK_V32_SERVER_TYPE, MINISHIFT_1_7_SERVER_TYPE
	};
	
	// Does not include minishift, only cdks
	public static final String[] MINISHIFT_BASED_CDKS = new String[]{
			CDK_V3_SERVER_TYPE, CDK_V32_SERVER_TYPE
	};

	public static final String PROP_FOLDER = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.FOLDER";

	public static final String PROP_PASS_CREDENTIALS = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.passCredentials";
	public static final String PROP_USERNAME = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.username";
	public static final String PROP_USER_ENV_VAR = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.env.user";
	public static final String PROP_PASS_ENV_VAR = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.env.pass";

	public static final String PROP_SKIP_REG = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.skip.registration";
	public static final String PROP_SKIP_UNREG = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.skip.unregistration";

	public CDKServer() {
	}

	@Override
	public void setDefaults(IProgressMonitor monitor) {
		getServerWorkingCopy().setHost("localhost"); //$NON-NLS-1$
		setDefaultServerName(monitor);
		setAttribute(PROP_PASS_CREDENTIALS, true);
	}

	/**
	 * Initializes this server with a default server name. 
	 * This method is called when a new server is created so that the server 
	 * can be initialized with a name suitable to the server type. 
	 * 
	 * This method currently overrides a *nonexistant* upstream method, which
	 * is only proposed in upstream bug. 
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	public void setDefaultServerName(IProgressMonitor monitor) {
		getServerWorkingCopy().setName(ServerNamingUtility.getDefaultServerName(getBaseName()));
	}

	protected String getBaseName() {
		return CDKServer.getServerTypeBaseName();
	}

	public static String getServerTypeBaseName() {
		return "Container Development Environment";
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
		ControllableServerBehavior beh = (ControllableServerBehavior) getServer()
				.loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
		Object user2 = beh.getSharedData(CDKServerBehaviour.PROP_CACHED_USER);
		if (user2 instanceof String)
			return (String) user2;

		String user = getServer().getAttribute(PROP_USERNAME, (String) null);
		if (user == null) {
			ICredentialDomain domain = CredentialService.getCredentialModel()
					.getDomain(CredentialService.REDHAT_ACCESS);
			user = domain.getDefaultUsername();
		}
		return user;
	}

	public String getPassword() throws UsernameChangedException {
		ControllableServerBehavior beh = (ControllableServerBehavior) getServer()
				.loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
		Object pw = beh.getSharedData(CDKServerBehaviour.PROP_CACHED_PASSWORD);
		if (pw instanceof String)
			return (String) pw;

		ICredentialDomain domain = CredentialService.getCredentialModel().getDomain(CredentialService.REDHAT_ACCESS);
		String user = getUsername();
		if (user != null && domain != null) {
			try {
				return domain.getCredentials(user);
			} catch (StorageException se) {
				CDKCoreActivator.getDefault().getLog()
						.log(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, se.getMessage(), se));
			} catch (UsernameChangedException uce) {
				if (uce.getSaveCredentials()) {
					saveChangedCredentials(uce);
				}
				throw uce;
			}
		}
		return null;
	}

	private void saveChangedCredentials(UsernameChangedException uce) {
		// The user has changed the username and is now requesting we save the changes
		IServerWorkingCopy wc = getServerWorkingCopy();
		if (wc == null) {
			wc = getServer().createWorkingCopy();

			// a server stored in metadata will have a null file. 
			IFile f = ((Server) getServer()).getFile();
			wc.setAttribute(PROP_USERNAME, uce.getUser());
			if (f == null) {
				try {
					wc.save(true, new NullProgressMonitor());
				} catch (CoreException ce) {
					CDKCoreActivator.pluginLog().logError("Error persisting changed username", ce);
				}
			} else {
				// Job scope rules will not allow us to change the credentials most likely, if called from a job
				// The change to user/pass will be sent through the usernameChangeException and so 
				// persisting it immediately is not important, since calling client will already have access to it
				final IServerWorkingCopy wc2 = wc;
				WorkspaceJob wj = new WorkspaceJob("Saving updated credentials on server " + getServer().getName()) {
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						wc2.save(true, new NullProgressMonitor());
						return Status.OK_STATUS;
					}
				};
				wj.schedule();
			}
		}
	}

	public boolean passCredentials() {
		boolean passCredentials = getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
		return passCredentials;
	}

	public String getUserEnvironmentKey() {
		return getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
	}

	public String getPasswordEnvironmentKey() {
		return getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK_ENV_SUB_PASSWORD);
	}

	public boolean skipRegistration() {
		return getServer().getAttribute(CDKServer.PROP_SKIP_REG, false);
	}

	public boolean skipUnregistration() {
		return getServer().getAttribute(CDKServer.PROP_SKIP_UNREG, false);
	}
	
	@Override
	public ServerPort[] getServerPorts() {
		return new ServerPort[0];
	}


}
