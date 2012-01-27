/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.text.SimpleDateFormat;

import org.jboss.tools.openshift.express.internal.ui.propertytable.AbstractPropertyTableContentProvider;
import org.jboss.tools.openshift.express.internal.ui.propertytable.ContainerElement;
import org.jboss.tools.openshift.express.internal.ui.propertytable.StringElement;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 * 
 */
public class ApplicationDetailsContentProvider extends AbstractPropertyTableContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		Object[] elements = null;
		if (inputElement instanceof IApplication) {
			try {
				IApplication application = (IApplication) inputElement;
				final ContainerElement infoContainer = new ContainerElement("info", null);
				infoContainer.add(new StringElement("Name", application.getName(), infoContainer));
				infoContainer.add(new StringElement("Public URL", application.getApplicationUrl().toString(), true,
						infoContainer));
				infoContainer.add(new StringElement("Type", application.getCartridge().getName(), infoContainer));
				final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss");
				infoContainer.add(
						new StringElement("Created on", format.format(application.getCreationTime()), infoContainer));
				infoContainer.add(new StringElement("UUID", application.getUUID(), infoContainer));
				infoContainer.add(new StringElement("Git URL", application.getGitUri(), infoContainer));
				infoContainer.add(createCartridges(application, infoContainer));
				elements = new Object[] { infoContainer };
			} catch (Exception e) {
				Logger.error("Failed to display details for OpenShift application", e);
			}
		}
		return elements;
	}

	private ContainerElement createCartridges(IApplication application, ContainerElement infoContainer)
			throws OpenShiftException {
		ContainerElement cartridgesContainer = new ContainerElement("Cartridges", infoContainer);
		for (IEmbeddableCartridge cartridge : application.getEmbeddedCartridges()) {
			cartridgesContainer.add(
					new StringElement(cartridge.getName(), cartridge.getUrl().toString(), true,
							cartridgesContainer));
		}
		return cartridgesContainer;
	}
}
