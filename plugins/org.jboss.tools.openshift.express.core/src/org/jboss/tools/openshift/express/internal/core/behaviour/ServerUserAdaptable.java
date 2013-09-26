/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A marker (adaptable) that indicates the WTP server publishing that a user
 * triggered publishing is requested
 * 
 * @author Andre Dietisheim
 */
public class ServerUserAdaptable implements IAdaptable {

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (String.class.equals(adapter)) {
			return "user";
		}
		return null;
	}
}