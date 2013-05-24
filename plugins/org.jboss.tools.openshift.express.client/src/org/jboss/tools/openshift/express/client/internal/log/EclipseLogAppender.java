/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.jboss.tools.openshift.express.client.internal.log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * @author Peter Friese - Initial contribution and API
 * @author Sven Efftinge
 * @author Knut Wannheden - Refactored handling when used in non OSGi
 *         environment
 * @author Andre Dietisheim
 */
public class EclipseLogAppender extends AppenderSkeleton {

	private static final String OPENSHIFT_CLIENT_BUNDLE = "org.jboss.tools.openshift.express.client";
	private static final String OPENSHIFT_CLIENT_TRACE = "/client";
	
	private ILog log;

	private Trace trace;

	public EclipseLogAppender() {
		this.trace = new Trace(OPENSHIFT_CLIENT_BUNDLE);
	}

	@Override
	protected void append(LoggingEvent event) {
	// always put client-lib logging to tracing, gets too chatty otherwise
	// if (Level.TRACE.equals(event.getLevel())) {
			trace(event);
	// } else {
	//		log(event);
	// }
	}	

	private void trace(LoggingEvent event) {
		if(trace.isDebugging()) {
			trace.trace(OPENSHIFT_CLIENT_TRACE, String.valueOf(event.getMessage()));
		}
	}

	private void log(LoggingEvent event) {
		String logString = layout.format(event);
		ILog myLog = getLog();
		if (myLog != null) {
			String loggerName = event.getLoggerName();
			int severity = mapLevel(event.getLevel());
			final Throwable throwable = event.getThrowableInformation() != null ? event
					.getThrowableInformation().getThrowable() : null;
			IStatus status = createStatus(severity, loggerName, logString,
					throwable);
			getLog().log(status);
		} else {
			// nothing to do (message should be logged to stdout by default
			// appender)
		}
	}

	private int mapLevel(Level level) {
		switch (level.toInt()) {
		case Priority.DEBUG_INT:
		case Priority.INFO_INT:
			return IStatus.INFO;

		case Priority.WARN_INT:
			return IStatus.WARNING;

		case Priority.ERROR_INT:
		case Priority.FATAL_INT:
			return IStatus.ERROR;

		default:
			return IStatus.INFO;
		}
	}

	private ILog getLog() {
		if (log == null) {
			log = Platform.getLog(Platform.getBundle(OPENSHIFT_CLIENT_BUNDLE));
		}
		return log;
	}
	
	private IStatus createStatus(int severity, String loggerName,
			String message, Throwable throwable) {
		return new Status(severity, OPENSHIFT_CLIENT_BUNDLE, message, throwable);
	}

	public void close() {
		trace.close();
	}

	public boolean requiresLayout() {
		return true;
	}

	// protected static boolean shouldTrace(String pluginId) {
	//		return shouldTrace0(pluginId + "/debug"); //$NON-NLS-1$
	// }
	//
	// protected boolean shouldTrace0(String option) {
	// if (option == null)
	// return false;
	// OpenShiftClientActivator.Activator activator = Activator.getDefault();
	// if (activator == null)
	// return false;
	// DebugOptions debugOptions = activator.getDebugOptions();
	// if (debugOptions == null)
	// return false;
	// String result = debugOptions.getOption(option);
	//		return (result == null) ? false : result.equalsIgnoreCase("true"); //$NON-NLS-1$
	// }


}