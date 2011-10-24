/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.common;

/**
 * @author André Dietisheim
 */
public class StringUtils {

	public static String null2emptyString(String value) {
		if (value != null) {
			return value;
		}
		return "";
	}

	public static boolean isEmpty(String value) {
		return value == null
				|| value.length() == 0;
	}

}
