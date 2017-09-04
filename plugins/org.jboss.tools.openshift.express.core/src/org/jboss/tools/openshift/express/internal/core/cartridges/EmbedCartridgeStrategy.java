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
package org.jboss.tools.openshift.express.internal.core.cartridges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.util.CartridgeToStringConverter;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

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

    private final ApplicationRequirement[] applicationRequirements = new ApplicationRequirement[] {
            new JBossApplicationRequirement(new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_SWITCHYARD)),
            new NonScalableApplicationRequirement(new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_PHPMYADMIN)) };

    private final EmbeddableCartridgeRelations[] cartridgeDependencies = new EmbeddableCartridgeRelations[] {
            new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_JENKINS_CLIENT), null, null,
                    new CartridgeSelector(IStandaloneCartridge.NAME_JENKINS)),
            new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_PHPMYADMIN), null,
                    new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_MYSQL), null),
            new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_ROCKMONGO), null,
                    new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_MONGODB), null),
            new EmbeddableCartridgeRelations(new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_10GEN_MMS_AGENT), null,
                    new EmbeddableCartridgeSelector(IEmbeddableCartridge.NAME_MONGODB), null) };

    private Map<ICartridge, EmbeddableCartridgeRelations> dependenciesByCartridge;
    private HashMap<ICartridge, Set<ICartridge>> dependantsByCartridge;

    private Collection<ICartridge> allEmbeddableCartridges;
    private Collection<ICartridge> allStandaloneCartridges;
    private Collection<IApplication> allApplications;

    public EmbedCartridgeStrategy(Collection<ICartridge> allEmbeddableCartridges, Collection<ICartridge> allStandaloneCartridges,
            Collection<IApplication> allApplications) {
        this.allEmbeddableCartridges = allEmbeddableCartridges;
        this.allStandaloneCartridges = allStandaloneCartridges;
        this.allApplications = allApplications;
        initDependencyMaps(allEmbeddableCartridges, allStandaloneCartridges, cartridgeDependencies);
    }

    private void initDependencyMaps(Collection<ICartridge> allEmbeddableCartridges, Collection<ICartridge> allCartridges,
            EmbeddableCartridgeRelations... dependencies) {
        this.dependenciesByCartridge = new HashMap<>();

        this.dependantsByCartridge = new HashMap<>();
        for (EmbeddableCartridgeRelations dependency : dependencies) {
            createDependency(allEmbeddableCartridges, dependency);
            createDependants(allEmbeddableCartridges, dependency);
        }
    }

    protected void createDependants(Collection<ICartridge> allEmbeddableCartridges, EmbeddableCartridgeRelations dependency) {
        Set<ICartridge> dependants = dependantsByCartridge.get(dependency.getRequired(allEmbeddableCartridges));
        if (dependants == null) {
            ICartridge dependantCartridge = dependency.getRequired(allEmbeddableCartridges);
            if (dependantCartridge != null) {
                dependantsByCartridge.put(dependantCartridge, dependants = new HashSet<>());
            }
        }
        if (dependants != null) {
            dependants.add(dependency.getSubject(allEmbeddableCartridges));
        }
    }

    protected void createDependency(Collection<ICartridge> allEmbeddableCartridges, EmbeddableCartridgeRelations dependency) {
        ICartridge requiringCartridge = dependency.getSubject(allEmbeddableCartridges);
        dependenciesByCartridge.put(requiringCartridge, dependency);
    }

    public ApplicationRequirement getMissingRequirement(ICartridge requestedCartridge, IApplicationProperties application) {
        for (ApplicationRequirement requirement : applicationRequirements) {
            if (requirement.isForCartridge(requestedCartridge) && !requirement.meetsRequirements(application)) {
                return requirement;
            }
        }
        return null;
    }

    public EmbeddableCartridgeDiff add(ICartridge cartridge, Set<ICartridge> currentCartridges) throws OpenShiftException {
        EmbeddableCartridgeDiff cartridgeDiff = new EmbeddableCartridgeDiff(cartridge);
        add(cartridge, currentCartridges, cartridgeDiff, allEmbeddableCartridges, allStandaloneCartridges, allApplications);
        return cartridgeDiff;
    }

    private void add(ICartridge cartridge, Set<ICartridge> currentCartridges, EmbeddableCartridgeDiff diff,
            Collection<ICartridge> allEmbeddableCartridges, Collection<ICartridge> allStandaloneCartridges,
            Collection<IApplication> allApplications) throws OpenShiftException {
        EmbeddableCartridgeRelations relation = dependenciesByCartridge.get(cartridge);
        if (relation == null) {
            return;
        }
        removeConflicting(currentCartridges, diff, relation, allEmbeddableCartridges);
        addRequired(currentCartridges, diff, relation.getRequired(allEmbeddableCartridges), allEmbeddableCartridges);
        addRequiredApplication(diff, relation, allStandaloneCartridges, allApplications);
    }

    private void addRequired(Set<ICartridge> currentCartridges, EmbeddableCartridgeDiff diff, ICartridge requiredCartridge,
            Collection<ICartridge> allEmbeddableCartridges) throws OpenShiftException {
        if (requiredCartridge != null && !currentCartridges.contains(requiredCartridge)) {
            // recurse
            add(requiredCartridge, currentCartridges, diff, allEmbeddableCartridges, allStandaloneCartridges, allApplications);
            diff.addAddition(requiredCartridge);
        }
    }

    private void addRequiredApplication(EmbeddableCartridgeDiff diff, EmbeddableCartridgeRelations relation,
            Collection<ICartridge> allStandaloneCartridges, Collection<IApplication> allApplications) throws OpenShiftException {
        ICartridge requiredCartridge = relation.getRequiredApplication(allStandaloneCartridges);
        if (requiredCartridge != null && !containsApplicationByCartridge(requiredCartridge, allApplications)) {
            diff.addApplicationAddition(requiredCartridge);
        }
    }

    private boolean containsApplicationByCartridge(ICartridge cartridge, Collection<IApplication> applications) {
        for (IApplication application : applications) {
            if (cartridge.equals(application.getCartridge())) {
                return true;
            }
        }
        return false;
    }

    private void removeConflicting(Set<ICartridge> currentCartridges, EmbeddableCartridgeDiff cartridgeDiff,
            EmbeddableCartridgeRelations relation, Collection<ICartridge> allEmbeddableCartridges) throws OpenShiftException {
        ICartridge conflictingCartridge = relation.getConflicting(allEmbeddableCartridges);
        if (conflictingCartridge != null) {
            remove(conflictingCartridge, currentCartridges, cartridgeDiff);
            if (currentCartridges.contains(conflictingCartridge)) {
                cartridgeDiff.addRemoval(conflictingCartridge);
            }
        }
    }

    public EmbeddableCartridgeDiff remove(ICartridge cartridge, Set<ICartridge> currentCartridges) throws OpenShiftException {
        EmbeddableCartridgeDiff cartridgeDiff = new EmbeddableCartridgeDiff(cartridge);
        remove(cartridge, currentCartridges, cartridgeDiff);
        return cartridgeDiff;
    }

    private void remove(ICartridge cartridge, Set<ICartridge> currentCartridges, EmbeddableCartridgeDiff cartridgeDiff)
            throws OpenShiftException {
        Set<ICartridge> dependantCartridges = dependantsByCartridge.get(cartridge);
        if (dependantCartridges == null) {
            return;
        }
        for (ICartridge dependantCartridge : dependantCartridges) {
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

        protected EmbeddableCartridgeRelations(EmbeddableCartridgeSelector cartridgeSelector, EmbeddableCartridgeSelector conflicting,
                EmbeddableCartridgeSelector required, CartridgeSelector requiredApplication) {
            this.subject = cartridgeSelector;
            this.conflicting = conflicting;
            this.required = required;
            this.requiredApplication = requiredApplication;
        }

        protected ICartridge getSubject(Collection<ICartridge> allEmbeddableCartridges) {
            return subject.getCartridge(allEmbeddableCartridges);
        }

        protected ICartridge getConflicting(Collection<ICartridge> allEmbeddableCartridges) {
            if (conflicting == null) {
                return null;
            }
            return conflicting.getCartridge(allEmbeddableCartridges);
        }

        protected ICartridge getRequired(Collection<ICartridge> allEmbeddableCartridges) {
            if (required == null) {
                return null;
            }
            return required.getCartridge(allEmbeddableCartridges);
        }

        protected ICartridge getRequiredApplication(Collection<ICartridge> allCartridges) {
            if (requiredApplication == null) {
                return null;
            }
            return requiredApplication.getCartridge(allCartridges);
        }
    }

    public static abstract class ApplicationRequirement {

        private EmbeddableCartridgeSelector cartridge;

        protected ApplicationRequirement(EmbeddableCartridgeSelector cartridgeSelector) {
            this.cartridge = cartridgeSelector;
        }

        public boolean isForCartridge(ICartridge requestedCartridge) {
            return cartridge.matches(requestedCartridge);
        }

        public String getCartridgeName() {
            return cartridge.getName();
        }

        protected abstract boolean meetsRequirements(IApplicationProperties application);

        public abstract String getMessage(ICartridge requestedCartridge, IApplicationProperties application);
    }

    private static class NonScalableApplicationRequirement extends ApplicationRequirement {

        protected NonScalableApplicationRequirement(EmbeddableCartridgeSelector cartridgeSelector) {
            super(cartridgeSelector);
        }

        @Override
        protected boolean meetsRequirements(IApplicationProperties application) {
            ApplicationScale scale = application.getApplicationScale();
            return scale == ApplicationScale.NO_SCALE || scale == null;
        }

        @Override
        public String getMessage(ICartridge requestedCartridge, IApplicationProperties application) {
            return NLS.bind(
                    "It is not recommended to add cartridge {0} to your application {1}."
                            + " The cartridge cannot scale and requires a non-scalable application. " + "Your application is scalable.",
                    requestedCartridge.getName(), application.getApplicationName());
        }
    }

    private static class JBossApplicationRequirement extends ApplicationRequirement {

        private CartridgeSelector eapSelector;
        private CartridgeSelector asSelector;

        protected JBossApplicationRequirement(EmbeddableCartridgeSelector cartridgeSelector) {
            super(cartridgeSelector);
            this.eapSelector = new CartridgeSelector(IStandaloneCartridge.NAME_JBOSSEAP);
            this.asSelector = new CartridgeSelector(IStandaloneCartridge.NAME_JBOSSAS);
        }

        @Override
        protected boolean meetsRequirements(IApplicationProperties application) {
            ICartridge standaloneCartridge = application.getStandaloneCartridge();
            return eapSelector.isMatching(standaloneCartridge) || asSelector.isMatching(standaloneCartridge);
        }

        @Override
        public String getMessage(ICartridge requestedCartridge, IApplicationProperties application) {
            ICartridge standaloneCartridge = application.getStandaloneCartridge();
            return NLS.bind(
                    "It is not recommended to add cartridge {0} to your application {1}."
                            + " The cartridge requires a {3} or {4} application and your application is a {2}.",
                    new String[] { requestedCartridge.getName(), application.getApplicationName(),
                            standaloneCartridge == null ? "'unknwon'" : standaloneCartridge.getName(), eapSelector.getName(),
                            asSelector.getName() });
        }

    }

    public static class EmbeddableCartridgeDiff {

        private List<ICartridge> removals;
        private List<ICartridge> additions;
        private List<ICartridge> applicationAdditions;
        private ICartridge cartridge;

        protected EmbeddableCartridgeDiff(ICartridge cartridge) {
            this.cartridge = cartridge;
            this.removals = new ArrayList<>();
            this.additions = new ArrayList<>();
            this.applicationAdditions = new ArrayList<>();
        }

        public ICartridge getCartridge() {
            return cartridge;
        }

        public List<ICartridge> getAdditions() {
            return additions;
        }

        public boolean hasAdditions() {
            return getAdditions().size() > 0;
        }

        protected void addAddition(ICartridge cartridge) {
            additions.add(cartridge);
        }

        public List<ICartridge> getRemovals() {
            return removals;
        }

        public boolean hasRemovals() {
            return getRemovals().size() > 0;
        }

        protected void addRemoval(ICartridge cartridge) {
            removals.add(cartridge);
        }

        protected void addApplicationAddition(ICartridge standaloneCartridge) {
            applicationAdditions.add(standaloneCartridge);
        }

        public List<ICartridge> getApplicationAdditions() {
            return applicationAdditions;
        }

        public boolean hasApplicationAdditions() {
            return getApplicationAdditions().size() > 0;
        }

        public boolean hasChanges() {
            return hasApplicationAdditions() || hasRemovals() || hasAdditions();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (hasApplicationAdditions()) {
                builder.append(NLS.bind("- Create {0}", StringUtils.toString(getApplicationAdditions(), new CartridgeToStringConverter())));
            }
            if (hasRemovals()) {
                builder.append(NLS.bind("\n- Remove {0}", StringUtils.toString(getRemovals(), new CartridgeToStringConverter())));

            }
            if (hasAdditions()) {
                builder.append(NLS.bind("\n- Add {0}", StringUtils.toString(getAdditions(), new CartridgeToStringConverter())));
            }
            return builder.toString();
        }
    }

    private static class EmbeddableCartridgeSelector {

        private String nameStartsWith;

        private EmbeddableCartridgeSelector(String nameStartsWith) {
            this.nameStartsWith = nameStartsWith;
        }

        public ICartridge getCartridge(Collection<ICartridge> embeddableCartridges) {
            if (embeddableCartridges == null || embeddableCartridges.isEmpty()) {
                return null;
            }

            for (ICartridge cartridge : embeddableCartridges) {
                if (matches(cartridge)) {
                    return cartridge;
                }
            }
            return null;
        }

        private boolean matches(ICartridge cartridge) {
            return cartridge.getName() != null && cartridge.getName().startsWith(nameStartsWith);
        }

        public String getName() {
            return nameStartsWith;
        }
    }

    private static class CartridgeSelector {

        private String nameStartsWith;

        private CartridgeSelector(String nameStartsWith) {
            this.nameStartsWith = nameStartsWith;
        }

        public ICartridge getCartridge(Collection<ICartridge> cartridges) {
            if (cartridges == null || cartridges.isEmpty()) {
                return null;
            }

            for (ICartridge cartridge : cartridges) {
                if (isMatching(cartridge)) {
                    return cartridge;
                }
            }
            return null;
        }

        public boolean isMatching(ICartridge cartridge) {
            return cartridge != null && cartridge.getName() != null && cartridge.getName().startsWith(nameStartsWith);
        }

        public String getName() {
            return nameStartsWith;
        }
    }

    /**
     * Provides properties for a (new or existing)  application
     */
    public interface IApplicationProperties {

        public ApplicationScale getApplicationScale();

        public ICartridge getStandaloneCartridge();

        public String getApplicationName();
    }
}
