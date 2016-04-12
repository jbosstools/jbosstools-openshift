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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.core.util.CollectionUtils;

import com.openshift.client.IQuickstart;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.internal.client.AlternativeCartridges;

/**
 * @author Andre Dietisheim
 */
public class QuickstartApplicationTemplate extends AbstractApplicationTemplate implements IQuickstartApplicationTemplate {

	private IQuickstart quickstart;
	private Set<ICartridge> cartridges;

	public QuickstartApplicationTemplate(IQuickstart quickstart) {
		super(quickstart.getName(), quickstart.getSummary());
		this.quickstart = quickstart;
		this.cartridges = getFirstAlternatives(quickstart.getSuitableCartridges());
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
	public String getPageUrl() {
		String href = quickstart.getHref();
		if (StringUtils.isEmpty(href)) {
			// OpenShift Enterprise has no href that points to the quickstart page.
			// use the website instead
			href = quickstart.getWebsite();
		}
		return href;
	}
	
	@Override
	public boolean hasPageUrl() {
		return !StringUtils.isEmpty(getPageUrl()); 
	}
	
	@Override
	public String getInitialGitUrl() {
		return quickstart.getInitialGitUrl();
	}

	@Override
	public List<ICartridge> getAlternativesFor(ICartridge cartridge) {
		return quickstart.getAlternativesFor(cartridge);
	}

	@Override
	public Set<ICartridge> getAllCartridges() {
		return cartridges;
	}

	protected HashSet<ICartridge> getFirstAlternatives(List<AlternativeCartridges> allAlternatives) {
		HashSet<ICartridge> cartridges = new HashSet<>();
		for (AlternativeCartridges alternatives : allAlternatives) {
			ICartridge firstAlternative = CollectionUtils.getFirstElement(alternatives.get());
			if (firstAlternative != null) {
				cartridges.add(firstAlternative);
			}
		}
		return cartridges;
	}

	@Override
	public Set<ICartridge> getEmbeddedCartridges() {
		return getAllCartridges();
	}
	
	@Override
	public String getName() {
		return new StringBuilder()
			.append(super.getName())
			.append(" (Quickstart)")
			.toString();
	}

	@Override
	public List<String> getTags() {
		if (quickstart == null) {
			return Collections.emptyList();
		}
		
		return quickstart.getTags();
	}
	
	@Override
	public String getTagsString() {
		return StringUtils.toString(getTags());
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
	public boolean canAddRemoveCartridges() {
		return false;
	}

	@Override
	public boolean isInitialGitUrlEditable() {
		return false;
	}
	
	@Override
	public boolean isCodeAnything() {
		return false;
	}


}
