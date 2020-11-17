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

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.internal.resources.ComputeProjectOrder;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.jboss.tools.openshift.core.odo.ComponentKind;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.S2iComponentType;

/**
 * @author Red Hat Developers
 *
 */
public class ComponentTypeContentProvider implements ITreeContentProvider {
  
  private final List<ComponentType> types;
  
  private final String S2I_NODE_LABEL = "S2I";
  
  public ComponentTypeContentProvider(List<ComponentType> types) {
    this.types = types;
  }
  
  @Override
  public Object[] getElements(Object inputElement) {
    if (inputElement == types) {
      boolean hasS2I = types.stream().anyMatch(type -> type.getKind() == ComponentKind.S2I);
      if (!hasS2I) {
        return types.toArray();
      } else {
        return Stream.concat(types.stream().filter(type -> type.getKind() == ComponentKind.DEVFILE), Stream.ofNullable(S2I_NODE_LABEL)).toArray();
      }
    } else if (inputElement == S2I_NODE_LABEL) {
      return types.stream().filter(type -> type.getKind() == ComponentKind.S2I).toArray();
    }
    return null;
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    return getElements(parentElement);
  }

  @Override
  public Object getParent(Object element) {
    if (element == S2I_NODE_LABEL || element instanceof DevfileComponentType) {
      return types;
    } else if (element instanceof S2iComponentType) {
      return S2I_NODE_LABEL;
    }
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return element == types || element == S2I_NODE_LABEL;
  }
}
