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
package org.jboss.tools.openshift.internal.ui.applicationexplorer;

import org.eclipse.jface.viewers.StyledString;
import org.jboss.tools.openshift.core.odo.KubernetesLabels;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ProjectElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ServiceElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.StorageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.URLElement;

/**
 * @author Red Hat Developers
 *
 */
public class OpenShiftApplicationExplorerLabelProvider extends BaseExplorerLabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof ApplicationExplorerUIModel) {
			return ((ApplicationExplorerUIModel)element).getClient().getMasterUrl().toString();
		}
		return super.getText(element);
	}

	@Override
	public StyledString getStyledText(Object element, int limit) {
		if (element instanceof ApplicationExplorerUIModel) {
			return style(((ApplicationExplorerUIModel)element).getClient().getMasterUrl().toString(), "", limit);
		} else if (element instanceof ProjectElement) {
			return style(((ProjectElement)element).getWrapped().getMetadata().getName(), "", limit);
		} else if (element instanceof ApplicationElement) {
			return style(((ApplicationElement)element).getWrapped().getName(), "", limit);
		} else if (element instanceof ComponentElement) {
			return style(((ComponentElement)element).getWrapped().getName(), "", limit);
		} else if (element instanceof StorageElement) {
			return style(((StorageElement)element).getWrapped().getName(), "", limit);
		} else if (element instanceof URLElement) {
			return style(((URLElement)element).getWrapped().getName(),((URLElement)element).getWrapped().getPort(), limit);
		} else if (element instanceof ServiceElement) {
			return style(KubernetesLabels.getComponentName(((ServiceElement)element).getWrapped()), "", limit);
		}
		return super.getStyledText(element, limit);
	}
	
	
	
	

}
