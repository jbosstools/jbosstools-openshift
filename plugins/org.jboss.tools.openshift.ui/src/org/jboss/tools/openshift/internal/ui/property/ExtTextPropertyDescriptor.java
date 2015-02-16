/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.eclipse.ui.views.properties.TextPropertyDescriptor;

/**
 * Extended implementation to facilitate
 * setting the property category
 */
public class ExtTextPropertyDescriptor extends TextPropertyDescriptor {

	/**
	 * Create a TextPropertyDescriptor defaulting the displayName
	 * to the id.ToString()
	 * @param id
	 * @param category
	 */
	public ExtTextPropertyDescriptor(Enum<?> id, String category) {
		this(id, id.toString(), category);
	}

	public ExtTextPropertyDescriptor(Object id, String displayName, String category) {
		super(id, displayName);
		setCategory(category);
	}

}
