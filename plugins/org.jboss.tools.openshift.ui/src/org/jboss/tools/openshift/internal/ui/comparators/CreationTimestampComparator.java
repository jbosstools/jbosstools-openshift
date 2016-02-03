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

import org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

/**
 * Comparator for sorting display models by resource createion timestamp
 * @author jeff.cantrill
 *
 */
public class CreationTimestampComparator implements Comparator<IResourceUIModel>{

	@Override
	public int compare(IResourceUIModel o1, IResourceUIModel o2) {
		try {
			return -1 * DateTimeUtils.parse(o1.getResource().getCreationTimeStamp())
					.compareTo(DateTimeUtils.parse(o2.getResource().getCreationTimeStamp()));
		} catch (ParseException e) {
		}
		return 0;
	}

}
