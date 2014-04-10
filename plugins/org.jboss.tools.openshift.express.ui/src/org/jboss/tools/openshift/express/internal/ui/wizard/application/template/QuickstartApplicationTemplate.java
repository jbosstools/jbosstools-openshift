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

import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.IQuickstart;

/**
 * @author Andre Dietisheim
 */
public class QuickstartApplicationTemplate extends AbstractApplicationTemplate implements IQuickstartApplicationTemplate {

	private IQuickstart quickstart;

	public QuickstartApplicationTemplate(IQuickstart quickstart) {
		super(quickstart.getName(), quickstart.getSummary());
		this.quickstart = quickstart;
	}

	@Override
	public IQuickstart getQuickstart() {
		return quickstart;
	}
	
	@Override
	public String getLanguage() {
		return quickstart.getLanguage();
	}
	
	@Override
	public String getHref() {
		return quickstart.getHref();
	}

	@Override
	public boolean isOpenShiftMaintained() {
		return "openshift".equals(StringUtils.toLowerCase(quickstart.getProvider()));
	}
	
	@Override
	public boolean isAutomaticSecurityUpdates() {
		return StringUtils.isEmpty(quickstart.getInitialGitUrl());
	}

	@Override
	public boolean isMatching(String expression) {
		boolean matching = super.isMatching(expression);
		if (matching) {
			return true;
		}

		return isMatchingTag(StringUtils.toLowerCase(expression));
	}
	
	private boolean isMatchingTag(String expression) {
		for (String tag : quickstart.getTags()) {
			if (isMatching(expression, tag)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean isQuickstart() {
		return true;
	}

}