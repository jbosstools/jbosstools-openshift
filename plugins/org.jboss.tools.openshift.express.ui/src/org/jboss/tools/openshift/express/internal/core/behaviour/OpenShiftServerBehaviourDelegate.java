/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.jboss.ide.eclipse.as.core.server.internal.AbstractBehaviourDelegate;

/**
 * @author Rob Stryker
 */
public class OpenShiftServerBehaviourDelegate extends AbstractBehaviourDelegate {
	
	public static final String OPENSHIFT_ID = "openshift";
	
	@Override
	public String getBehaviourTypeId() {
		return OPENSHIFT_ID;
	}
}
