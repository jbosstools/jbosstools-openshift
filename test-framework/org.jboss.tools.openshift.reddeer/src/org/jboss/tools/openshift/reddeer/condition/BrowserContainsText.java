/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import org.eclipse.core.runtime.Platform;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

public class BrowserContainsText extends AbstractWaitCondition {

	private InternalBrowser browser;
	private String text;
	private String url;
	private boolean refresh;

	public BrowserContainsText(String url, String text) {
		browser = new InternalBrowser();
		this.url = url;
		this.text = text;
		this.refresh = true;
	}

	public BrowserContainsText(String url, String text, boolean refresh) {
		this(url, text);
		this.refresh = refresh;
	}

	public BrowserContainsText(String text) {
		this(null, text);
	}

	public BrowserContainsText(String text, boolean refresh) {
		this(null, text, refresh);
	}

	@Override
	public boolean test() {
		browser = new InternalBrowser();
		if (url != null) {
			browser.setURL(url);
			if (refresh)
				browser.refresh();
		} else {
			if (refresh)
				browser.refresh();
		}

		new WaitUntil(new PageIsLoaded(browser));

		if (Platform.getOS().startsWith(Platform.OS_WIN32)) {
			return browser.getText().contains(text);
		} else {
			//Workaround for webkit issues with method browser.getText(), e.g. https://bugs.eclipse.org/bugs/show_bug.cgi?id=514719
			String pageHTML = "";
			if (!StringUtils.isEmptyOrNull(browser.getURL())) {
				pageHTML = (String) browser.evaluate("return document.documentElement.innerHTML;"); 
			}
			return pageHTML.contains(text);
		}

	}

	@Override
	public String description() {
		return "browser contains text";
	}
}
