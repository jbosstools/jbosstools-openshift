/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.archives.webtools.modules.LocalZippedPublisherUtil;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.IDeployableServerBehaviour;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;

/**
 * @author Rob Stryker
 */
public class OpenShiftServerBinaryPublishMethod extends OpenShiftServerPublishMethod {

	@Override
	public int publishFinish(IDeployableServerBehaviour behaviour,
			IProgressMonitor monitor) throws CoreException {

		String outProject = OpenShiftServerUtils.getExpressDeployProject(behaviour.getServer());
		if( outProject != null ) {
			final IProject destProj = ResourcesPlugin.getWorkspace().getRoot().getProject(outProject);
			if( destProj.exists() ) {
				refreshProject(destProj,new NullProgressMonitor());
				commitAndPushProject(destProj, behaviour, monitor);
			}
		}
		
        return areAllPublished(behaviour) ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;	
	}
	
	@Override
	public int publishModule(IDeployableServerBehaviour behaviour, int kind,
			int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		if( module.length > 1 )
			return 0;
		
		IDeployableServer depServ = ServerConverter.getDeployableServer(behaviour.getServer());
		IPath dest = depServ.getDeploymentLocation(module, false); 
		
		if( module.length == 0 ) return IServer.PUBLISH_STATE_NONE;
		int modulePublishState = behaviour.getServer().getModulePublishState(module);
		int publishType = behaviour.getPublishType(kind, deltaKind, modulePublishState);

		IModuleResourceDelta[] delta = new IModuleResourceDelta[]{};
		if( deltaKind != ServerBehaviourDelegate.REMOVED)
			delta = behaviour.getPublishedResourceDelta(module);

		
		LocalZippedPublisherUtil util = new LocalZippedPublisherUtil();
		IStatus status = util.publishModule(behaviour.getServer(), dest.toString(), module, publishType, delta, monitor);
		monitor.done();
		return 0;
	}

	@Override
	public String getPublishDefaultRootFolder(IServer server) {
		IDeployableServer s = ServerConverter.getDeployableServer(server);
		return s.getDeployFolder();
	}

}
