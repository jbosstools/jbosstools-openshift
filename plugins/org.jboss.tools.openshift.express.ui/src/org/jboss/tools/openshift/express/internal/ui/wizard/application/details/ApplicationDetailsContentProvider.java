/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard.application.details;

import static org.jboss.tools.openshift.express.internal.ui.utils.StringUtils.toStringOrNull;

import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.util.NLS;
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
 * 
 */
public class ApplicationDetailsContentProvider extends AbstractPropertyTableContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		List<IProperty> elements = new ArrayList<IProperty>();
		if (inputElement instanceof IApplication) {
			IApplication application = (IApplication) inputElement;
			try {
				elements.add(new StringElement("Name", application.getName()));
				elements.add(
						new StringElement("Public URL", application.getApplicationUrl().toString(), true));
				elements.add(new StringElement("Type", application.getCartridge().getName()));
				SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss");
				elements.add(
						new StringElement("Created on", format.format(application.getCreationTime())));
				elements.add(new StringElement("UUID", application.getUUID()));
				elements.add(new StringElement("Git URL", application.getGitUrl()));
				elements.add(createCartridges(application));

			} catch (Exception e) {
				Logger.error(
						NLS.bind("Could not display details for OpenShift application {0}", application.getName()), e);
			}
		}
		return elements.toArray();
	}

	private ContainerElement createCartridges(IApplication application)
			throws OpenShiftException, SocketTimeoutException {
		ContainerElement cartridgesContainer = new ContainerElement("Cartridges");
		for (IEmbeddedCartridge cartridge : application.getEmbeddedCartridges()) {
			cartridgesContainer.add(
					new StringElement(
							cartridge.getName(), toStringOrNull(cartridge.getUrl()), true, cartridgesContainer));
		}
		return cartridgesContainer;
	}
}
