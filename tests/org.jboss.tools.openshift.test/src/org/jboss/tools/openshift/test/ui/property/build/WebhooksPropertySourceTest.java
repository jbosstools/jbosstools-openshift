/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.property.build;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.property.build.WebHooksPropertySource;
import org.junit.Test;

import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.IWebhookTrigger;

public class WebhooksPropertySourceTest {

	@Test
	public void test() {
		List<IBuildTrigger> triggers = givenBuildTriggers();
		WebHooksPropertySource source = new WebHooksPropertySource(triggers);
		IPropertyDescriptor[] propertyDescriptors = source.getPropertyDescriptors();
		assertEquals("Exp. descripter for each webhook only", 2, propertyDescriptors.length);
		assertEquals("Generic", propertyDescriptors[0].getId());
		assertEquals("GitHub", propertyDescriptors[1].getId());
	}
	
	private List<IBuildTrigger> givenBuildTriggers(){
		List<IBuildTrigger> triggers = Arrays.asList(new IBuildTrigger[]{
				mock(IWebhookTrigger.class),
				mock(IBuildTrigger.class),
				mock(IWebhookTrigger.class)
		});
		when(triggers.get(0).getType()).thenReturn(BuildTriggerType.GENERIC);
		when(triggers.get(1).getType()).thenReturn(BuildTriggerType.IMAGE_CHANGE);
		when(triggers.get(2).getType()).thenReturn(BuildTriggerType.GITHUB);
		return triggers;
	}

}
