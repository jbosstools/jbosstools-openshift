/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.viewer;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.utils.GTK3Utils;

/**
 * A styled cell labelprovider that works around gtk3 issues by disabling owner draw
 * 
 * @author Andre Dietisheim
 * 
 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=466499
 * @see https://issues.jboss.org/browse/JBIDE-19853
 * @see https://issues.jboss.org/browse/JBIDE-19932

 */
public abstract class GTK3WorkaroundStyledCellLabelProvider extends StyledCellLabelProvider {

	protected GTK3WorkaroundStyledCellLabelProvider() {
		if (GTK3Utils.isRunning()) {
			setOwnerDrawEnabled(false);
		}
	}
}
