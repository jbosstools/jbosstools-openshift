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
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4j eclipse appender
 */
public class EclipseLogAppender extends AppenderSkeleton {

	private static final String OPENSHIFT_CLIENT_TRACE = "/client";
	
	private Trace trace;

	public EclipseLogAppender(){
		this.trace = new Trace("org.jboss.tools.openshift.express.client");
	}
	
	@Override
	protected void append(LoggingEvent event) {
		// always put client-lib logging to tracing, gets too chatty otherwise
			trace(event);
	}	

	private void trace(LoggingEvent event) {
		if(trace.isDebugging()) {
			trace.trace(OPENSHIFT_CLIENT_TRACE, String.valueOf(event.getMessage()));
		}
	}

	@Override
	public void close() {
		trace.close();
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

}