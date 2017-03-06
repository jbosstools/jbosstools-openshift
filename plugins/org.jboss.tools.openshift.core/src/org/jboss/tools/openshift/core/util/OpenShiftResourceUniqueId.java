/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat Inc..
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

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftResourceUniqueId {

	private static final char UNIQUE_ID_PROJECT_NAME_DELIMITER = '@';

	public static String get(IResource resource) {
		if (resource == null || StringUtils.isEmpty(resource.getName()) || resource.getProject() == null
				|| StringUtils.isEmpty(resource.getProject().getName())) {
			return null;
		}

		if (ResourceKind.SERVICE.equals(resource.getKind())) {
	        return new StringBuilder().append(resource.getProject().getName()).append(UNIQUE_ID_PROJECT_NAME_DELIMITER)
                    .append(resource.getName()).toString();   
		} else {
	        return new StringBuilder().append(resource.getProject().getName()).append(UNIQUE_ID_PROJECT_NAME_DELIMITER)
                    .append(resource.getKind()).append(UNIQUE_ID_PROJECT_NAME_DELIMITER)
                    .append(resource.getName()).toString();   
		}
	}

	/**
	 * Returns the project name for a given resource unique id
	 * 
	 * @param uniqueId
	 * @return
	 */
	public static String getProjectName(String uniqueId) {
		if (StringUtils.isEmpty(uniqueId)) {
			return null;
		}

		int index = uniqueId.indexOf(UNIQUE_ID_PROJECT_NAME_DELIMITER);
		if (index == -1) {
			return null;
		}
		return uniqueId.substring(0, index);
	}

	/**
	 * Returns the resource name for a given resource uniqueId
	 * 
	 * @param uniqueId
	 * @return
	 */
	public static String getResourceName(String uniqueId) {
		if (StringUtils.isEmpty(uniqueId)) {
			return null;
		}
        String[] comps = uniqueId.split(String.valueOf(UNIQUE_ID_PROJECT_NAME_DELIMITER));
        if (comps.length > 0) {
            return comps[comps.length - 1];
        } else {
            return null;
        }
	}

	/**
	 * Return the OpenShift resource kind associated with this resource id. If an old
	 * resource id is given, then Service is assumed.
	 * 
	 * @param uniqueId the OpenShift resource id
	 * @return the kind of the OpenShift resource
	 */
    public static String getKind(String uniqueId) {
        if (StringUtils.isEmpty(uniqueId)) {
            return null;
        }
        String[] comps = uniqueId.split(String.valueOf(UNIQUE_ID_PROJECT_NAME_DELIMITER));
        if (comps.length == 2) {
            return ResourceKind.SERVICE;
        } else if (comps.length == 3) {
            return comps[1];
        } else {
            return null;
        }
    }
    
	/**
	 * Returns the resource within the given collection of resources that match
	 * the given uniqueId. Returns {@code null} otherwise
	 * 
	 * @param uniqueId
	 *            the unique id to match the resources against
	 * @param resources
	 *            the resources to search
	 * @return
	 */
	public static <R extends IResource> R getByUniqueId(String uniqueId, Collection<R> resources) {
		if (resources == null || resources.isEmpty() || StringUtils.isEmpty(uniqueId)) {
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
