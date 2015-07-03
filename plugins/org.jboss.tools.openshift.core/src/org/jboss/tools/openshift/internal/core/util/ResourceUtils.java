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
package org.jboss.tools.openshift.internal.core.util;

import org.apache.commons.lang.StringUtils;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
import com.openshift.restclient.model.IResource;

public class ResourceUtils {

	/**
	 * Returns <code>true</code> if the given resource contains the given text
	 * in name or tags.
	 * 
	 * @param filterText
	 * @param template
	 * @return
	 */
	public static boolean isMatching(final String filterText, IResource template) {
		if (StringUtils.isBlank(filterText)) {
			return true;
		}
		if (template.getName().contains(filterText)) {
			return true;
		}
		return template.accept(new CapabilityVisitor<ITags, Boolean>() {
			@Override
			public Boolean visit(ITags capability) {
				for (String tag : capability.getTags()) {
					if (tag.contains(filterText)) {
						return true;
					}
				}
				return false;
			}
		}, Boolean.FALSE);
	}	
}
