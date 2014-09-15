/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.filters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.IActionFilter;

/**
 * SimplePropertyActionFilter is an IActionFilter implementation that uses
 * reflection to test if a target object has a property with a value that
 * matches the desired value.
 * 
 * @author Jeff Cantrill
 */
public class SimplePropertyActionFilter implements IActionFilter {

	private static final int NAME_CAPTURE_GROUP = 2;
	private final Pattern simpleAccessorPattern = Pattern.compile("(get|is)([a-zA-Z_0-9]*)");

	@Override
	public boolean testAttribute(Object target, String name, String desiredValue) {
		if (target == null) {
			return false;
		}
		Method accessor = findAttribute(target, name);
		if (accessor == null)
		{
			return false;
		}
		try {
			Object value = accessor.invoke(target, new Object[] {});
			return value != null && desiredValue.equals(value.toString());
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		} catch (InvocationTargetException e) {
		}
		return false;
	}

	private Method findAttribute(Object target, String name) {
		Method[] methods = target.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (isAccessorFor(name, methods[i].getName())) {
				return methods[i];
			}
		}
		return null;
	}

	private boolean isAccessorFor(String property, String accessorName) {
		Matcher matcher = simpleAccessorPattern.matcher(accessorName);
		if (matcher.lookingAt()) {
			return property.equals(StringUtils.uncapitalize(matcher.group(NAME_CAPTURE_GROUP)));
		}
		return false;
	}

}
