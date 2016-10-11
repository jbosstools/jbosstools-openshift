/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.js.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.wst.jsdt.chromium.debug.core.model.LaunchParams.PredefinedSourceWrapperIds;
import org.eclipse.wst.jsdt.chromium.debug.core.util.MementoFormat;

/**
 * @author "Ilya Buziuk (ibuziuk)"
 */
public final class NodeDebuggerUtil {
	
	private NodeDebuggerUtil() {
	}
	
	public static final String CHROMIUM_LAUNCH_CONFIGURATION_TYPE_ID = "org.eclipse.wst.jsdt.chromium.debug.ui.LaunchType$StandaloneV8"; //$NON-NLS-1$
	public static final String LOCALHOST = "localhost"; //$NON-NLS-1$
	public static final String PACKAGE_JSON = "package.json"; //$NON-NLS-1$

	// By default source wrappers from {@link HardcodedSourceWrapProvider} must be enabled
	@SuppressWarnings("serial")
	public static final List<String> PREDEFIENED_WRAPPERS = new ArrayList<String>() {
		{
			add("org.eclipse.wst.jsdt.chromium.debug.core.model.HardcodedSourceWrapProvider$NodeJsStandardEntry"); //$NON-NLS-1$
			add("org.eclipse.wst.jsdt.chromium.debug.core.model.HardcodedSourceWrapProvider$NodeJsWithDefinedEntry"); //$NON-NLS-1$
		}
	};

	/**
	 * Encoding predefined source wrappers via {@link MementoFormat} for correct
	 * decoding in {@link PredefinedSourceWrapperIds}
	 */
	public static String encode(List<String> wrappers) {
		StringBuilder output = new StringBuilder();
		Collections.sort(wrappers);
		for (String wrapper : wrappers) {
			MementoFormat.encodeComponent(wrapper, output);
		}
		return output.toString();
	}

}
