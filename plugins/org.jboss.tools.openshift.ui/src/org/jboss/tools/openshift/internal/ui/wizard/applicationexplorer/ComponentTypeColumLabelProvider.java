/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.Objects;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;

/**
 * @author Red Hat Developers
 *
 */
public class ComponentTypeColumLabelProvider extends ColumnLabelProvider {

	@Override
	public String getText(Object element) {
	  if (element instanceof ComponentType) {
	    String text = ((ComponentType)element).getName();
	    if (element instanceof DevfileComponentType) {
	      text += " (from " + ((DevfileComponentType)element).getDevfileRegistry().getName() + ")";
	    }
	    return text;
	  }
	  return Objects.toString(element);
	}
}
