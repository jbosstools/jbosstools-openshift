/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.openshift.restclient.model.IResource;

public class ResourcePropertySource<T extends IResource> implements IPropertySource {

	private static final String BASIC = "Basic";
	private static final String ANNOTATIONS = "Annotations";
	private static final String LABELS = "Labels";
	
	private T  resource;
	
	public ResourcePropertySource(T resource) {
		this.resource = resource;
	}

	protected T getResource() {
		return (T) resource;
	}

	/**
	 * Retrieve the list of property descriptors that are specific to the given
	 * resource type.  Subclasses should override
	 * @return
	 */
	protected IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {};
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> common = new ArrayList<>(Arrays.<IPropertyDescriptor> asList(
				new ExtTextPropertyDescriptor(Ids.KIND, "Kind", BASIC),
				new ExtTextPropertyDescriptor(Ids.NAME, "Name", BASIC),
				new ExtTextPropertyDescriptor(Ids.NAMESPACE, "Namespace", BASIC),
				new ExtTextPropertyDescriptor(Ids.CREATED, "Creation Timestamp", BASIC),
				new ExtTextPropertyDescriptor(Ids.RESOURCE_VERSION, "Resource Version", BASIC)
		));
		 common.addAll(buildPropertyDescriptors(ANNOTATIONS, resource.getAnnotations()));
		 common.addAll(buildPropertyDescriptors(LABELS, resource.getLabels()));
		 common.addAll(Arrays.asList(getResourcePropertyDescriptors()));

		 return common.toArray(new IPropertyDescriptor[common.size()]);
	}
	
	private List<IPropertyDescriptor> buildPropertyDescriptors(String prefix, Map<String, String> values) {
		 List<IPropertyDescriptor> descriptors = new ArrayList<>(values.size()); 
		 for (Map.Entry<String, String> entry : values.entrySet()) {
			descriptors.add(new ExtTextPropertyDescriptor(new PrefixPropertySourceKey(prefix, entry.getKey()), entry.getKey(), prefix));
		}
		return descriptors;
	}
	
	@Override
	public Object getPropertyValue(Object id) {
		if (id instanceof Ids) {
			Ids e = (Ids) id;
			switch (e) {
			case KIND: return resource.getKind();
			case NAME: return resource.getName();
			case NAMESPACE: return resource.getNamespace();
			case CREATED: return resource.getCreationTimeStamp();
			case RESOURCE_VERSION: return resource.getResourceVersion();
			default:
			}
		}
		if (id instanceof PrefixPropertySourceKey) {
			PrefixPropertySourceKey key = (PrefixPropertySourceKey) id;
			String prefix = key.getPrefix();
			if(ANNOTATIONS.equals(prefix)){
				return resource.getAnnotation(key.getKey());
			}
			if(LABELS.equals(prefix)){
				return resource.getLabels().get(key.getKey());
			}
		}
		return null;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
	}
	
	public static enum Ids {
		KIND,
		NAME,
		NAMESPACE,
		CREATED,
		RESOURCE_VERSION
	}
}
