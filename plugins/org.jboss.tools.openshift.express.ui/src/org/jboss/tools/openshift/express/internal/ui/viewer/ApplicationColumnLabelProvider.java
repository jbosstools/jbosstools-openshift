/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class ApplicationColumnLabelProvider extends ColumnLabelProvider {

    @Override
    public String getText(Object element) {
        if (!(element instanceof IApplication)) {
            return super.getText(element);
        }
        return ((IApplication)element).getName();
    }
}
