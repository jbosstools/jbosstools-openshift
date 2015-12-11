/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerExtendedProperties extends ServerExtendedProperties {

	public OpenShiftServerExtendedProperties(IAdaptable adaptable) {
		super(adaptable);
	}

	@Override
	public boolean allowConvenienceEnhancements() {
		return false;
	}

	@Override
	public boolean hasWelcomePage() {
		// return getWelcomePageUrl() != null;
		return false;
    }

	@Override
    public String getWelcomePageUrl() {
		// checked when unfolding the context menu, cant do remoting
		// org.jboss.ide.eclipse.as.ui.views.server.extensions.ShowInWelcomePageActionProvider#hasURL -> #getUrl -> props.getWelcomePageUrl()
		
//        IService service = OpenShiftServerUtils.getService(server);
//        if (service != null) {
//            IProject project = service.getProject();
//            if (project != null) {
//                List<IRoute> routes = project.getResources(ResourceKind.ROUTE);
//                if (routes != null && !routes.isEmpty()) {
//                    //if more than 1 route, well, we're not in the UI layer here, 
//                    //so can't let the user select the url to open, tough luck. 
//                    //So we pick the 1st one we find
//                    return routes.get(0).getURL();
//                }
//            }
//        }
        return null;
    }


}
