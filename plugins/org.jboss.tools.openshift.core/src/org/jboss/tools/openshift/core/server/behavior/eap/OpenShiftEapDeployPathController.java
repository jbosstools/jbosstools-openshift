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
package org.jboss.tools.openshift.core.server.behavior.eap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.jst.j2ee.internal.deployables.J2EEDeployableFactory;
import org.eclipse.jst.jee.internal.deployables.JEEDeployableFactory;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModule2;
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
		@Override
		protected String getOutputNameFromSettings(IServerAttributes server, IModule module) {
		    try {
		        Method clearCacheMethod = J2EEDeployableFactory.class.getDeclaredMethod("clearCache", IProject.class);
		        clearCacheMethod.setAccessible(true);
		        clearCacheMethod.invoke(JEEDeployableFactory.jeeInstance(), getEclipseProject(server));
            } catch (IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			String ret = super.getOutputNameFromSettings(server, module);
			if (ret == null && module.equals(findProjectModule(server))) {
				String suffix = ServerModelUtilities.getDefaultSuffixForModule(module);
				String prefix = "";
				if (module instanceof IModule2) {
				    prefix = ((IModule2)module).getProperty(IModule2.PROP_DEPLOY_NAME);
		        }
		        // Otherwise use the module's name
		        if( prefix == null ) {
		            prefix = module.getName();
		        }
				ret = prefix + suffix;
			}
			return ret;
		}

		protected IModule findProjectModule(IServerAttributes server) {
			return OpenShiftServerUtils.findProjectModule(getEclipseProject(server));
		}
		
		private IProject getEclipseProject(IServerAttributes server) {
		    return OpenShiftServerUtils.getDeployProject(server);
		}
	}
}
