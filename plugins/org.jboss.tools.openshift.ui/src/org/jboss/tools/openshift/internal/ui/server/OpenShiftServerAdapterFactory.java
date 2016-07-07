/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;
import org.jboss.tools.openshift.core.server.OpenShiftServer;

public class OpenShiftServerAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof IServer && IServerModule.class.equals(adapterType)) {
			IServer server = (IServer)adaptableObject;
			OpenShiftServer oss = (OpenShiftServer)server.loadAdapter(OpenShiftServer.class, new NullProgressMonitor());
			if (oss != null) {
				return (T) new OpenShiftServerModuleAdapter(server);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] {IServerModule.class};
	}

	private static class OpenShiftServerModuleAdapter implements IServerModule {

		private IServer server;
		private RootModule module;

		private OpenShiftServerModuleAdapter(IServer server) {
			this.server = server;
			module = new RootModule(server.toString(), "");//TODO we need to be able to determine the context root
		}

		@Override
		public IServer getServer() {
			return server;
		}

		@Override
		public IModule[] getModule() {
			return new IModule[]{module};
		}

	}

	//TODO we're not supposed to implement IModule. That'll do until we find a better way to integrate with LiveReload
	private static class RootModule implements IModule {

		private String id;

		private RootModule(String id, String contextRoot) {
			this.id = id;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getName() {
			return "";
		}

		@Override
		public IModuleType getModuleType() {
			return null;
		}

		@Override
		public IProject getProject() {
			return null;
		}

		@Override
		public boolean isExternal() {
			return false;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public Object getAdapter(Class adapter) {
			if(adapter == IWebModule.class) {
				return new RootWebModule();
			}
			return null;
		}

		@Override
		public Object loadAdapter(Class adapter, IProgressMonitor monitor) {
			return getAdapter(adapter);
		}

	}
	
	private static class RootWebModule implements IWebModule {

		@Override
		public IContainer[] getResourceFolders() {
			return null;
		}

		@Override
		public IContainer[] getJavaOutputFolders() {
			return null;
		}

		@Override
		public boolean isBinary() {
			return false;
		}

		@Override
		public String getContextRoot() {
			return null;
		}

		@Override
		public String getContextRoot(IModule earModule) {
			return null;
		}

		@Override
		public IModule[] getModules() {
			return null;
		}

		@Override
		public String getURI(IModule module) {
			return null;
		}
		
	}

}
