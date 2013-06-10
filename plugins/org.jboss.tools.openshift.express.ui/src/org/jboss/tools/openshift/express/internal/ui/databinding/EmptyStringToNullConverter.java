/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.databinding;

import org.eclipse.core.databinding.conversion.Converter;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

/**
 * A converter that turns empty strings to null. Leaves them untouched
 * otherwise.
 * 
 * @author Andre Dietisheim
 */
public class EmptyStringToNullConverter extends Converter {

	public EmptyStringToNullConverter() {
		super(String.class, String.class);
	}

	@Override
	public Object convert(Object fromObject) {
		if (!(fromObject instanceof String)
				|| StringUtils.isEmpty((String) fromObject)) {
			return null;
		} else {
			return fromObject;
		}
	}
}
