/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.propertytable;

public class StringElement extends ContainerElement {

	private final String value;
	private final boolean isLink;

	public StringElement(String property, String value) {
		this(property, value, false, null);
	}
	
	public StringElement(String property, String value, ContainerElement parent) {
		this(property, value, false, parent);
	}

	public StringElement(String property, String value, boolean isLink) {
		this(property, value, isLink, null);
	}

	public StringElement(String property, String value, boolean isLink, ContainerElement parent) {
		super(property, parent);
		this.value = value;
		this.isLink = isLink;
	}

	public final String getValue() {
		return value;
	}

	public boolean isLink() {
		return isLink;
	}
}