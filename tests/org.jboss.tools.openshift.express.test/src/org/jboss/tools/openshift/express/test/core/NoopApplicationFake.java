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
package org.jboss.tools.openshift.express.test.core;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.jcraft.jsch.Session;
import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IApplicationPortForwarding;
import com.openshift.client.IDomain;
import com.openshift.client.IEnvironmentVariable;
import com.openshift.client.IGearGroup;
import com.openshift.client.IGearProfile;
import com.openshift.client.Messages;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftSSHOperationException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IDeployedStandaloneCartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Andre Dietisheim
 */
public class NoopApplicationFake implements IApplication {

    @Override
    public String getCreationLog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCreationLog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUUID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGitUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getApplicationUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApplicationScale getApplicationScale() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IGearProfile getGearProfile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IDeployedStandaloneCartridge getCartridge() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IEmbeddedCartridge addEmbeddableCartridge(ICartridge cartridge) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IEmbeddedCartridge> addEmbeddableCartridges(ICartridge... cartridges) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IEmbeddedCartridge> getEmbeddedCartridges() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasEmbeddedCartridge(String cartridgeName) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IEmbeddedCartridge getEmbeddedCartridge(String cartridgeName) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IEmbeddedCartridge getEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getCreationTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restart() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(boolean force) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean waitForAccessible(long timeout) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IDomain getDomain() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scaleDown() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scaleUp() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAlias(String string) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAliases() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAlias(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAlias(String alias) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSSHSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPortFowardingStarted() throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IApplicationPortForwarding> getForwardablePorts() throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IApplicationPortForwarding> startPortForwarding() throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IApplicationPortForwarding> stopPortForwarding() throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IApplicationPortForwarding> refreshForwardablePorts() throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getEnvironmentProperties() throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSSHSession(Session session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session getSSHSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Boolean> waitForAccessibleAsync(long timeout) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IEmbeddedCartridge> addEmbeddableCartridges(Collection<IEmbeddableCartridge> cartridge) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEmbeddedCartridges(Collection<IEmbeddableCartridge> cartridges) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<IGearGroup> getGearGroups() throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Messages getMessages() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getInitialGitUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IEnvironmentVariable addEnvironmentVariable(String arg0, String arg1) throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, IEnvironmentVariable> addEnvironmentVariables(Map<String, String> arg0) throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canGetEnvironmentVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUpdateEnvironmentVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IEnvironmentVariable getEnvironmentVariable(String arg0) throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IEnvironmentVariable updateEnvironmentVariable(String arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, IEnvironmentVariable> getEnvironmentVariables() throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSshUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasEnvironmentVariable(String arg0) throws OpenShiftSSHOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEnvironmentVariable(String name) throws OpenShiftException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEnvironmentVariable(IEnvironmentVariable arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEnvironmentVariableValue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDeploymentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String setDeploymentType(String deploymentType) {
        throw new UnsupportedOperationException();
    }
}