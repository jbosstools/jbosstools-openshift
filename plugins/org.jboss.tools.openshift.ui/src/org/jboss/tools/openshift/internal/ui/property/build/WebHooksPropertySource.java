/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.IWebhookTrigger;

/**
 * Property source to display a BuildConfig's webhook triggers.
 */
public class WebHooksPropertySource implements IPropertySource{

	private static final String GITHUB = "GitHub";
	private static final String GENERIC = "Generic";
	
	private IWebhookTrigger genericTrigger;
	private IWebhookTrigger gitTrigger;

	public WebHooksPropertySource(Collection<IBuildTrigger> triggers ){
		for (IBuildTrigger trigger : triggers) {
			switch(trigger.getType()){
			case BuildTriggerType.generic:
			case BuildTriggerType.GENERIC:
				genericTrigger = (IWebhookTrigger) trigger;
				break;
			case BuildTriggerType.github:
			case BuildTriggerType.GITHUB:
				gitTrigger = (IWebhookTrigger) trigger;
				break;
			default:
			}
		}
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> descriptors = new ArrayList<>(2);
		if(genericTrigger != null) descriptors.add(new PropertyDescriptor(GENERIC, "Generic URL"));
		if(gitTrigger != null) descriptors.add(new PropertyDescriptor(GITHUB, "Github URL"));
		return descriptors.toArray(new IPropertyDescriptor[]{});
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(GENERIC.equals(id))
			return genericTrigger.getWebhookURL();
		if(GITHUB.equals(id))
			return gitTrigger.getWebhookURL();
		return null;
	}

	@Override
	public boolean isPropertySet(Object arg0) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object arg0) {
	}

	@Override
	public void setPropertyValue(Object arg0, Object arg1) {
	}
	
}
