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

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.swt.impl.browser.InternalBrowser;

public class BrowserContainsText extends AbstractWaitCondition {

	private InternalBrowser browser;
	private String text;
	private String url;
	
	public BrowserContainsText(String url, String text) {
		browser = new InternalBrowser();
		this.url = url;
		this.text = text;
	}
	
	public BrowserContainsText(String text) {
		this(null, text);
	}
	
	@Override
	public boolean test() {
		if (url != null) {
			browser.setURL(url);
			browser.refresh();
		} else {
			browser.refresh();
		}
		return browser.getText().contains(text);
	}

	@Override
	public String description() {
		return "browser contains text";
	}

}
