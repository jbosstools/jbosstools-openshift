/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.webhooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.IWebhookTrigger;

/**
 * Utility class to extract and manipulate webhooks.
 * 
 * @author Fred Bricon
 */
public class WebhookUtil {

	public static List<IWebhookTrigger> getWebHooks(IBuildConfig buildConfig) {
		if (buildConfig == null) {
			return null;
		}
		List<IBuildTrigger> triggers = buildConfig.getBuildTriggers();
		List<IWebhookTrigger> webHooks = null;
		if (triggers == null || triggers.isEmpty()) {
			webHooks = Collections.emptyList();
		} else {
			webHooks = new ArrayList<>(triggers.size());
			for (IBuildTrigger trigger : triggers) {
				IWebhookTrigger webHook = getAsWebHook(trigger);
				if (webHook != null) {
					webHooks.add(webHook);
				}
			}
		}
		return webHooks;
	}

	private static IWebhookTrigger getAsWebHook(IBuildTrigger trigger) {
		if (trigger == null) {
			return null;
		}
		switch (trigger.getType()) {
		case BuildTriggerType.generic:
		case BuildTriggerType.GENERIC:
		case BuildTriggerType.github:
		case BuildTriggerType.GITHUB:
			return (IWebhookTrigger) trigger;
		default:
			return null;
		}
	}
}
