/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openshift.client.IApplication;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IStandaloneCartridge;
import com.openshift.client.OpenShiftException;

/**
 * A UI strategy that is able to add and remove embedded cartridges while
 * fullfilling requirements and resolving conflicts (ex. mutual exclusivity
 * etc.)
 * <p>
 * TODO: replaced this manual code by a generic dependency-tree analysis
 * mechanism as soon as OpenShift completed design of cartridge metamodel
 * 
 * @author Andre Dietisheim
 */
public class EmbedCartridgeStrategy {

	private final EmbeddableCartridgeRelations[] dependencies =
			new EmbeddableCartridgeRelations[] {
					new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector("jenkins-client-"),
							null, null, new CartridgeSelector("jenkins-")),
					new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector("phpmyadmin-"),
							null, new EmbeddableCartridgeSelector("mysql-"), null),
					new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector("rockmongo-"),
							null, new EmbeddableCartridgeSelector("mongodb-"), null),
					new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector("10gen-mms-agent-"),
							null, new EmbeddableCartridgeSelector("mongodb-"), null)
			};

	private Map<IEmbeddableCartridge, EmbeddableCartridgeRelations> dependenciesByCartridge;
	private HashMap<IEmbeddableCartridge, Set<IEmbeddableCartridge>> dependantsByCartridge;

	private Collection<IEmbeddableCartridge> allEmbeddableCartridges;
	private Collection<IStandaloneCartridge> allStandaloneCartridges;
	private Collection<IApplication> allApplications;

	public EmbedCartridgeStrategy(Collection<IEmbeddableCartridge> allEmbeddableCartridges,
			Collection<IStandaloneCartridge> allStandaloneCartridges, Collection<IApplication> allApplications) {
		this.allEmbeddableCartridges = allEmbeddableCartridges;
		this.allStandaloneCartridges = allStandaloneCartridges;
		this.allApplications = allApplications;
		initDependencyMaps(allEmbeddableCartridges, allStandaloneCartridges, dependencies);
	}

	private void initDependencyMaps(Collection<IEmbeddableCartridge> allEmbeddableCartridges,
			Collection<IStandaloneCartridge> allCartridges, EmbeddableCartridgeRelations... dependencies) {
		this.dependenciesByCartridge = new HashMap<IEmbeddableCartridge, EmbeddableCartridgeRelations>();

		this.dependantsByCartridge = new HashMap<IEmbeddableCartridge, Set<IEmbeddableCartridge>>();
		for (EmbeddableCartridgeRelations dependency : dependencies) {
			createDependency(allEmbeddableCartridges, dependency);
			createDependants(allEmbeddableCartridges, dependency);
		}
	}

	protected void createDependants(Collection<IEmbeddableCartridge> allEmbeddableCartridges,
			EmbeddableCartridgeRelations dependency) {
		Set<IEmbeddableCartridge> dependants = dependantsByCartridge.get(dependency.getRequired(allEmbeddableCartridges));
		if (dependants == null) {
			IEmbeddableCartridge dependantCartridge = dependency.getRequired(allEmbeddableCartridges); 
			if (dependantCartridge != null) {
				dependantsByCartridge.put(
						dependantCartridge,
						dependants = new HashSet<IEmbeddableCartridge>());
			}
		}
		if (dependants != null) {
			dependants.add(dependency.getSubject(allEmbeddableCartridges));
		}
	}

	protected void createDependency(Collection<IEmbeddableCartridge> allEmbeddableCartridges,
			EmbeddableCartridgeRelations dependency) {
		IEmbeddableCartridge requiringCartridge = dependency.getSubject(allEmbeddableCartridges);
		dependenciesByCartridge.put(requiringCartridge, dependency);
	}

	public EmbeddableCartridgeDiff add(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges)
			throws OpenShiftException {
		EmbeddableCartridgeDiff cartridgeDiff = new EmbeddableCartridgeDiff(cartridge);
		add(cartridge, currentCartridges, cartridgeDiff, allEmbeddableCartridges, allStandaloneCartridges, allApplications);
		return cartridgeDiff;
	}

	private void add(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges,
			EmbeddableCartridgeDiff diff, Collection<IEmbeddableCartridge> allEmbeddableCartridges,
			Collection<IStandaloneCartridge> allStandaloneCartridges, Collection<IApplication> allApplications)
			throws OpenShiftException {
		EmbeddableCartridgeRelations relation = dependenciesByCartridge.get(cartridge);
		if (relation == null) {
			return;
		}
		removeConflicting(currentCartridges, diff, relation, allEmbeddableCartridges);
		addRequired(currentCartridges, diff, relation.getRequired(allEmbeddableCartridges), allEmbeddableCartridges);
		addRequiredApplication(diff, relation, allStandaloneCartridges, allApplications);
	}

	private void addRequired(Set<IEmbeddableCartridge> currentCartridges, EmbeddableCartridgeDiff diff,
			IEmbeddableCartridge requiredCartridge, Collection<IEmbeddableCartridge> allEmbeddableCartridges) throws OpenShiftException {
		if (requiredCartridge != null
				&& !currentCartridges.contains(requiredCartridge)) {
			// recurse
			add(requiredCartridge, currentCartridges, diff, allEmbeddableCartridges, allStandaloneCartridges, allApplications);
			diff.addAddition(requiredCartridge);
		}
	}

	private void addRequiredApplication(EmbeddableCartridgeDiff diff,
			EmbeddableCartridgeRelations relation, Collection<IStandaloneCartridge> allStandaloneCartridges, Collection<IApplication> allApplications) throws OpenShiftException {
		IStandaloneCartridge requiredCartridge = relation.getRequiredApplication(allStandaloneCartridges);
		if (requiredCartridge != null
				&& !containsApplicationByCartridge(requiredCartridge, allApplications)) {
			diff.addApplicationAddition(requiredCartridge);
		}
	}

	private boolean containsApplicationByCartridge(IStandaloneCartridge cartridge, Collection<IApplication> applications) {
		for(IApplication application : applications) {
			if (cartridge.equals(application.getCartridge())) {
				return true;
			}
		}
		return false;
	}
	
	private void removeConflicting(Set<IEmbeddableCartridge> currentCartridges, EmbeddableCartridgeDiff cartridgeDiff,
			EmbeddableCartridgeRelations relation, Collection<IEmbeddableCartridge> allEmbeddableCartridges) throws OpenShiftException {
		IEmbeddableCartridge conflictingCartridge = relation.getConflicting(allEmbeddableCartridges);
		if (conflictingCartridge != null) {
			remove(conflictingCartridge, currentCartridges, cartridgeDiff);
			if (currentCartridges.contains(conflictingCartridge)) {
				cartridgeDiff.addRemoval(conflictingCartridge);
			}
		}
	}

	public EmbeddableCartridgeDiff remove(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges)
			throws OpenShiftException {
		EmbeddableCartridgeDiff cartridgeDiff = new EmbeddableCartridgeDiff(cartridge);
		remove(cartridge, currentCartridges, cartridgeDiff);
		return cartridgeDiff;
	}

	private void remove(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges,
			EmbeddableCartridgeDiff cartridgeDiff)
			throws OpenShiftException {
		Set<IEmbeddableCartridge> dependantCartridges = dependantsByCartridge.get(cartridge);
		if (dependantCartridges == null) {
			return;
		}
		for (IEmbeddableCartridge dependantCartridge : dependantCartridges) {
			if (currentCartridges.contains(dependantCartridge)) {
				remove(dependantCartridge, currentCartridges, cartridgeDiff);
				cartridgeDiff.addRemoval(dependantCartridge);
			}
		}
	}

	private static class EmbeddableCartridgeRelations {

		private EmbeddableCartridgeSelector subject;
		private EmbeddableCartridgeSelector conflicting;
		private EmbeddableCartridgeSelector required;
		private CartridgeSelector requiredApplication;

		protected EmbeddableCartridgeRelations(EmbeddableCartridgeSelector cartridgeSelector,
				EmbeddableCartridgeSelector conflicting, EmbeddableCartridgeSelector required, CartridgeSelector requiredApplication) {
			this.subject = cartridgeSelector;
			this.conflicting = conflicting;
			this.required = required;
			this.requiredApplication = requiredApplication;
		}

		protected IEmbeddableCartridge getSubject(Collection<IEmbeddableCartridge> allEmbeddableCartridges) {
			return subject.getCartridge(allEmbeddableCartridges);
		}

		protected IEmbeddableCartridge getConflicting(Collection<IEmbeddableCartridge> allEmbeddableCartridges) {
			if (conflicting == null) {
				return null;
			}
			return conflicting.getCartridge(allEmbeddableCartridges);
		}

		protected IEmbeddableCartridge getRequired(Collection<IEmbeddableCartridge> allEmbeddableCartridges) {
			if (required == null) {
				return null;
			}
			return required.getCartridge(allEmbeddableCartridges);
		}

		protected IStandaloneCartridge getRequiredApplication(Collection<IStandaloneCartridge> allCartridges) {
			if (requiredApplication == null) {
				return null;
			}
			return requiredApplication.getCartridge(allCartridges);
		}
	}

	public static class EmbeddableCartridgeDiff {

		private List<IEmbeddableCartridge> removals;
		private List<IEmbeddableCartridge> additions;
		private List<IStandaloneCartridge> applicationAdditions;
		private IEmbeddableCartridge cartridge;

		protected EmbeddableCartridgeDiff(IEmbeddableCartridge cartridge) {
			this.cartridge = cartridge;
			this.removals = new ArrayList<IEmbeddableCartridge>();
			this.additions = new ArrayList<IEmbeddableCartridge>();
			this.applicationAdditions = new ArrayList<IStandaloneCartridge>();
		}

		public IEmbeddableCartridge getCartridge() {
			return cartridge;
		}

		public List<IEmbeddableCartridge> getAdditions() {
			return additions;
		}

		public boolean hasAdditions() {
			return getAdditions().size() > 0;
		}

		protected void addAddition(IEmbeddableCartridge cartridge) {
			additions.add(cartridge);
		}

		public List<IEmbeddableCartridge> getRemovals() {
			return removals;
		}
		
		public boolean hasRemovals() {
			return getRemovals().size() > 0;
		}

		protected void addRemoval(IEmbeddableCartridge cartridge) {
			removals.add(cartridge);
		}

		protected void addApplicationAddition(IStandaloneCartridge applicationType) {
			applicationAdditions.add(applicationType);
		}

		public List<IStandaloneCartridge> getApplicationAdditions() {
			return applicationAdditions;
		}

		public boolean hasApplicationAdditions() {
			return getApplicationAdditions().size() > 0;
		}

		public boolean hasChanges() {
			return hasApplicationAdditions()
					|| hasRemovals()
					|| hasAdditions();
		}
	}

	private static class EmbeddableCartridgeSelector {

		private String nameStartsWith;

		private EmbeddableCartridgeSelector(String nameStartsWith) {
			this.nameStartsWith = nameStartsWith;
		}

		private IEmbeddableCartridge getCartridge(Collection<IEmbeddableCartridge> embeddableCartridges) {
			if (embeddableCartridges == null
					|| embeddableCartridges.isEmpty()) {
				return null;
			}
			
			for(IEmbeddableCartridge cartridge : embeddableCartridges) {
				if (cartridge.getName() != null
						&& cartridge.getName().startsWith(nameStartsWith)) {
					return cartridge;
				}
			}
			return null;
		}
		
	}
	
	private static class CartridgeSelector {

		private String nameStartsWith;

		private CartridgeSelector(String nameStartsWith) {
			this.nameStartsWith = nameStartsWith;
		}

		private IStandaloneCartridge getCartridge(Collection<IStandaloneCartridge> cartridges) {
			if (cartridges == null
					|| cartridges.isEmpty()) {
				return null;
			}
			
			for(IStandaloneCartridge cartridge : cartridges) {
				if (cartridge.getName() != null
						&& cartridge.getName().startsWith(nameStartsWith)) {
					return cartridge;
				}
			}
			return null;
		}
		
	}
}
