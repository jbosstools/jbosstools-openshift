/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.internal.ui.processor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swt.browser.Browser;
import org.jboss.tools.openshift.io.core.LoginResponse;
import org.jboss.tools.openshift.io.core.OSIOUtils;
import org.jboss.tools.openshift.io.internal.ui.OpenShiftIOUIActivator;

public class DefaultRequestProcessor implements RequestProcessor {

	private static final String SCRIPT = "return location.search;";
	
	private String landingURL;
			
	public DefaultRequestProcessor(String landingURL) {
		this.landingURL = landingURL;
	}

	@Override
	public LoginResponse getRequestInfo(Browser browser, String url, String content) {
		LoginResponse response = null;
		try {
			if (url.startsWith(landingURL)) {
				String tokenJSON = (String) browser.evaluate(SCRIPT);
				URI uri = new URI(tokenJSON);
				String json = getJSON(uri);
				if (json != null) {
					response = OSIOUtils.decodeLoginResponse(json);
				}
			}
		}
		catch (RuntimeException | IOException | URISyntaxException e) {
			OpenShiftIOUIActivator.logError(e.getLocalizedMessage(), e);
		}
		return response;
	}

	private String getJSON(URI uri) {
		String[] query = uri.getQuery().split("&");
		Map<String, String> parameters = Arrays.stream(query).map(parameter -> new String[] { parameter.split("=")[0], parameter.split("=")[1]}).collect(Collectors.toMap(element -> element[0], element -> element[1]));
		if (parameters.containsKey("token_json")) {
			return parameters.get("token_json");
		} else if (parameters.containsKey("api_token")) {
			return parameters.get("api_token");
		}
		return null;
	}
}
