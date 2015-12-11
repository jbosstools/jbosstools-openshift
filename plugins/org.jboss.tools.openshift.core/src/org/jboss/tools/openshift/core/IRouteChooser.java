/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core;

import java.util.List;

import com.openshift.restclient.model.route.IRoute;

/**
 * UI that allows core components to let the user choose among different routes
 * 
 * @author Andre Dietisheim
 */
public interface IRouteChooser {

	public IRoute chooseRoute(List<IRoute> routes);
	public void noRouteErrorDialog();

}
