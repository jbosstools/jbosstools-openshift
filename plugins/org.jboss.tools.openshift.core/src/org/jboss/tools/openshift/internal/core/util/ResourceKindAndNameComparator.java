/*******************************************************************************
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.util;

import java.util.Comparator;

import org.apache.commons.lang.ObjectUtils;

import com.openshift.restclient.model.IResource;

public class ResourceKindAndNameComparator implements Comparator<IResource> {
	@Override
	public int compare(IResource first, IResource second) {
		int result = compareKind(first, second);
		if (result != 0) {
			return result;
		}
		return compareName(first, second);
	}

	private int compareName(IResource first, IResource second) {
		return first.getName().compareTo(second.getName());
	}

	private int compareKind(IResource first, IResource second) {
		return ObjectUtils.compare(first.getKind(), second.getKind());
	}
}