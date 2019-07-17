/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.core.server.IServerWorkingCopyProvider;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentPage;
import org.jboss.ide.eclipse.as.ui.editor.ModuleDeploymentOptionsComposite;
import org.jboss.tools.as.core.internal.modules.DeploymentPreferencesLoader;

@SuppressWarnings("restriction")
public class OpenShiftDeploymentPage extends DeploymentPage implements IServerWorkingCopyProvider {

	@Override
	public void createPartControl(Composite parent) {
		this.preferences = DeploymentPreferencesLoader.loadPreferencesFromServer(getServer());

		new ModuleDeploymentOptionsComposite(parent, this, getFormToolkit(parent.getDisplay()), getPreferences()) {

			@Override
			protected boolean showTemporaryColumn() {
				return false;
			}

			@Override
			protected String getDefaultDeploymentTypeFilter() {
				return DEPLOYED;
			}
		};
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);

		/**
		 * https://issues.jboss.org/browse/JBIDE-26744: triggering a full re-deploy so
		 * that change in deployment name results in new artifacts to rsync to the pod.
		 * Without it, nothing new shows up in the temp directory and rsync ends up
		 * syncing nothing.
		 */ 
		Server server = (Server) getServer().getOriginal();
		setPublishState(IServer.PUBLISH_STATE_FULL, server.getModules(), server);
	}
	
	private void setPublishState(int state, IModule[] modules, Server server) {
		Arrays.stream(modules).forEach(module -> 
			server.setModulePublishState(new IModule[] { module }, state));
	}
	
}