/*******************************************************************************
 * Copyright (c) 2016-2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.eap;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModule2;
import org.eclipse.wst.server.core.IServerAttributes;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;
import org.jboss.tools.as.core.internal.modules.ModuleDeploymentPrefsUtil;
import org.jboss.tools.as.core.server.controllable.subsystems.internal.ModuleDeployPathController;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;

public class OpenShiftEapDeployPathController extends ModuleDeployPathController implements ISubsystemController {

	@Override
	protected ModuleDeploymentPrefsUtil createModuleDeploymentPrefsUtil() {
		return new OpenShiftModuleDeploymentPrefsUtil();
	}

	public static class OpenShiftModuleDeploymentPrefsUtil extends ModuleDeploymentPrefsUtil {
		// We must override the presumed name given to the module upon publish if it is our magic project
		@Override
		protected String getOutputNameFromSettings(IServerAttributes server, IModule module) {
			String ret = super.getOutputNameFromSettings(server, module);
			if (ret == null 
					&& module.equals(findProjectModule(server))) {
				String suffix = ServerModelUtilities.getDefaultSuffixForModule(module);
				String deployName = getDeployName(module);
				ret = deployName + suffix;
			}
			return ret;
		}

		private String getDeployName(IModule module) {
			String prefix = "";
			if (module instanceof IModule2) {
				prefix = ((IModule2) module).getProperty(IModule2.PROP_DEPLOY_NAME);
			}
			// Otherwise use the module name
			if (prefix == null) {
			    prefix = module.getName();
			}
			return prefix;
		}

		protected IModule findProjectModule(IServerAttributes server) {
			return OpenShiftServerUtils.findProjectModule(OpenShiftServerUtils.getDeployProject(server));
		}
	}
}
