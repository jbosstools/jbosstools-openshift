/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.utils;

import org.eclipse.core.runtime.IAdapterFactory;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;

/**
 * Adapter factory for creating resource process from Openshift resources.
 * 
 * @author Jeff Maury
 *
 */
public class ResourceProcessorAdapterFactory implements IAdapterFactory {

    private ResourceProcessor BASE_PROCESSOR;
    
    private ResourceProcessor DEPLOYMENT_CONFIG_PROCESSOR;

    private ResourceProcessor BUILD_CONFIG_PROCESSOR;

    private ResourceProcessor REPLICATION_CONTROLLER_PROCESSOR;
    
    private ResourceProcessor PROJECT_PROCESSOR;

    /**
     * Lazy loading
     * 
     * @return the base processor
     */
    private synchronized ResourceProcessor getBaseProcessor() {
        if (null == BASE_PROCESSOR) {
            BASE_PROCESSOR = new BaseResourceProcessor();
        }
        return BASE_PROCESSOR;
    }
    
    /**
     * Lazy loading
     * 
     * @return the deployment config processor
     */
    private synchronized ResourceProcessor getDeploymentConfigProcessor() {
        if (null == DEPLOYMENT_CONFIG_PROCESSOR) {
            DEPLOYMENT_CONFIG_PROCESSOR = new DeploymentConfigResourceProcessor();
        }
        return DEPLOYMENT_CONFIG_PROCESSOR;
    }

    /**
     * Lazy loading
     * 
     * @return the build config processor
     */
    private synchronized ResourceProcessor getBuildConfigProcessor() {
        if (null == BUILD_CONFIG_PROCESSOR) {
            BUILD_CONFIG_PROCESSOR = new BuildConfigResourceProcessor();
        }
        return BUILD_CONFIG_PROCESSOR;
    }

    /**
     * Lazy loading
     * 
     * @return the replication controller processor
     */
    private synchronized ResourceProcessor getReplicationControllerProcessor() {
        if (null == REPLICATION_CONTROLLER_PROCESSOR) {
            REPLICATION_CONTROLLER_PROCESSOR = new ReplicationControllerResourceProcessor();
        }
        return REPLICATION_CONTROLLER_PROCESSOR;
    }

    /**
     * Lazy loading
     * 
     * @return the project processor
     */
    private synchronized ResourceProcessor getProjectProcessor() {
        if (null == PROJECT_PROCESSOR) {
            PROJECT_PROCESSOR = new ProjectResourceProcessor();
        }
        return PROJECT_PROCESSOR;
    }

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        T adapter = null;
        if (adaptableObject instanceof IResource) {
            switch (((IResource)adaptableObject).getKind()) {
            case ResourceKind.DEPLOYMENT_CONFIG:
                adapter = (T) getDeploymentConfigProcessor();
                break;
            case ResourceKind.BUILD_CONFIG:
                adapter = (T) getBuildConfigProcessor();
                break;
            case ResourceKind.REPLICATION_CONTROLLER:
                adapter = (T) getReplicationControllerProcessor();
                break;
            case ResourceKind.PROJECT:
                adapter = (T) getProjectProcessor();
                break;
            default:
                adapter = (T) getBaseProcessor();
                break;
            }
        }
        return adapter;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return new Class[] {ResourceProcessor.class};
    }
}
