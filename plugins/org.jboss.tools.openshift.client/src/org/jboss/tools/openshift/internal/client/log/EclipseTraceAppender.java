/******************************************************************************* 
 * Copyright (c) 2020 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.client.log;

import static org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.jboss.tools.openshift.internal.client.OpenShiftClientActivator;

@Plugin(name = "EclipseTraceAppender", category = "Core", elementType = "appender", printObject = true)
public final class EclipseTraceAppender extends AbstractAppender {

	private static final String OPENSHIFT_CLIENT_TRACE = "/client";

	private final Trace trace = new Trace(OpenShiftClientActivator.PLUGIN_ID);
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();

	private EclipseTraceAppender(String name, Layout<? extends Serializable> layout, Filter filter,
			boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions, EMPTY_ARRAY);
	}

	@PluginFactory
	public static EclipseTraceAppender createAppender(
			@PluginAttribute("name") String name,
			@PluginElement("Layout") Layout<? extends Serializable> layout, 
			@PluginElement("Filters") Filter filter,
			@PluginAttribute("ignoreExceptions") final String ignoreExceptions) {

		if (name == null) {
			LOGGER.error("No name provided for EclipseTraceAppender");
			return null;
		}
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}
		return new EclipseTraceAppender(name, layout, filter, Booleans.parseBoolean(ignoreExceptions, true));
	}

	@Override
	public void append(LogEvent event) {
		readLock.lock();
		try {
			if (trace.isDebugging()) {
				trace.trace(OPENSHIFT_CLIENT_TRACE, new String(getLayout().toByteArray(event)));
			}
		} catch (Exception ex) {
			if (!ignoreExceptions()) {
				throw new AppenderLoggingException(ex);
			}
		} finally {
			readLock.unlock();
		}
	}
	
    @Override
    public void stop() {
    	super.stop();
    	if (trace != null) {
    		trace.close();
    	}
    }

}
