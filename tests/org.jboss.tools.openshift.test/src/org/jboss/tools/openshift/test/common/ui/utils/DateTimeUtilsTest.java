/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.common.ui.utils;

import static org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils.formatDuration;
import static org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils.formatSince;
import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DateTimeUtilsTest {

	private static Locale originalLocale;
	
	@BeforeClass
	public static void beforeClass() {
		originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
	}
	
	@AfterClass
	public static void afterClass() {
		Locale.setDefault(originalLocale);
	}
	
	@Test
	public void testFormatDurationLessThanSec() {
		long duration = 33000000L;
		assertEquals("Now", formatDuration(duration));
	}

	@Test
	public void testFormatDurationLessThanHour() {
		long duration = 330000000000L;
		assertEquals("5 min. 30 sec.", formatDuration(duration));
	}

	@Test
	public void testFormatDurationLessThanDay() {
		long duration = 33000000000000L;
		assertEquals("9 hrs. 10 min.", formatDuration(duration));
	}
	
	@Test
	public void testFormatSince() {
		String date = "2015-11-11T20:32:37Z";
		assertEquals("11/11/15 3:32:37 PM EST", formatSince(date, TimeZone.getTimeZone("EST")));
	}

}
