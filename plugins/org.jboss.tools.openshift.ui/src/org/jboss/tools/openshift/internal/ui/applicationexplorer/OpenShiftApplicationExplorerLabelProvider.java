/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
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
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.DevfileRegistriesElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.DevfileRegistryComponentTypeElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.DevfileRegistryComponentTypeStarterElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.DevfileRegistryElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.MessageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.NamespaceElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ServiceElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.URLElement;

/**
 * @author Red Hat Developers
 *
 */
public class OpenShiftApplicationExplorerLabelProvider extends BaseExplorerLabelProvider {

	@Override
	public StyledString getStyledText(Object element, int limit) {
		if (element instanceof ApplicationExplorerUIModel) {
			if (((ApplicationExplorerUIModel) element).getOdo() != null) {
				return style(((ApplicationExplorerUIModel) element).getOdo().getMasterUrl().toString(), "", limit);
			}
			return style("Loading", "", limit);
		} else if (element instanceof NamespaceElement) {
			return style(((NamespaceElement) element).getWrapped(), "", limit);
		} else if (element instanceof ComponentElement) {
			return style(((ComponentElement) element).getWrapped().getName(), ((ComponentElement) element).getWrapped().getLiveFeatures().toString(), limit);
		} else if (element instanceof URLElement) {
			String base = ((URLElement) element).getWrapped().getHost()+":"+((URLElement) element).getWrapped().getLocalPort();
			String qualified = ((URLElement) element).getWrapped().getName()+":"+((URLElement) element).getWrapped().getContainerPort();
			return style(base ,qualified, limit);
		} else if (element instanceof ServiceElement) {
			return style(((ServiceElement) element).getWrapped().getName(),
					((ServiceElement) element).getWrapped().getKind(), limit);
		} else if (element instanceof MessageElement) {
			return getStyledText((MessageElement<?>) element);
		} else if (element instanceof DevfileRegistriesElement) {
			return style("Devfile registries", "", limit);
		} else if (element instanceof DevfileRegistryElement) {
			return style(((DevfileRegistryElement) element).getWrapped().getName(),
					((DevfileRegistryElement) element).getWrapped().getURL(), limit);
		} else if (element instanceof DevfileRegistryComponentTypeElement) {
			return style(((DevfileRegistryComponentTypeElement) element).getWrapped().getDisplayName(),
					((DevfileRegistryComponentTypeElement) element).getWrapped().getDescription(), limit);
		} else if (element instanceof DevfileRegistryComponentTypeStarterElement) {
			return style(((DevfileRegistryComponentTypeStarterElement) element).getWrapped().getName(),
					((DevfileRegistryComponentTypeStarterElement) element).getWrapped().getDescription(), limit);
		}
		return super.getStyledText(element, limit);
	}

	private StyledString getStyledText(MessageElement<?> messageElement) {
		StyledString styledString = new StyledString();
		styledString.append(messageElement.getWrapped(), new Styler() {

			@Override
			public void applyStyles(TextStyle textStyle) {
				textStyle.underline = true;
				textStyle.underlineStyle = SWT.UNDERLINE_LINK;
			}
		});
		return styledString;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof ApplicationExplorerUIModel) {
			return OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_ICON_IMG;
		} else if (element instanceof NamespaceElement) {
			return OpenShiftImages.PROJECT_IMG;
		} else if (element instanceof ComponentElement) {
			return OpenShiftImages.COMPONENT_IMG;
		} else if (element instanceof ServiceElement) {
			return OpenShiftImages.SERVICE_IMG;
		} else if (element instanceof URLElement) {
			return OpenShiftImages.URL_IMG;
		} else if (element instanceof DevfileRegistriesElement || element instanceof DevfileRegistryElement) {
			return OpenShiftImages.REGISTRY_IMG;
		} else if (element instanceof DevfileRegistryComponentTypeElement) {
			return OpenShiftImages.COMPONENT_TYPE_IMG;
		} else if (element instanceof DevfileRegistryComponentTypeStarterElement) {
			return OpenShiftImages.STARTER_IMG;
		}
		return super.getImage(element);
	}

}
