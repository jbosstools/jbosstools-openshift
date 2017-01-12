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
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.runtime.core.model.AbstractRuntimeDetectorDelegate;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;

public class CDKRuntimeDetector extends AbstractRuntimeDetectorDelegate{
	public static final String CDK_RUNTIME_TYPE = "CDK";
	
	public static final String DOT_CDK_SUBSCRIPTION_USERNAME = "rhel.subscription.username";
	public static final String DOT_CDK_SUBSCRIPTION_PASSWORD = "rhel.subscription.password";
	public static final String DOT_CDK_BOX_VERSION = "cdk.box.version";
	
	@Override
	public RuntimeDefinition getRuntimeDefinition(File root,
			IProgressMonitor monitor) {
		if( validate(root)) {
			File cdkFile = new File(root, CDKConstants.CDK_RESOURCE_DOTCDK);
			Properties props = loadCDKProperties(cdkFile);
			String version = props.getProperty(DOT_CDK_BOX_VERSION);
			version = (version == null ? "2.0" : version);
			return createDefinition("Container Development Environment", version, CDK_RUNTIME_TYPE, root);
		}
		return null;
	}
	
	@Override
	public boolean initializeRuntime(RuntimeDefinition runtimeDefinition) throws CoreException {
		if( !exists(runtimeDefinition) && validate(runtimeDefinition.getLocation())) {
			IServerType st = ServerCore.findServerType(CDKServer.CDK_SERVER_TYPE);
			String possibleId = runtimeDefinition.getName();
			String suffixed = ServerNamingUtility.getDefaultServerName(possibleId);
			try {
				IServerWorkingCopy wc = st.createServer(suffixed, null, new NullProgressMonitor());
				String folder = runtimeDefinition.getLocation().getAbsolutePath();
				File cdkFile = new File(folder, CDKConstants.CDK_RESOURCE_DOTCDK);
				Properties props = loadCDKProperties(cdkFile);
				String val = props.getProperty(DOT_CDK_SUBSCRIPTION_USERNAME);
				if( val != null ) {
					ICredentialDomain domain = CredentialService.getCredentialModel().getDomain(CredentialService.REDHAT_ACCESS);
					String password = System.getenv(DOT_CDK_SUBSCRIPTION_PASSWORD);
					if( !domain.userExists(val)) {
						if( password == null || password.isEmpty()) {
							CredentialService.getCredentialModel().addPromptedCredentials(domain, val);
						} else {
							CredentialService.getCredentialModel().addCredentials(domain, val, password);
						}
						CredentialService.getCredentialModel().save();
					}
				}
				wc.setAttribute(CDKServer.PROP_FOLDER, folder);
				wc.setAttribute(CDKServer.PROP_USERNAME, val);
				wc.setName(suffixed);
				IServer s = wc.save(true, new NullProgressMonitor());
				return true;
			} catch(CoreException ce) {
				
			}
		}
		return false;
	}
	
	private Properties loadCDKProperties(File cdkFile) {
		Properties props = new Properties();
		if( cdkFile.exists()) {
			try {
				props.load(new FileInputStream(cdkFile));
			} catch(IOException ioe) {
				// Ignore
			}
		}
		return props;
	}
	
	@Override
	public boolean exists(RuntimeDefinition runtimeDefinition) {
		if( validate(runtimeDefinition.getLocation()) ) {
			IServer[] all = ServerCore.getServers();
			for( int i = 0; i < all.length; i++ ) {
				if( all[i] != null ) {
					// all[i] should never be null
					IServerType st = all[i].getServerType();
					if( st != null ) {
						// st will only be null if someone is using a workspace 
						// that has a server type that is no longer installed or is missing.
						if( CDKServer.CDK_SERVER_TYPE.equals(st.getId())) {
							String s = all[i].getAttribute(CDKServer.PROP_FOLDER, (String)null);
							if( s != null ) {
								if( new File(s).equals(runtimeDefinition.getLocation())) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean validate(File root) {
		if( new File(root, CDKConstants.CDK_RESOURCE_VAGRANTFILE).exists()) {
			if( new File(root, CDKConstants.CDK_RESOURCE_DOTCDK).exists()) {
				return true;
			}
		}
		return false;
	}
}
