/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils;

import com.openshift.restclient.model.IEvent;

/**
 * PropertySource for displaying IEvent details for Kubernetes Events
 * @author jeff.cantrill
 *
 */
public class EventPropertySource extends ResourcePropertySource<IEvent> {

	public EventPropertySource(IEvent resource) {
		super(resource);
	}
	
	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] {
			new UneditablePropertyDescriptor("firstSeen", "First Seen"),
			new UneditablePropertyDescriptor("lastSeen", "Last Seen"),
			new UneditablePropertyDescriptor("count", "Count"),
			new UneditablePropertyDescriptor("name", "Name"),
			new UneditablePropertyDescriptor("kind", "Kind"),
			new UneditablePropertyDescriptor("subobject", "Subobject"),
			new UneditablePropertyDescriptor("type", "Type"),
			new UneditablePropertyDescriptor("reason", "Reason"),
			new UneditablePropertyDescriptor("source", "Source"),
			new UneditablePropertyDescriptor("message", "Message"),
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		IEvent e = getResource();
		switch((String)id) {
		case "firstSeen": return DateTimeUtils.formatSince(e.getFirstSeenTimestamp());
		case "lastSeen": 
			return DateTimeUtils.formatSince(e.getLastSeenTimestamp());
		case "count": 
			return e.getCount();
		case "name" :
			return StringUtils.substringBefore(e.getName(), ".");
		case "kind" :
			return e.getKind();
		case "type": 
			return e.getType();
		case "reason" :
			return e.getReason();
		case "source" :
			return e.getEventSource();
		case "subobject" :
			return e.getInvolvedObject() != null ? e.getInvolvedObject().getFieldPath() : null;
		case "message" :
			return e.getMessage();
		}
		return super.getPropertyValue(id);
	}

}
