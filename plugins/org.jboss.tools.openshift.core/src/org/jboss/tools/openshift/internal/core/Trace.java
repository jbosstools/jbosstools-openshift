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
package org.jboss.tools.openshift.internal.core;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.plugin.AbstractTrace;

/**
 * Hook into platform trace functionality.  Message binding 
 * adheres to NLS.bind (e.g. bind("foo {0}",'bar'))
 * @author jeff.cantrill
 *
 */
public class Trace extends AbstractTrace{
	
	private static Trace instance = null;
	
	protected Trace(OpenShiftCoreActivator plugin) {
		super(plugin);
		instance = this;
	}
	
	public static void debug(String message, Object...args) {
		debug(message,null,args);
	}

	public static void debug(String message, Throwable err, Object...args) {
		traceInternal(instance, STRING_FINER, NLS.bind(message, args),err);
	}

	public static void info(String message, Object...args) {
		traceInternal(instance, STRING_INFO, NLS.bind(message, args));
	}
	
	public static void error(String message, Throwable err, Object...args) {
		traceInternal(instance, STRING_SEVERE, NLS.bind(message, args), err);
	}
	
	public static void warn(String message, Throwable err, Object...args) {
		traceInternal(instance, STRING_WARNING, NLS.bind(message, args), err);
	}
}
