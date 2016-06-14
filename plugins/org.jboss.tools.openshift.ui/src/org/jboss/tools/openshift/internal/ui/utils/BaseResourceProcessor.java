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

/**
 * Common base class for processor deleting linked resources.
 * 
 * @author Jeff Maury
 *
 */
public class BaseResourceProcessor implements ResourceProcessor {
    private boolean willCascadeDeleteLinkedResources;
    
    public BaseResourceProcessor() {
        this(false);
    }
    
    public BaseResourceProcessor(boolean willCascadeDeleteLinkedResources) {
        this.willCascadeDeleteLinkedResources = willCascadeDeleteLinkedResources;
    }
    
    @Override
    public boolean willCascadeDeleteLinkedResources() {
        return willCascadeDeleteLinkedResources;
    }
}
