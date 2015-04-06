/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.property;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.jboss.tools.openshift.test.ui.property.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openshift.restclient.model.IResource;

import org.jboss.tools.openshift.internal.ui.property.PrefixPropertySourceKey;
import org.jboss.tools.openshift.internal.ui.property.ExtTextPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.property.ResourcePropertySource;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourcePropertySourceTest {
	
	@Mock private IResource resource;
	private ResourcePropertySource<IResource> source;
	
	@Before
	public void setup(){
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("foo","bar");
		labels.put("bar","bbar");
		Map<String, String> annotations = new HashMap<String, String>();
		annotations.put("xyz", "abc");
		annotations.put("efg", "def");
		
		when(resource.getName()).thenReturn("aname");
		when(resource.getLabels()).thenReturn(labels);
		when(resource.getAnnotations()).thenReturn(annotations);
		when(resource.getAnnotation("xyz")).thenReturn("abc");
		when(resource.getAnnotation("efg")).thenReturn("def");
		when(resource.getCreationTimeStamp()).thenReturn("2014");
		when(resource.getNamespace()).thenReturn("anamespace");

		source = new ResourcePropertySource<IResource>(resource);
	}
	
	@Test
	public void getPropertyValue(){
		assertEquals("aname", source.getPropertyValue(ResourcePropertySource.Ids.Name));
		assertEquals("anamespace", source.getPropertyValue(ResourcePropertySource.Ids.Namespace));
		assertEquals("2014", source.getPropertyValue(ResourcePropertySource.Ids.Created));
		assertEquals("abc", source.getPropertyValue(new PrefixPropertySourceKey("Annotations", "xyz")));
		assertEquals("def", source.getPropertyValue(new PrefixPropertySourceKey("Annotations", "efg")));
		assertEquals("bar", source.getPropertyValue(new PrefixPropertySourceKey("Labels", "foo")));
	}
	
	@Test
	public void getPropertyDescriptor() {
		IPropertyDescriptor [] exp = new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.Name, "Name", "Basic"),
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.Created, "Creation Timestamp", "Basic"),
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.Namespace, "Namespace", "Basic"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Annotations", "xyz"), "xyz", "Annotations"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Annotations", "efg"), "efg", "Annotations"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Labels", "foo"), "foo", "Labels"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Labels", "bar"), "bar", "Labels")
		};
		assertPropertyDescriptorsEquals(exp, source.getPropertyDescriptors());
	}

}
