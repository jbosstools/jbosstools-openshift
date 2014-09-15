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
package org.jboss.tools.openshift.express.test.ui.wizard.application.details;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.tools.openshift.express.internal.ui.propertytable.IProperty;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.details.ApplicationDetailsContentProvider;
import org.jboss.tools.openshift.express.test.core.ApplicationDetailsFake;
import org.junit.Test;

import com.openshift.client.IApplication;

/**
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public class ApplicationDetailsContentProviderTest {

	@Test
	public void testGetElements() {
		IApplication app = new ApplicationDetailsFake();
		ApplicationDetailsContentProvider provider = new ApplicationDetailsContentProvider();

		String[] exp = { "Name", "Public URL", "Type", "Created on", "UUID", "Git URL", "SSH Connection", "Scalable",
				"Cartridges" };
		assertApplicationDetails(exp, provider.getElements(app));
	}

	private void assertApplicationDetails(String[] exp, Object[] elements) {
		IProperty[] props = Arrays.copyOf(elements, elements.length, IProperty[].class);
		List<String> actual = new ArrayList<String>(props.length);
		for (int i = 0; i < props.length; i++) {
			actual.add(props[i].getName());
		}
		assertArrayEquals("Exp. the details to show the visible properties", exp, actual.toArray());
	}

}
