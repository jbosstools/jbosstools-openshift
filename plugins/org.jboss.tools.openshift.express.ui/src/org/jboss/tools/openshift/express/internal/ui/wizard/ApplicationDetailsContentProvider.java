/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * 
 */
public class ApplicationDetailsContentProvider implements ITreeContentProvider {

	private IApplication application;

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IApplication) {
			try {
				final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss");
				final ContainerElement infoContainer = new ContainerElement("info", null);
				infoContainer.add(new SimpleElement("Name", application.getName(), infoContainer));
				infoContainer.add(new SimpleElement("Public URL", application.getApplicationUrl().toString(), true,
						infoContainer));
				infoContainer.add(new SimpleElement("Type", application.getCartridge().getName(), infoContainer));
				infoContainer.add(new SimpleElement("Created on", format.format(application.getCreationTime()),
						infoContainer));
				infoContainer.add(new SimpleElement("UUID", application.getUUID(), infoContainer));
				infoContainer.add(new SimpleElement("Git URL", application.getGitUri(), infoContainer));
				final ContainerElement cartridgesContainer = new ContainerElement("Cartridges", infoContainer);
				infoContainer.add(cartridgesContainer);
				for (IEmbeddableCartridge cartridge : application.getEmbeddedCartridges()) {
					cartridgesContainer.add(new SimpleElement(cartridge.getName(), cartridge.getUrl().toString(), true,
							cartridgesContainer));
				}
				return new Object[] { infoContainer };
			} catch (OpenShiftException e) {
				Logger.error("Failed to display details for OpenShift application", e);
			}
		}
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ContainerElement) {
			return ((ContainerElement) parentElement).getChildren();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof SimpleElement) {
			return ((SimpleElement) element).getParent();
		} else if (element instanceof ContainerElement) {
			return ((ContainerElement) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof ContainerElement);
	}

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof IApplication) {
			this.application = (IApplication) newInput;
		} else {
			this.application = null;
		}

	}

	static class SimpleElement {

		private final String property;
		private final String value;
		private final ContainerElement parent;
		private final boolean isLink;

		public SimpleElement(String property, String value, ContainerElement parent) {
			super();
			this.property = property;
			this.value = value;
			this.parent = parent;
			this.isLink = false;
		}

		public SimpleElement(String property, String value, boolean isLink, ContainerElement parent) {
			super();
			this.property = property;
			this.value = value;
			this.parent = parent;
			this.isLink = isLink;
		}

		/**
		 * @return the property
		 */
		public final String getProperty() {
			return property;
		}

		/**
		 * @return the value
		 */
		public final String getValue() {
			return value;
		}

		/**
		 * @return the parent container
		 */
		public final ContainerElement getParent() {
			return parent;
		}

		/**
		 * @return the isLink
		 */
		public boolean isLink() {
			return isLink;
		}
	}

	static class ContainerElement {

		private final String property;
		private final List<Object> children;
		private final ContainerElement parent;

		public ContainerElement(String property, ContainerElement parent) {
			this.property = property;
			this.children = new ArrayList<Object>();
			this.parent = parent;
		}

		/**
		 * @return the property
		 */
		public final String getProperty() {
			return property;
		}

		public final void add(SimpleElement child) {
			children.add(child);
		}

		public final void add(ContainerElement child) {
			children.add(child);
		}

		/**
		 * @return the value
		 */
		public final Object[] getChildren() {
			return children.toArray();
		}

		/**
		 * @return the parent container
		 */
		public final ContainerElement getParent() {
			return parent;
		}
	}

}
