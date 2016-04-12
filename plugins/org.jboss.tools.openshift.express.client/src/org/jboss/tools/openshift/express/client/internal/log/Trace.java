/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.client.internal.log;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class Trace {

	public static final String GLOBAL_DEBUG_KEY = "/debug";
	public static final String CLIENT_DEBUG_KEY = "/debug/client";

	private static final boolean DEFAULT_DEBUG = false;

	private String pluginId;
	private DebugOptions options;
	private ServiceTracker<DebugOptions, DebugOptions> tracker;
	private DebugTrace trace;

	public Trace(String pluginId) {
		this.pluginId = pluginId;
	}

	public void close() {
		if (tracker != null) {
			tracker.close();
		}
	}

	private DebugOptions createDebugOptions() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		if (bundle == null) {
			return null;
		}
		BundleContext context = bundle.getBundleContext();
		if (context == null)
			return null;
		this.tracker = 
				new ServiceTracker<>(context, DebugOptions.class.getName(), null);
		tracker.open();
		return tracker.getService();
	}

	public boolean isDebugging() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		if (bundle == null) {
			return DEFAULT_DEBUG;
		}
		
		DebugOptions debugOptions = getDebugOptions();
		if (debugOptions == null) {
			return DEFAULT_DEBUG;
		}
		
		if(!debugOptions.isDebugEnabled()) {
			return false;
		}
		
		if (!debugOptions.getBooleanOption(pluginId + GLOBAL_DEBUG_KEY, DEFAULT_DEBUG)) {
			return false;
		}

		return debugOptions.getBooleanOption(pluginId + CLIENT_DEBUG_KEY, DEFAULT_DEBUG);
	}
	
	private DebugTrace getDebugTrace() {
		if (trace == null) {
			this.trace = getDebugOptions().newDebugTrace(pluginId);
		}
		return trace;
	}
	
	public void trace(String option, String message) {
		getDebugTrace().trace(GLOBAL_DEBUG_KEY + option, message);
	}

	private DebugOptions getDebugOptions() {
		if (options == null) {
			this.options = createDebugOptions();
		}
		return this.options;
	}
}
