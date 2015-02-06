/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.common.core.util;

import static org.junit.Assert.*;

import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void testHumanize() {
		assertEquals("Build Configs", StringUtils.humanize("buildConfigs"));
	}

}
