/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

/**
 * @author Jeff Maury
 */
public interface IProjectPageModel<C extends IConnection> extends IConnectionAware<C>, IProjectAware {
    void loadResources();
}