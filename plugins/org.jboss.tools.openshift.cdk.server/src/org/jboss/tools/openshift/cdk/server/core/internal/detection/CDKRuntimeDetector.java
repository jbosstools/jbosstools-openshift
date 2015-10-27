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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.core.util.ServerNamingUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.runtime.core.model.AbstractRuntimeDetectorDelegate;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;

public class CDKRuntimeDetector extends AbstractRuntimeDetectorDelegate{
	public static final String CDK_RUNTIME_TYPE = "CDK";
	@Override
	public RuntimeDefinition getRuntimeDefinition(File root,
			IProgressMonitor monitor) {
		if( validate(root)) {
			return new RuntimeDefinition(root.getName(), "1.0", CDK_RUNTIME_TYPE, root);
		}
		return null;
	}
	
	public boolean initializeRuntime(RuntimeDefinition runtimeDefinition) throws CoreException {
		if( !exists(runtimeDefinition)) {
			IServerType st = ServerCore.findServerType(CDKServer.CDK_SERVER_TYPE);
			String possibleId = runtimeDefinition.getLocation().getName() + " CDK Server";
			String suffixed = ServerNamingUtility.getDefaultServerName(possibleId);
			try {
				IServerWorkingCopy wc = st.createServer(suffixed, null, new NullProgressMonitor());
				wc.setAttribute(CDKServer.PROP_FOLDER, runtimeDefinition.getLocation().getAbsolutePath());
				wc.setName(suffixed);
				IServer s = wc.save(true, new NullProgressMonitor());
				return true;
			} catch(CoreException ce) {
				
			}
		}
		return false;
	}
	
	public boolean exists(RuntimeDefinition runtimeDefinition) {
		if( validate(runtimeDefinition.getLocation()) ) {
			IServer[] all = ServerCore.getServers();
			for( int i = 0; i < all.length; i++ ) {
				if( all[i].getServerType().getId().equals(CDKServer.CDK_SERVER_TYPE)) {
					String s = all[i].getAttribute(CDKServer.PROP_FOLDER, (String)null);
					if( s != null ) {
						if( new File(s).equals(runtimeDefinition.getLocation())) {
							return true;
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
