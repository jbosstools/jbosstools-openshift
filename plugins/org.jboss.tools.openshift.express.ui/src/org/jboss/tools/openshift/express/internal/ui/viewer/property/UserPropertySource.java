package org.jboss.tools.openshift.express.internal.ui.viewer.property;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class UserPropertySource implements IPropertySource {

	private final IUser user;

	public UserPropertySource(IUser user) {
		this.user = user;
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] { new TextPropertyDescriptor("Username", "Username"),
				new TextPropertyDescriptor("Domain", "Domain") };
	}

	@Override
	public Object getPropertyValue(Object id) {
		try {
			if (id.equals("Username")) {
				return user.getRhlogin();
			}
			if (id.equals("Domain")) {
				return user.getDomain().getNamespace() + "." + user.getDomain().getRhcDomain();
			}
		} catch (OpenShiftException e) {
			Logger.error("Could not get selected object's property '" + id + "'.", e);
		}
		return null;
	}

	@Override
	public void resetPropertyValue(Object id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

}
