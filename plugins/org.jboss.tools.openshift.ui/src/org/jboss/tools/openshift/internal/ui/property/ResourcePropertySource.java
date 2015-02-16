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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.openshift3.client.model.IResource;

public  class ResourcePropertySource implements IPropertySource {
	
	private static final String ANNOTATIONS = "Annotations";
	private static final String CATEGORY = "Basic";
	private static final String LABELS = "Labels";
	
	private IResource  resource;
	
	protected ResourcePropertySource(IResource resource){
		this.resource = resource;
	}
	/**
	 * Retrieve the list of property descriptors that are specific to the given
	 * resource type.  Subclasses should override
	 * @return
	 */
	protected IPropertyDescriptor[] getResourcePropertyDescriptors(){
		return null;
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		 IPropertyDescriptor[] common = new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(Ids.Name, CATEGORY),
				new ExtTextPropertyDescriptor(Ids.Created, "Creation Timestamp", CATEGORY),
				new ExtTextPropertyDescriptor(Ids.Namespace, CATEGORY)
		};
		 List<IPropertyDescriptor> annotations = buildPropertyDescriptors(ANNOTATIONS, resource.getAnnotations());
		 List<IPropertyDescriptor> labels = buildPropertyDescriptors(LABELS, resource.getLabels());
		 common =  (IPropertyDescriptor[]) ArrayUtils.addAll(common, annotations.toArray());
		 common =  (IPropertyDescriptor[]) ArrayUtils.addAll(common, labels.toArray());
		 return (IPropertyDescriptor[]) ArrayUtils.addAll(common, getResourcePropertyDescriptors());
	}
	
	private List<IPropertyDescriptor> buildPropertyDescriptors(String prefix, Map<String, String> values){
		 List<IPropertyDescriptor> descriptors = new ArrayList<IPropertyDescriptor>(values.size()); 
		 for (Map.Entry<String, String> entry : values.entrySet()) {
			descriptors.add(new ExtTextPropertyDescriptor(new PrefixPropertySourceKey(prefix, entry.getKey()), entry.getKey(), prefix));
		}
		return descriptors;
	}
	
	@Override
	public Object getPropertyValue(Object id) {
		if(id instanceof Ids){
			Ids e = (Ids)id;
			switch(e){
			case Name: return resource.getName();
			case Namespace: return resource.getNamespace();
			case Created: return resource.getCreationTimeStamp();
			default:
			}
		}
		if(id instanceof PrefixPropertySourceKey){
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
	
	public static enum Ids{
		Created,
		Name,
		Namespace
	}
}
