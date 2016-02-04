/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.common.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void testHumanize() {
		assertEquals("Build Configs", StringUtils.humanize("buildConfigs"));
	}
	
	@Test
	public void testSerialize(){
		Map<String, String> map = new HashMap<String, String>();
		map.put("first", "avalue");
		map.put("second", "secondvalue");
		
		assertEquals("first=avalue,second=secondvalue", StringUtils.serialize(map));
	}

	@Test
	public void testRemoveTrailingSlashes(){
		assertNull(StringUtils.removeTrailingSlashes(null));
		assertEquals("", StringUtils.removeTrailingSlashes(""));
		assertEquals("", StringUtils.removeTrailingSlashes("/"));
		assertEquals("a", StringUtils.removeTrailingSlashes("a"));
		assertEquals("a/ ", StringUtils.removeTrailingSlashes("a/ /"));
		assertEquals("http://foo.bar", StringUtils.removeTrailingSlashes("http://foo.bar/"));
		assertEquals("a", StringUtils.removeTrailingSlashes("a//"));
	}

	@Test
	public void testShortenParts(){
		String[] parts = new String[]{"I am 18 chars long", "7 chars", "?"};
		StringUtils.shorten(parts, 12);
		assertEquals("I a...ng", parts[0]);
		assertEquals("...", parts[1]);
		assertEquals("?", parts[2]);
	}

	@Test
	public void testShortenPartsWithNull(){
		String[] parts = new String[]{"I am 18 chars long", null, "7 chars"};
		StringUtils.shorten(parts, 12);
		assertEquals("I a...ng", parts[0]);
		assertEquals("7...", parts[2]);
		assertNull(parts[1]);
	}
}
