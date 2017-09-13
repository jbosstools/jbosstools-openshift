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
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.ui.browser.BrowserEditor;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jboss.tools.common.reddeer.utils.StackTraceUtils;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;

public class BrowserIsReadyElseReloadCondition extends AbstractWaitCondition {

	private static Logger LOGGER = new Logger(BrowserIsReadyElseReloadCondition.class);
	private ServerAdapter serverAdapter;

	public BrowserIsReadyElseReloadCondition(ServerAdapter serverAdapter) {
		this.serverAdapter = serverAdapter;
	}

	@Override
	public boolean test() {
		BrowserEditor browserEditor = null;
		try {
			browserEditor = new BrowserEditor(new BaseMatcher<String>() {

				@Override
				public boolean matches(Object arg0) {
					return true;
				}

				@Override
				public void describeTo(Description arg0) {
					// TODO Auto-generated method stub

				}
			});
		} catch (CoreLayerException ex) {
			LOGGER.debug("CoreLayerException in waiting for browser in BrowserIsReadyElseReloadCondition");
			LOGGER.debug(StackTraceUtils.stackTraceToString(ex));
			return false;
		}
		String text = browserEditor.getText();
		if (text.contains("Unable to load page") || text.contains("404")) {
			LOGGER.debug("Refreshing browser");
			new ServersView2().open();
			serverAdapter.select();
			new ContextMenuItem("Show In", "Web Browser").select();
			return false;
		} else {
			// Browser is ready
			return true;
		}
	}

	@Override
	public String description() {
		return "BrowserIsReadyElseRefresh";
	}

}
