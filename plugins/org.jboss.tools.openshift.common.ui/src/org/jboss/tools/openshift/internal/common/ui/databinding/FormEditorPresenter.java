package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.widgets.Form;
import org.jboss.tools.common.ui.utils.MessageFactory;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport.IFormPresenter;

/**
 * A form presenter that allows FormPresenterSupport for a {@link Form} based
 * editor.
 * 
 * @author Andre Dietisheim
 *
 * @see Form
 * @see FormPresenterSupport
 */
public class FormEditorPresenter implements IFormPresenter {
	
	private Form form;

	public FormEditorPresenter(Form form) {
		this.form = form;
	}

	@Override
	public void setMessage(String message, int type) {
		if (form.getBody().isDisposed()) {
			return;
		}
		if (!StringUtils.isEmpty(message)) {
 			form.setMessage(message,
					type, new IMessage[] { new MessageFactory().create(message, type) });
		} else {
			form.setMessage("", IMessage.NONE); // clear the header from errors/messages
		}
	}
	
	@Override
	public void setComplete(boolean complete) {				
	}
	
	@Override
	public Control getControl() {
		return form;
	}
}