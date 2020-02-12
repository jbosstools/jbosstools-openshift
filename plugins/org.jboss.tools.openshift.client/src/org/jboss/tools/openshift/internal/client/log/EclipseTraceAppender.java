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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jboss.tools.openshift.internal.client.OpenShiftClientActivator;

@Plugin(name = "Stub", category = "Core", elementType = "appender", printObject = true)
public final class EclipseTraceAppender extends AbstractAppender {

	private static final String OPENSHIFT_CLIENT_TRACE = "/client";

	private Trace trace;

	private EclipseTraceAppender(String name, Layout<? extends Serializable> layout, Filter filter,
			boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions, EMPTY_ARRAY);

		this.trace = new Trace(OpenShiftClientActivator.PLUGIN_ID);
	}

	@PluginFactory
	public static EclipseTraceAppender createAppender(@PluginAttribute("name") String name,
			@PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginElement("Filters") Filter filter) {

		if (name == null) {
			LOGGER.error("No name provided for EclipseTraceAppender");
			return null;
		}
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}
		return new EclipseTraceAppender(name, layout, filter, ignoreExceptions);
	}

	@Override
	public void append(LogEvent event) {
		if (trace.isDebugging()) {
			trace.trace(OPENSHIFT_CLIENT_TRACE, String.valueOf(event.getMessage()));
		}
	}
}
