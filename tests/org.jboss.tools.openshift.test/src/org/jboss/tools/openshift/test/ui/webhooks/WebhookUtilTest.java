/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.webhooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.internal.ui.webhooks.WebhookUtil;
import org.junit.Test;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.IWebhookTrigger;

/**
 * @author Fred Bricon
 */
public class WebhookUtilTest {

	@Test
	public void testGetWebHooks() {
		IBuildConfig buildConfig = givenBuildConfigWithTriggers(genericTrigger(), imageChangeTrigger(),
				githubTrigger());
		List<IWebhookTrigger> webHooks = WebhookUtil.getWebHooks(buildConfig);
		assertEquals(2, webHooks.size());
		assertNotNull(webHooks);
		assertEquals(BuildTriggerType.GENERIC, webHooks.get(0).getType());
		assertEquals(BuildTriggerType.GITHUB, webHooks.get(1).getType());
	}

	@Test
	public void testEmptyWebHooks() {
		IBuildConfig buildConfig = givenBuildConfigWithTriggers();
		List<IWebhookTrigger> webHooks = WebhookUtil.getWebHooks(buildConfig);
		assertNotNull(webHooks);
		assertEquals(0, webHooks.size());
	}

	private IBuildConfig givenBuildConfigWithTriggers(IBuildTrigger... buildTriggers) {
		IBuildConfig buildConfig = mock(IBuildConfig.class);
		List<IBuildTrigger> triggers = (buildTriggers == null) ? Collections.<IBuildTrigger> emptyList()
				: Arrays.asList(buildTriggers);
		when(buildConfig.getBuildTriggers()).thenReturn(triggers);
		return buildConfig;
	}

	private IBuildTrigger genericTrigger() {
		return mockTrigger(IWebhookTrigger.class, BuildTriggerType.GENERIC);
	}

	private IBuildTrigger imageChangeTrigger() {
		return mockTrigger(IBuildTrigger.class, BuildTriggerType.IMAGE_CHANGE);
	}

	private IBuildTrigger githubTrigger() {
		return mockTrigger(IWebhookTrigger.class, BuildTriggerType.GITHUB);
	}

	private IBuildTrigger mockTrigger(Class<? extends IBuildTrigger> clazz, String type) {
		IBuildTrigger trigger = mock(clazz);
		when(trigger.getType()).thenReturn(type);
		return trigger;
	}
}
