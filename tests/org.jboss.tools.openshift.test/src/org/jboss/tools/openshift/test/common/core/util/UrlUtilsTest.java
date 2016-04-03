/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.test.common.core.util;

import static org.jboss.tools.openshift.common.core.utils.UrlUtils.getHost;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UrlUtilsTest {

	@Test
	public void testGetHost() {
		assertNull(getHost(null));
		assertEquals("", getHost(""));
		assertEquals("foo", getHost("http://foo/bar"));
		assertEquals("foo", getHost("http://foo/"));
		assertEquals("foo", getHost("http://foo"));
		assertEquals("foo", getHost("http://foo:8080/"));
		assertEquals("foo", getHost("foo"));
		assertEquals("foo", getHost("foo:8080"));
		assertEquals("foo", getHost("foo:8080/foo"));
		assertEquals("foo", getHost("foo"));
		assertEquals("foo", getHost("foo:8080"));
		assertEquals("foo", getHost("foo:8080/foo"));
		assertEquals("foo", getHost("//foo:8080/foo"));
		assertEquals("foo", getHost("user@foo:8080/foo"));
		assertEquals("foo", getHost("//user@foo:8080/foo"));
		assertEquals("foo", getHost("file:///foo/bar"));//it's probably incorrect
		assertEquals("", getHost("a@"));
		assertEquals("a", getHost("@a"));
		assertEquals("", getHost(":a"));
		assertEquals("a", getHost("/a"));
	}
}
