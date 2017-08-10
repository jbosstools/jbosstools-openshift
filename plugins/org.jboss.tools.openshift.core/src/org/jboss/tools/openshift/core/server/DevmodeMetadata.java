/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class DevmodeMetadata {

	// default fallback
	private static final String DEFAULT_ENABLEMENT_KEY = "DEBUG";
	private static final String DEFAULT_ENABLEMENT_VALUE = "true";
	private static final String DEFAULT_PORT_KEY = "DEBUG_PORT";
	private static final String DEFAULT_PORT_VALUE = "8787";

	// "image->"dockerImageMetadata"->"Config"->"Labels"->
	private static final Pattern REGEX_LABEL_DEVMODE = Pattern
			.compile("\\\"com\\.redhat\\.dev-mode\\\" ?\\: ?\\\"([^(:|\\\")]+)(:|\\\")([^\\\"]*)");
	private static final Pattern REGEX_LABEL_DEVMODE_PORT = Pattern
			.compile("\\\"com\\.redhat\\.dev-mode\\.port\\\" ?\\: ?\\\"([^(:|\\\")]+)(:|\\\")([^\\\"]*)");

	private String enablementKey;
	private String enablementValue;
	private String portKey;
	private String portValue;

	public DevmodeMetadata(String metadata) {
		parse(metadata);
	}

	public String getEnablementKey() {
		return enablementKey;
	}

	public String getEnablementValue() {
		return enablementValue;
	}

	public String getPortKey() {
		return portKey;
	}

	public String getPortValue() {
		return portValue;
	}

	private void parse(String metadata) {
		if (!StringUtils.isEmpty(metadata)) {
			parseEnablement(metadata);
			parsePort(metadata);
		}
	}

	private void parseEnablement(String metadata) {
		Matcher matcher = REGEX_LABEL_DEVMODE.matcher(metadata);
		if (matcher.find()) {
			this.enablementKey = matcher.group(1);
			this.enablementValue = matcher.group(3);
		} else {
			this.enablementKey = DEFAULT_ENABLEMENT_KEY;
			this.enablementValue = DEFAULT_ENABLEMENT_VALUE;
		}
	}

	private void parsePort(String metadata) {
		Matcher matcher = REGEX_LABEL_DEVMODE_PORT.matcher(metadata);
		if (matcher.find()) {
			this.portKey = matcher.group(1);
			this.portValue = matcher.group(3);
		} else {
			this.portKey = DEFAULT_PORT_KEY;
			this.portValue = DEFAULT_PORT_VALUE;
		}
	}

}
