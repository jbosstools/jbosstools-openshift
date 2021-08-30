/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface OperatorCRD {
    String getName();
    String getVersion();
    String getKind();
    String getDisplayName();
    String getDescription();
    JsonNode getSample();
    JsonNode getSchema();
    List<OperatorCRDSpecDescriptor> getSpecDescriptors();
}
