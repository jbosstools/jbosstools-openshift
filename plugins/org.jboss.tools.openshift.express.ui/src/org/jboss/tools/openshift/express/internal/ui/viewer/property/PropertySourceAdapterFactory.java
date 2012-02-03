/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.viewer.property;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.IUser;

/**
 * @author Xavier Coulon
 * 
 */
public class PropertySourceAdapterFactory implements IAdapterFactory {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if(adapterType == IPropertySource.class) {
			if(adaptableObject instanceof IUser) {
				return new UserPropertySource((IUser)adaptableObject);
			}
			if(adaptableObject instanceof IApplication) {
				return new ApplicationPropertySource((IApplication)adaptableObject);
			}if(adaptableObject instanceof IEmbeddableCartridge) {
				return new EmbeddableCartridgePropertySource((IEmbeddableCartridge)adaptableObject);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}

}
