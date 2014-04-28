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

import java.util.List;

import com.openshift.client.IQuickstart;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public interface IQuickstartApplicationTemplate extends IApplicationTemplate {

	public IQuickstart getQuickstart();

	public String getLanguage();
	
	public String getHref();
	
	public String getInitialGitUrl();
	
	public List<ICartridge> getAlternativesFor(ICartridge cartridge);
	
	public boolean isOpenShiftMaintained();
	
	public boolean isAutomaticSecurityUpdates();

}