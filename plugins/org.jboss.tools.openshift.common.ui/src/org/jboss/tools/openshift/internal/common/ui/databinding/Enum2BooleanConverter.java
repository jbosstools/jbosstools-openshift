/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.conversion.Converter;

/**
 * @author Jeff Maury
 */
public class Enum2BooleanConverter extends Converter {

    private Enum trueValue;
    
	public Enum2BooleanConverter(Enum trueValue) {
		super(trueValue.getClass(), Boolean.class);
		this.trueValue = trueValue;
	}

	@Override
	public Object convert(Object fromObject) {
		if (!(fromObject instanceof Enum)) {
			return fromObject;
		}
		return fromObject == trueValue;
	}

}
