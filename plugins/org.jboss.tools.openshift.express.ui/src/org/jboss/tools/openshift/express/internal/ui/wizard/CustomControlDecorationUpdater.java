/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationUpdater;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * @author Xavier Coulon
 * 
 */
public class CustomControlDecorationUpdater extends ControlDecorationUpdater {

	/**
	 * {@inheritDoc} Overrides the standard behaviour: for CANCEL status, items are decorated with the REQUIRED
	 * decorator, not the ERROR one.
	 */
	@Override
	protected Image getImage(IStatus status) {
		if (status == null) {
			return null;
		}
		String fieldDecorationID = null;
		switch (status.getSeverity()) {
		case IStatus.INFO:
			fieldDecorationID = FieldDecorationRegistry.DEC_INFORMATION;
			break;
		case IStatus.WARNING:
			fieldDecorationID = FieldDecorationRegistry.DEC_WARNING;
			break;
		case IStatus.ERROR:
			fieldDecorationID = FieldDecorationRegistry.DEC_ERROR;
			break;
		case IStatus.CANCEL:
			fieldDecorationID = FieldDecorationRegistry.DEC_REQUIRED;
			break;
		}

		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(fieldDecorationID);
		return fieldDecoration == null ? null : fieldDecoration.getImage();
	}
}
