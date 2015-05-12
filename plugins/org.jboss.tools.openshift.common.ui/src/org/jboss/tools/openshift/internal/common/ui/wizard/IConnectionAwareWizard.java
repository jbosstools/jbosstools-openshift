/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.wizard;

import org.eclipse.jface.wizard.IWizard;
import org.jboss.tools.openshift.common.core.connection.IConnection;

/**
 * @author Andre Dietisheim
 */
public interface IConnectionAwareWizard<C extends IConnection> extends IConnectionAware<C>, IWizard {

}