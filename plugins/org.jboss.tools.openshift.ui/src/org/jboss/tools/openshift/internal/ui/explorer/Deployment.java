/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.explorer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * A deployment is the collection of resources
 * that makes up an 'application'
 * 
 * @author jeff.cantrill
 *
 */
public class Deployment {

	private IService service;
	private Set<IRoute> routes;
	private Set<IPod> pods;
	
	public Deployment(IService service, Collection<IRoute> routes, Collection<IPod> pods) {
		this.service = service;
		this.pods = new HashSet<>(pods);
		this.routes = new HashSet<>(routes);
	}
	
	public Collection<IPod> getPods() {
		return Collections.unmodifiableSet(pods);
	}
	public Collection<IRoute> getRoutes() {
		return Collections.unmodifiableSet(this.routes);
	}
	public IService getService() {
		return this.service;
	}

	public void add(IPod pod) {
		this.pods.add(pod);
	}

	public void remove(IPod pod) {
		this.pods.remove(pod);
	}
	
}