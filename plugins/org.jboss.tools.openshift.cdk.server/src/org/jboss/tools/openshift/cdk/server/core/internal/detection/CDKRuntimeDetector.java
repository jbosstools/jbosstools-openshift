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
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;

public class CDKRuntimeDetector extends AbstractCDKRuntimeDetector {
	
	public static final String CDK_RUNTIME_TYPE = "CDK";
	public static final String DOT_CDK_BOX_VERSION = "cdk.box.version";
	
	@Override
	protected boolean matches(RuntimeDefinition def, IServer server) {
		String s = server.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		if( s != null ) {
			if( new File(s).equals(def.getLocation())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected String getServerType() {
		return CDKServer.CDK_SERVER_TYPE;
	}

	@Override
	protected String[] getRequiredChildren() {
		return new String[]{
				CDKConstants.CDK_RESOURCE_VAGRANTFILE,
				CDKConstants.CDK_RESOURCE_DOTCDK
		};
	}
	

	@Override
	protected String getDefinitionName(File root) {
		return CDKServer.getServerTypeBaseName();

	}
	
	@Override
	protected String getRuntimeDetectionType() {
		return CDK_RUNTIME_TYPE;
	}
	
	@Override
	protected String getDefinitionVersion(File root) {
		File cdkFile = new File(root, CDKConstants.CDK_RESOURCE_DOTCDK);
		Properties props = readProperties(cdkFile);
		String version = props.getProperty(DOT_CDK_BOX_VERSION);
		version = (version == null ? "2.0" : version);
		return version;
	}
	
	@Override
	protected void initializeServer(IServerWorkingCopy wc, RuntimeDefinition runtimeDefinition) throws CoreException {
		String folder = runtimeDefinition.getLocation().getAbsolutePath();
		File cdkFile = new File(folder, CDKConstants.CDK_RESOURCE_DOTCDK);
		Properties props = readProperties(cdkFile);
		String user = props.getProperty(DOT_CDK_SUBSCRIPTION_USERNAME);
		String password = System.getenv(DOT_CDK_SUBSCRIPTION_PASSWORD);
		if( user != null ) {
			addToCredentialsModel(CredentialService.REDHAT_ACCESS, user, password);
		}
		wc.setAttribute(CDKServer.PROP_FOLDER, folder);
		wc.setAttribute(CDKServer.PROP_USERNAME, user);
	}

}
