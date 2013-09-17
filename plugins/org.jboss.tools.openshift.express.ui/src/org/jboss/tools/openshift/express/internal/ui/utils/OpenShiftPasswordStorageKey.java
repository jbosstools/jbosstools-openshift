/*******************************************************************************
 * Copyright (c) 2010 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;



/**
 * Implements a key to be used to store values in the preferences store.
 * 
 * @author Andre Dietisheim
 * @deprecated
 */
public class OpenShiftPasswordStorageKey extends org.jboss.tools.openshift.express.internal.core.secure.OpenShiftPasswordStorageKey {

	public OpenShiftPasswordStorageKey(String platform, String userName) {
		super(platform, userName);
	}
}
