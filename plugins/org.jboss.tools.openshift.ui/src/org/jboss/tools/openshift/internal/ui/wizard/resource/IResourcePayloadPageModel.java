/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.ui.wizard.common.IProjectPageModel;

/**
 * @author Jeff Maury
 *
 */
public interface IResourcePayloadPageModel extends IProjectPageModel<IConnection> {
    public static final String PROPERTY_SOURCE = "source";
    
    /**
     * Set the source. The source can be a file path or an URL.
     * 
     * @param source the source
     */
    void setSource(String source);
    
    /**
     * Gets the source. The source can be a file path or an URL.
     * 
     * @return the source
     */
    String getSource();
}
