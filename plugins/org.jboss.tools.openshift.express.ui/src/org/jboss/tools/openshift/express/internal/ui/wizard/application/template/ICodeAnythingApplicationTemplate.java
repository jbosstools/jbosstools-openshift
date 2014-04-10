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
package org.jboss.tools.openshift.express.internal.ui.wizard.application.template;


/**
 * @author Andre Dietisheim
 */
public interface ICodeAnythingApplicationTemplate extends ICartridgeApplicationTemplate {

	public static final String PROPERTY_CARTRIDGE_URL = "url";

	public String getUrl();

	public void setUrl(String url);
}