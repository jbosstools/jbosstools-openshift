package org.jboss.tools.openshift.test.ui.models;

import com.openshift.restclient.model.IResource;

public class ResourceEquality {
	public static boolean equals(Object left, Object right) {
		if (right == left) {
			return true;
		}
		if (!(left instanceof IResource)) {
			throw new IllegalArgumentException();
		}
		IResource thiz= (IResource) left;
		if (right == null || right.getClass() != left.getClass()) {
			return false;
		}
		IResource other= (IResource) right;
		return thiz.getNamespace().equals(other.getNamespace()) 
				&& thiz.getKind().equals(other.getKind())
				&& thiz.getName().equals(other.getName());
	}
	
	public static int hashCode(Object left) {
		IResource thiz= (IResource) left;
		return thiz.getName().hashCode()^thiz.getNamespace().hashCode();
	}
}
