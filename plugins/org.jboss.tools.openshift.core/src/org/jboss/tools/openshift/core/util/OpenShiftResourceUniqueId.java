/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.util;

import java.util.Collection;

import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftResourceUniqueId {
	
	private static final char UNIQUE_ID_PROJECT_NAME_DELIMITER = '@';
	
	public static String get(IResource resource) {
		return new StringBuilder()
			.append(resource.getProject().getName())
			.append(UNIQUE_ID_PROJECT_NAME_DELIMITER)
			.append(resource.getName())
			.toString();
	}

	public static String getProject(String uniqueId) {
		if (StringUtils.isEmpty(uniqueId)) {
			return null;
		}
		int index = uniqueId.indexOf(UNIQUE_ID_PROJECT_NAME_DELIMITER);
		if (index == -1) {
			return null;
		}
		return uniqueId.substring(0, index);
	}
	
	public static String getName(String uniqueId) {
		if (StringUtils.isEmpty(uniqueId)) {
			return null;
		}
		int index = uniqueId.indexOf(UNIQUE_ID_PROJECT_NAME_DELIMITER);
		if (index == -1) {
			return null;
		}
		return uniqueId.substring(index);
	}

	public static <R extends IResource> R getByUniqueId(String uniqueId, Collection<R> resources) {
		if (resources == null
				|| resources.isEmpty()
				|| StringUtils.isEmpty(uniqueId)) {
			return null;
		}
		
		for (R resource : resources) {
			if (uniqueId.equals(get(resource))) {
				return resource;
			}
		}
		return null;
	}
}
