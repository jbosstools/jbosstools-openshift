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
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.conversion.Converter;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * A converter that <code>null</code> or emtpy string to <code>false</code>.
 * 
 * @author Andre Dietisheim
 */
public class IsNotEmptyString2BooleanConverter extends Converter {

	public IsNotEmptyString2BooleanConverter() {
		super(String.class, Boolean.class);
	}

	@Override
	public Object convert(Object fromObject) {
		if(!(fromObject instanceof String)) {
			return Boolean.FALSE;
		} 
		return !StringUtils.isEmpty((String) fromObject) ;
	}
}
