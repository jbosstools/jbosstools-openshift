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
package org.jboss.tools.openshift.internal.ui.comparators;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;

import org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils;
import org.jboss.tools.openshift.internal.ui.models2.AbstractResourceWrapper;

/**
 * Comparator for sorting display models by resource creation timestamp
 * @author jeff.cantrill
 *
 */
public class CreationTimestampComparator implements Comparator<AbstractResourceWrapper<?, ?>>{

	@Override
	public int compare(AbstractResourceWrapper<?, ?> o1, AbstractResourceWrapper<?, ?> o2) {
		Date date1 = getDate(o1);
		Date date2 = getDate(o2);
		if(date1 == null || date2 == null) {
			//invalid date goes to the end of list.
			if(date1 != null) {
				return -1;
			} else if(date2 != null) {
				return 1;
			} else {
				return 0;
			}
		}
		return -1 * date1.compareTo(date2);
	}

	private Date getDate(AbstractResourceWrapper<?, ?> o1) {
		String value = o1.getResource().getCreationTimeStamp();
		try {
			return DateTimeUtils.parse(value);
		} catch (ParseException e) {
			return null;
		}
	}

}
