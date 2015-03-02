/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.ui.filters;

import static org.junit.Assert.*;

import org.jboss.tools.openshift.express.internal.ui.filters.SimplePropertyActionFilter;
import org.junit.Test;

/**
 * @author Jeff Cantrill
 */
public class SimplePropertyActionFilterTest {

	private SimplePropertyActionFilter filter = new SimplePropertyActionFilter();
	private TestType aType = new TestType();
	
	@Test
	public void nullTargetShouldReturnFalse() {
		assertFalse(filter.testAttribute(null, "", ""));
	}
	
	@Test
	public void targetWithoutPropertyShouldReturnFalse(){
		assertFalse(filter.testAttribute(aType, "missingProperty", "anyValue"));
	}
	
	@Test
	public void targetWithMatchingStringPropertyShouldReturnTrue(){
		assertTrue(filter.testAttribute(aType, "stringType", "aStringType"));
	}

	@Test
	public void targetWithNonMatchingStringPropertyShouldReturnFalse(){
		assertFalse(filter.testAttribute(aType, "stringType", "someOtherValue"));
	}
	
	@Test
	public void targetWithMachingEnumPropertyShouldReturnTrue(){
		assertTrue(filter.testAttribute(aType, "anEnum", "START"));
	}

	@SuppressWarnings("unused")
	public class TestType {
		
		public String getStringType(){
			return "aStringType";
		}
		
		public State getAnEnum(){
			return State.START;
		}
	}
	
	public enum State{
		START,
		STOP
	}
}
