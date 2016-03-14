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
package org.jboss.tools.openshift.core.server;

import org.jboss.ide.eclipse.as.wtp.core.server.behavior.CachedPublisherProfileBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;

/**
 * The OpenShiftServerBehavior is an {@link IControllableServerBehavior},
 * which means it pulls its behavior from the various subsystems and profiles
 * declared via xml. 
 * 
 * This server requires caching the Publish controller, since the publish controller maintains some state between calls
 */
public class OpenShiftServerBehaviour extends CachedPublisherProfileBehavior {
	public static final String PROFILE_OPENSHIFT3 = "openshift3";
	public static final String PROFILE_OPENSHIFT3_EAP = "openshift3.eap";
	

}
