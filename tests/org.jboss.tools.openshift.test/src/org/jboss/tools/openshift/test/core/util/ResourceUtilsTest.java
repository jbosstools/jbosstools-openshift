/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.util;

import static org.junit.Assert.*;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.selectorsOverlap;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ResourceUtilsTest {

	@Test
	public void testSelectorsOverlapDoesNotNullPointer() {
		assertFalse(selectorsOverlap(null, new HashMap<>()));
		assertFalse(selectorsOverlap(new HashMap<>(), null));
	}
	
	@Test
	public void testSelectorsDoNotMatchWhenTargetDoesNotContainAllSourceKeys() {
		Map<String, String> source = new HashMap<>(); 
		source.put("foo", "bar");
		Map<String, String> target= new HashMap<>(); 
		target.put("xyz", "abc");
		
		assertFalse(selectorsOverlap(source, target));
	}
	
	@Test
	public void testSelectorsDoNotMatchWhenTargetValuesDoNotMatchSourceValues() {
		Map<String, String> source = new HashMap<>(); 
		source.put("foo", "bar");
		Map<String, String> target= new HashMap<>(); 
		target.put("foo", "abc");
		target.put("xyz", "bar");
		
		assertFalse(selectorsOverlap(source, target));
	}
	
	@Test
	public void testSelectorMatchesWhenTargetIncludesAllSourceKeyAndValues() {
		Map<String, String> source = new HashMap<>(); 
		source.put("foo", "bar");
		Map<String, String> target= new HashMap<>(); 
		target.put("foo", "bar");
		target.put("xyz", "bar");
		assertTrue(selectorsOverlap(source, target));
	}

}
