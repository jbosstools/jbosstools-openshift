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

import org.eclipse.swt.browser.Browser;
import org.jboss.tools.openshift.io.core.LoginResponse;
import org.jboss.tools.openshift.io.core.OSIOUtils;
import org.jboss.tools.openshift.io.internal.ui.OpenShiftIOUIActivator;

public class DefaultRequestProcessor implements RequestProcessor {

	private static String SCRIPT = "           var request = (function() {\r\n" + 
			"    var _get = {};\r\n" + 
			"    var re = /[?&]([^=&]+)(=?)([^&]*)/g;\r\n" + 
			"    while (m = re.exec(location.search))\r\n" + 
			"        _get[decodeURIComponent(m[1])] = (m[2] == '=' ? decodeURIComponent(m[3]) : true);\r\n" + 
			"    return _get;\r\n" + 
			"})();\r\n" + 
			"return request.token_json;";
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
				response = OSIOUtils.decodeLoginResponse(tokenJSON);
			}
		}
		catch (RuntimeException | IOException e) {
			OpenShiftIOUIActivator.logError(e.getLocalizedMessage(), e);
		}
		return response;
	}
}
