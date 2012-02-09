/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.util;

/**
 * @author Xavier Coulon
 *
 */
public class SecurePasswordStoreException extends Exception {

	/** generated serialVersionUID. */
	private static final long serialVersionUID = -1732042851833545771L;

	/**
	 * Full constructor
	 * @param message the message to print
	 * @param cause the underlying cause
	 */
	public SecurePasswordStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Full constructor
	 * @param message the message to print
	 */
	public SecurePasswordStoreException(String message) {
		super(message);
	}


}
