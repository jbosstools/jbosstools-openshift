/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.swt.api.Browser;

public class PageIsLoaded extends AbstractWaitCondition {

	private Browser browser;

	/**
	 * Constructs PageIsLoaded wait condition. Condition is met when 
	 * page is loaded in specified browser. 
	 * 
	 * @param browser browser where to test page availability status (loaded or not)
	 */
	public PageIsLoaded(Browser browser) {
		this.browser = browser;
	}

	@Override
	public boolean test() {
		return browser.isPageLoaded();
	}

	@Override
	public String description() {
		return "page is loaded to browser";
	}

}
