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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.eclipse.ui.browser.BrowserEditor;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersView;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
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
			new ServersView().open();
			serverAdapter.select();
			new ContextMenu("Show In", "Web Browser").select();
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
