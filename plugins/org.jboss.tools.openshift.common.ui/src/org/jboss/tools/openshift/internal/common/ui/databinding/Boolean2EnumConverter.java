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
public class Boolean2EnumConverter extends Converter {

    private Enum trueValue;
    
	public Boolean2EnumConverter(Enum trueValue) {
		super(Boolean.class, trueValue.getClass());
		this.trueValue = trueValue;
	}

	@Override
	public Object convert(Object fromObject) {
		if (!(fromObject instanceof Boolean)) {
			return fromObject;
		}
		return ((Boolean)fromObject)?trueValue:null;
	}

}
