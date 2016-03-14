/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;
import org.jboss.tools.as.core.internal.modules.ModuleDeploymentPrefsUtil;
import org.jboss.tools.as.core.server.controllable.subsystems.internal.ModuleDeployPathController;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;

public class OpenShiftEapDeployPathController extends ModuleDeployPathController implements ISubsystemController {

	public OpenShiftEapDeployPathController() {
		super();
	}
	
	@Override
	protected ModuleDeploymentPrefsUtil createModuleDeploymentPrefsUtil() {
		return new OpenShiftModuleDeploymentPrefsUtil();
	}
	
	public static class OpenShiftModuleDeploymentPrefsUtil extends ModuleDeploymentPrefsUtil {
		// We must override the presumed name given to the module upon publish if it is our magic project
		protected String getOutputNameFromSettings(IServerAttributes server, IModule module) {
			String ret = super.getOutputNameFromSettings(server, module);
			if( ret == null && module.equals(findProjectModule(server))) {
				String suffix = ServerModelUtilities.getDefaultSuffixForModule(module);
				ret = "ROOT" + suffix;
			}
			return ret;
		}
		
		protected IModule findProjectModule(IServerAttributes server) {
			return OpenShiftServerUtils.findProjectModule(OpenShiftServerUtils.getDeployProject(server));
		}
	}
}
