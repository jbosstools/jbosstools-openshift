/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.matcher;

import java.util.regex.Pattern;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import com.openshift.restclient.model.IResource;

/**
 * Matcher that matches the name of a given resource against a given regex.
 * 
 * @author adietish@redhat.com
 */
public class ResourceNameMatcher extends BaseMatcher<String> {

	private Pattern pattern;
	
	public ResourceNameMatcher(String regex) {
		this.pattern = Pattern.compile(regex);
	}
	
	@Override
	public boolean matches(Object o) {
		if (!(o instanceof IResource)) {
			return false;
		}
		IResource resource = (IResource) o;
		return matches(resource.getName());
	}
	
	public boolean matches(String text) {
		return pattern.matcher(text).find();
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("resource name matches " + pattern.pattern());
	}	
}
