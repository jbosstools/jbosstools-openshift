/*******************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.details;

import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftResourceLabelUtils;
import org.jboss.tools.openshift.express.internal.ui.propertytable.AbstractPropertyTableContentProvider;
import org.jboss.tools.openshift.express.internal.ui.propertytable.ContainerElement;
import org.jboss.tools.openshift.express.internal.ui.propertytable.IProperty;
import org.jboss.tools.openshift.express.internal.ui.propertytable.StringElement;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 * @author Jeff Cantrill
 */
public class ApplicationDetailsContentProvider extends AbstractPropertyTableContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		List<IProperty> elements = new ArrayList<IProperty>();
		if (inputElement instanceof IApplication) {
			final IApplication application = (IApplication) inputElement;
			try {
				elements.add(
						new StringElement("Name",
								new ApplicationPropertyGetter(application) {
									@Override
									protected String doGet(IApplication application) {
										return application.getName();
									}
								}.safeGet()));
				elements.add(
						new StringElement("Public URL",
								new ApplicationPropertyGetter(application) {
									@Override
									protected String doGet(IApplication application) {
										return application.getApplicationUrl().toString();
									}
								}.safeGet(), true));
				elements.add(new StringElement("Type",
						new ApplicationPropertyGetter(application) {
							@Override
							protected String doGet(IApplication application) {
								return OpenShiftResourceLabelUtils.toString(application.getCartridge());
							}
						}.safeGet()));
				elements.add(
						new StringElement("Created on",
								new ApplicationPropertyGetter(application) {
									@Override
									protected String doGet(IApplication application) {
										return new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss").format(application
												.getCreationTime());
									}
								}.safeGet()));
				elements.add(new StringElement("UUID",
						new ApplicationPropertyGetter(application) {
							@Override
							protected String doGet(IApplication application) {
								return application.getUUID();
							}
						}.safeGet()));
				elements.add(new StringElement("Git URL",
						new ApplicationPropertyGetter(application) {
							@Override
							protected String doGet(IApplication application) {
								return application.getGitUrl();
							}
						}.safeGet()));
				elements.add(new StringElement("SSH Connection",
						new ApplicationPropertyGetter(application) {
							@Override
							protected String doGet(IApplication application) {
								String gitUrl = application.getGitUrl();
								return new StringBuilder(EGitUtils.getGitUsername(gitUrl))
										.append('@')
										.append(EGitUtils.getGitHost(gitUrl))
										.toString();
							}
						}.safeGet()));
				elements.add(new StringElement("Scalable",
						new ApplicationPropertyGetter(application) {
							@Override
							protected String doGet(IApplication application) {
								return application.getApplicationScale().getValue();
							}
						}.safeGet()));
				elements.add(
						createCartridges(new ContainerElement("Cartridges"), application));

			} catch (Exception e) {
				Logger.error(
						NLS.bind("Could not display details for OpenShift application {0}", application.getName()), e);
			}
		}
		return elements.toArray();
	}

	private ContainerElement createCartridges(ContainerElement cartridgesContainer, IApplication application)
			throws OpenShiftException, SocketTimeoutException {
		for (IEmbeddedCartridge cartridge : application.getEmbeddedCartridges()) {
			cartridgesContainer.add(
					new StringElement(
							cartridge.getName(),
							new ApplicationPropertyGetter(application) {
								@Override
								protected String doGet(IApplication application) {
									return application.getApplicationScale().getValue();
								}
							}.safeGet(), true));
		}
		return cartridgesContainer;
	}

	private abstract static class ApplicationPropertyGetter {

		private IApplication application;

		ApplicationPropertyGetter(IApplication application) {
			this.application = application;
		}

		protected abstract String doGet(IApplication application);

		public String safeGet() {
			try {
				return doGet(application);
			} catch (Exception e) {
				Logger.error(
						NLS.bind("Could not display details for OpenShift application {0}", application.getName()), e);
				return "<could not get property>";
			}
		};
	}
}
