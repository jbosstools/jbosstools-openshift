/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property.build;

import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jboss.tools.openshift.internal.ui.property.UneditablePropertyDescriptor;

import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.IImageChangeTrigger;

public class ImageChangePropertySource implements IPropertySource {
	
	private IImageChangeTrigger trigger;
	
	public ImageChangePropertySource(List<IBuildTrigger> buildTriggers) {
		for (IBuildTrigger trigger : buildTriggers) {
			if(trigger.getType() == BuildTriggerType.imageChange){
				this.trigger = (IImageChangeTrigger) trigger;
				break;
			}
		}
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new UneditablePropertyDescriptor("image", "Image"),
				new UneditablePropertyDescriptor("from", "From"),
				new UneditablePropertyDescriptor("tag", "Tag")
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(trigger == null) return null;
		if("image".equals(id)) return trigger.getImage();
		if("from".equals(id)) return trigger.getFrom();
		if("tag".equals(id)) return trigger.getTag();
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
