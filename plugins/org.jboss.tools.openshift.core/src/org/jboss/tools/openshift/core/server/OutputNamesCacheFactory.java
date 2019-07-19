/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.core.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.as.core.internal.modules.ModuleDeploymentPrefsUtil;

public class OutputNamesCacheFactory {

	public static final OutputNamesCacheFactory INSTANCE = new OutputNamesCacheFactory();

	private Map<String, OutputNamesCache> caches;

	private OutputNamesCacheFactory() {
		this(new HashMap<>());
	}

	/* for testing purposes */
	protected OutputNamesCacheFactory(Map<String, OutputNamesCache> caches) {
		this.caches = caches;
	}

	public OutputNamesCache get(IServer server) {
		OutputNamesCache cache = caches.get(server.getId());
		if (cache == null) {
			cache = createCache(server);
			onServerDeleted(server);
		}
		return cache;
	}

	private OutputNamesCache createCache(IServer server) {
		OutputNamesCache cache;
		cache = new OutputNamesCache(server);
		caches.put(server.getId(), cache);
		return cache;
	}

	private void onServerDeleted(final IServer cachedServer) {
		ServerCore.addServerLifecycleListener(new IServerLifecycleListener() {

			@Override
			public void serverAdded(IServer server) {
				// NOP
			}

			@Override
			public void serverChanged(IServer server) {
				// NOP
			}

			@Override
			public void serverRemoved(IServer server) {
				if (!cachedServer.equals(server)) {
					return;
				}
				caches.remove(server.getId());
				ServerCore.removeServerLifecycleListener(this);
			}
			
		});
	}

	public static class OutputNamesCache {

		protected Map<String, String> oldOutputNames = new HashMap<>();
		protected Map<String, String> newOutputNames = new HashMap<>();
		protected IServer server;

		/* for testing purposes */
		protected OutputNamesCache(IServer server) {
			this.server = server;
		}

		public void collect() {
			this.oldOutputNames = getModuleOutputNames(server);
		}

		public void onModified(IServerAttributes server) {
			this.newOutputNames = getModuleOutputNames(server);
		}

		/**
		 * Returns the output names for a given server or working copy.
		 * 
		 * @param server
		 * @return
		 */
		private Map<String, String> getModuleOutputNames(final IServerAttributes server) {
			return Arrays.stream(server.getModules()).collect(Collectors.toMap(
					this::getModuleKey,
					module -> getOutputName(module, server)));
		}

		public boolean isModified(IModule module) {
			if (module == null) {
				return false;
			}
			return newOutputNames.containsKey(getModuleKey(module))
					&& !Objects.equals(newOutputNames.get(getModuleKey(module)), oldOutputNames.get(getModuleKey(module)));
		}

		protected String getOutputName(IModule module, IServerAttributes server) {
			// Get the full path of where this module would be deployed to, and just take the last segment
			// IF the array is of size 1, it will pull from prefs.
			// If the array is larger, it will pull from the relative path of the child to its parent module
			IPath path = new ModuleDeploymentPrefsUtil().getModuleNestedDeployPath(new IModule[] { module }, "/", server); //$NON-NLS-1$
			return path.lastSegment();
		}

		public String getOldOutputName(IModule module) {
			if (module == null) {
				return null;
			}
			return oldOutputNames.get(getModuleKey(module));
		}

		private String getModuleKey(IModule module) {
			if (module == null) {
				return null;
			}
			return module.getId();
		}

		public void reset(IModule module) {
			oldOutputNames.put(getModuleKey(module), newOutputNames.get(getModuleKey(module)));
		}
	}
}
