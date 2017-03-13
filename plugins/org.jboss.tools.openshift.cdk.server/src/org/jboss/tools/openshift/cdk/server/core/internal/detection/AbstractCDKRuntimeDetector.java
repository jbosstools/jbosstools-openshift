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
package org.jboss.tools.openshift.cdk.server.core.internal.detection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.core.util.ServerNamingUtility;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.core.credentials.ICredentialDomain;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.runtime.core.model.AbstractRuntimeDetectorDelegate;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;

public abstract class AbstractCDKRuntimeDetector extends AbstractRuntimeDetectorDelegate {

	public static final String DOT_CDK_SUBSCRIPTION_USERNAME = "rhel.subscription.username";
	public static final String DOT_CDK_SUBSCRIPTION_PASSWORD = "rhel.subscription.password";

	@Override
	public RuntimeDefinition getRuntimeDefinition(File root, IProgressMonitor monitor) {
		if (validate(root)) {
			RuntimeDefinition def = createDefinition(getDefinitionName(root), getDefinitionVersion(root),
					getRuntimeDetectionType(), root);
			calculateProblems(def);
			return def;
		}
		return null;
	}

	@Override
	public boolean initializeRuntime(RuntimeDefinition runtimeDefinition) throws CoreException {
		if (!exists(runtimeDefinition) && validate(runtimeDefinition.getLocation())) {
			IServerType st = ServerCore.findServerType(getServerType());
			String possibleId = runtimeDefinition.getName();
			String suffixed = ServerNamingUtility.getDefaultServerName(possibleId);
			try {
				IServerWorkingCopy wc = st.createServer(suffixed, null, new NullProgressMonitor());
				initializeServer(wc, runtimeDefinition);
				wc.setName(suffixed);
				wc.save(true, new NullProgressMonitor());
				return true;
			} catch (CoreException ce) {
				CDKCoreActivator.pluginLog().logError("Error creating runtime", ce);
			}
		}
		return false;
	}

	protected void addToCredentialsModel(String domainString, String user, String password) {
		ICredentialDomain domain = CredentialService.getCredentialModel().getDomain(domainString);
		if (!domain.userExists(user)) {
			if (password == null || password.isEmpty()) {
				CredentialService.getCredentialModel().addPromptedCredentials(domain, user);
			} else {
				CredentialService.getCredentialModel().addCredentials(domain, user, password);
			}
			CredentialService.getCredentialModel().save();
		}
	}

	protected Properties readProperties(File cdkFile) {
		Properties props = new Properties();
		if (cdkFile.exists()) {
			try {
				props.load(new FileInputStream(cdkFile));
			} catch (IOException ioe) {
				// Ignore
			}
		}
		return props;
	}

	@Override
	public boolean exists(RuntimeDefinition runtimeDefinition) {
		if (validate(runtimeDefinition.getLocation())) {
			IServer[] ofType = findServersOfType(getServerType());
			for (int i = 0; i < ofType.length; i++) {
				if (ofType[i] != null && matches(runtimeDefinition, ofType[i])) {
					return true;
				}
			}
		}
		return false;
	}

	protected IServer[] findServersOfType(String type) {
		if (type == null)
			return new IServer[0];
		ArrayList<IServer> ret = new ArrayList<>();
		IServer[] all = ServerCore.getServers();
		for (int i = 0; i < all.length; i++) {
			if (all[i] != null && all[i].getServerType() != null) {
				// all[i] should never be null
				// st will only be null if someone is using a workspace
				// that has a server type that is no longer installed or is missing
				if (type.equals(all[i].getServerType().getId())) {
					ret.add(all[i]);
				}
			}
		}
		return (IServer[]) ret.toArray(new IServer[ret.size()]);
	}

	protected boolean isHomeDirectory(File root) {
		String home = System.getProperty("user.home");
		return new File(home).equals(root);
	}

	protected boolean validate(File root) {
		String[] required = getRequiredChildren();
		for (int i = 0; i < required.length; i++) {
			if (!(new File(root, required[i]).exists())) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Subclasses should override this
	 * 
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.runtime.core.model.AbstractRuntimeDetectorDelegate#
	 * calculateProblems(org.jboss.tools.runtime.core.model.RuntimeDefinition)
	 */
	@Override
	public void calculateProblems(RuntimeDefinition def) {
		super.calculateProblems(def);
	}

	protected abstract boolean matches(RuntimeDefinition def, IServer server);

	protected abstract String getServerType();

	protected abstract String[] getRequiredChildren();

	protected abstract String getDefinitionName(File root);

	protected abstract String getRuntimeDetectionType();

	protected abstract String getDefinitionVersion(File root);

	protected abstract void initializeServer(IServerWorkingCopy wc, RuntimeDefinition runtimeDefinition)
			throws CoreException;

}
