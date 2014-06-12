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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

/**
 * @author André Dietisheim
 */
public class StyleRangeUtils {


	public static StyleRange createBoldStyleRange(String string, Color background) {
		StyleRange styleRange = new StyleRange();
		styleRange.fontStyle = SWT.BOLD;
		if (background != null) {
			styleRange.background = background;
		}
		styleRange.start = 0;
		styleRange.length = string.length();
		return styleRange;
	}
	
}
