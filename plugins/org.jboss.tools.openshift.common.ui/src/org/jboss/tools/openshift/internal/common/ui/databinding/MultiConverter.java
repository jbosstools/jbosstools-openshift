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
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.osgi.util.NLS;

/**
 * A converter that invokes several given converters.
 * 
 * @author Andre Dietisheim
 */
public class MultiConverter implements IConverter {

	private IConverter[] converters;

	public MultiConverter(IConverter... converters) {
		validate(converters);
		this.converters = converters;
	}

	private void validate(IConverter[] converters) {
		if (!hasConverters(converters)) {
			return;
		}

		Class<?> fromType = null;
		for (IConverter converter : converters) {
			if (fromType != null) {
				if (converter == null) {
					throw new IllegalArgumentException("Null converter given.");
				}
				if (converter.getFromType() instanceof Class) {
					throw new IllegalArgumentException(
							NLS.bind("Converter {0} is not set to convert from a class (type).", converter.getClass()));
				}
				if (!fromType.isAssignableFrom((Class<?>) converter.getFromType())) {
					throw new IllegalArgumentException(
							NLS.bind(
									"Converter {0} does not match preceeding converter. It expects {1} and precedessor converts to {2}",
									new Object[] { converter.getClass(), converter.getFromType(), fromType }));
				}
				fromType = (Class<?>) converter.getToType();
			}
		}

	}

	@Override
	public Object convert(Object fromObject) {
		for (IConverter converter : converters) {
			fromObject = converter.convert(fromObject);
		}
		return fromObject;
	}

	@Override
	public Object getFromType() {
		if (!hasConverters()) {
			return Object.class;
		}
		return converters[0].getFromType();
	}

	@Override
	public Object getToType() {
		if (!hasConverters()) {
			return Object.class;
		}
		return converters[converters.length - 1].getToType();
	}

	private boolean hasConverters() {
		return hasConverters(converters);
	}

	private boolean hasConverters(IConverter[] converters) {
		return converters != null
				&& converters.length > 0;
	}

}
