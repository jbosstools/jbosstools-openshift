/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;

/**
 * A converter used when a single model value is handled by 2 different UI elements.
 * 
 */
public class AggregatingConverter extends Converter {

	private IObservableValue peer;
	private boolean append;

	/**
	 * Constructor.
	 * 
	 * @param peer the associated observable value (UI element)
	 * @param append if true append the associated otherwhise insert in from
	 */
	public AggregatingConverter(IObservableValue peer, boolean append) {
		super(String.class, String.class);
		this.peer = peer;
		this.append = append;
	}

	@Override
	public Object convert(Object fromObject) {
		String str = (String) fromObject;
		String peerValue = (String) peer.getValue();
		if (append) {
			if (StringUtils.isNotBlank(str)) {
				str = str + peerValue;
			}
		} else if (StringUtils.isNotBlank(peerValue)) {
			str = peerValue + str;
		}
		return str;
	}
}