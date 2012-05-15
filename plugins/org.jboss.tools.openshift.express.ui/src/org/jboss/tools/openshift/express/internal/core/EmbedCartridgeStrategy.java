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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
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

	private static final EmbeddableCartridgeRelations[] dependencies =
			new EmbeddableCartridgeRelations[] {
					new EmbeddableCartridgeRelations(IEmbeddableCartridge.JENKINS_14,
							null, null, ICartridge.JENKINS_14),
					new EmbeddableCartridgeRelations(IEmbeddableCartridge.PHPMYADMIN_34,
							null, IEmbeddableCartridge.MYSQL_51, null),
					new EmbeddableCartridgeRelations(IEmbeddableCartridge.ROCKMONGO_11,
							null, IEmbeddableCartridge.MONGODB_20, null),
					new EmbeddableCartridgeRelations(IEmbeddableCartridge._10GEN_MMS_AGENT_01,
							null, IEmbeddableCartridge.MONGODB_20, null),
					new EmbeddableCartridgeRelations(IEmbeddableCartridge.POSTGRESQL_84,
							IEmbeddableCartridge.MYSQL_51, null, null),
					new EmbeddableCartridgeRelations(IEmbeddableCartridge.MYSQL_51,
							IEmbeddableCartridge.POSTGRESQL_84, null, null),
			};

	private Map<IEmbeddableCartridge, EmbeddableCartridgeRelations> dependenciesByCartridge;
	private HashMap<IEmbeddableCartridge, Set<IEmbeddableCartridge>> dependantsByCartridge;

	private IApplication application;

	public EmbedCartridgeStrategy(IApplication application) {
		this.application = application;
		initDependencyMaps(dependencies);
	}

	private void initDependencyMaps(EmbeddableCartridgeRelations... dependencies) {
		this.dependenciesByCartridge = new HashMap<IEmbeddableCartridge, EmbeddableCartridgeRelations>();

		this.dependantsByCartridge = new HashMap<IEmbeddableCartridge, Set<IEmbeddableCartridge>>();
		for (EmbeddableCartridgeRelations dependency : dependencies) {
			dependenciesByCartridge.put(dependency.getSubject(), dependency);
			Set<IEmbeddableCartridge> dependants = getDependants(dependency);
			dependants.add(dependency.getSubject());
		}
	}

	private Set<IEmbeddableCartridge> getDependants(EmbeddableCartridgeRelations relation) {
		Set<IEmbeddableCartridge> dependants = dependantsByCartridge.get(relation.getRequired());
		if (dependants == null) {
			dependantsByCartridge.put(
					relation.getRequired(),
					dependants = new HashSet<IEmbeddableCartridge>());
		}
		return dependants;
	}

	public EmbeddableCartridgeDiff add(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges)
			throws OpenShiftException, SocketTimeoutException {
		EmbeddableCartridgeDiff cartridgeDiff = new EmbeddableCartridgeDiff(cartridge);
		add(cartridge, currentCartridges, cartridgeDiff);
		return cartridgeDiff;
	}

	private void add(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges,
			EmbeddableCartridgeDiff diff)
			throws OpenShiftException, SocketTimeoutException {
		EmbeddableCartridgeRelations relation = dependenciesByCartridge.get(cartridge);
		if (relation == null) {
			return;
		}
		removeConflicting(currentCartridges, diff, relation);
		addRequired(currentCartridges, diff, relation.getRequired());
		addRequiredApplication(diff, relation);
	}

	private void addRequired(Set<IEmbeddableCartridge> currentCartridges, EmbeddableCartridgeDiff diff,
			IEmbeddableCartridge requiredCartridge) throws OpenShiftException,
			SocketTimeoutException {
		if (requiredCartridge != null
				&& !currentCartridges.contains(requiredCartridge)) {
			// recurse
			add(requiredCartridge, currentCartridges, diff);
			diff.addAddition(requiredCartridge);
		}
	}

	private void addRequiredApplication(EmbeddableCartridgeDiff diff,
			EmbeddableCartridgeRelations relation) throws OpenShiftException {
		if (relation.getRequiredApplication() != null
				&& !application.getDomain().hasApplicationByCartridge(relation.getRequiredApplication())) {
			diff.addApplicationAddition(relation.getRequiredApplication());
		}
	}

	private void removeConflicting(Set<IEmbeddableCartridge> currentCartridges, EmbeddableCartridgeDiff cartridgeDiff,
			EmbeddableCartridgeRelations relation) throws OpenShiftException, SocketTimeoutException {
		IEmbeddableCartridge conflictingCartridge = relation.getConflicting();
		if (conflictingCartridge != null) {
			remove(conflictingCartridge, currentCartridges, cartridgeDiff);
			if (currentCartridges.contains(conflictingCartridge)) {
				cartridgeDiff.addRemoval(conflictingCartridge);
			}
		}
	}

	public EmbeddableCartridgeDiff remove(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges)
			throws OpenShiftException, SocketTimeoutException {
		EmbeddableCartridgeDiff cartridgeDiff = new EmbeddableCartridgeDiff(cartridge);
		remove(cartridge, currentCartridges, cartridgeDiff);
		return cartridgeDiff;
	}

	private void remove(IEmbeddableCartridge cartridge, Set<IEmbeddableCartridge> currentCartridges,
			EmbeddableCartridgeDiff cartridgeDiff)
			throws OpenShiftException, SocketTimeoutException {
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

		private IEmbeddableCartridge conflicting;
		private IEmbeddableCartridge required;
		private IEmbeddableCartridge subject;
		private ICartridge requiredApplication;

		protected EmbeddableCartridgeRelations(IEmbeddableCartridge cartridge,
				IEmbeddableCartridge conflicting, IEmbeddableCartridge required, ICartridge requiredApplication) {
			this.subject = cartridge;
			this.conflicting = conflicting;
			this.required = required;
			this.requiredApplication = requiredApplication;
		}

		protected IEmbeddableCartridge getSubject() {
			return subject;
		}

		protected IEmbeddableCartridge getConflicting() {
			return conflicting;
		}

		protected IEmbeddableCartridge getRequired() {
			return required;
		}

		protected ICartridge getRequiredApplication() {
			return requiredApplication;
		}

	}

	public static class EmbeddableCartridgeDiff {

		private List<IEmbeddableCartridge> removals;
		private List<IEmbeddableCartridge> additions;
		private List<ICartridge> applicationAdditions;
		private IEmbeddableCartridge cartridge;

		protected EmbeddableCartridgeDiff(IEmbeddableCartridge cartridge) {
			this.cartridge = cartridge;
			this.removals = new ArrayList<IEmbeddableCartridge>();
			this.additions = new ArrayList<IEmbeddableCartridge>();
			this.applicationAdditions = new ArrayList<ICartridge>();
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

		protected void addApplicationAddition(ICartridge applicationType) {
			applicationAdditions.add(applicationType);
		}

		public List<ICartridge> getApplicationAdditions() {
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

}
