/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class manages control's tooltip that should show text representation of the control 
 * only when it is not fit into the space allocated to the control. If a custom tooltip 
 * provided, the text representation of the control is added before it when needed.
 * 
 * Be aware that this class overrides any other tooltip set on the control.
 *  
 * @author Viacheslav Kabanovich
 *
 */
public class SmartTooltip {
	private static final String dataKey = "jboss-auto-tooltip";

	/**
	 * Convenience method that allows not to keep reference to the instance 
	 * of this class created for the control.
	 *
	 * @param control
	 * @return
	 */
	public static SmartTooltip get(Control control) {
		Object data = control.getData(dataKey);
		return (data instanceof SmartTooltip) ? (SmartTooltip)data : null;
	}

	protected Shell shell;
	protected Control control;
	protected String tooltip;

	/**
	 * Listeners are null in disabled state, and assigned to listeners after the instance is enabled;
	 */
	protected ModifyListener modifyListener;
	protected ControlListener controlListener;

	/**
	 * Manages tooltip so that it shows full text representation of the control
	 * when it is not fit into the available space, otherwise no tooltip is shown.
	 * @param control
	 */
	public SmartTooltip(Control control) {
		this(control, control.getToolTipText());
	}

	/**
	 * Manages tooltip so that
	 * - it shows only the provided custom tooltip if text representation of the control
	 *   is fit into the available space.
	 * - adds the line with text representation of the control before the custom tooltip.
	 *
	 * @param control
	 * @param tooltip
	 */
	public SmartTooltip(Control control, String tooltip) {
		this.control = control;
		this.tooltip = tooltip;
		enable();
		control.setData(dataKey, this);
	}

	/**
	 * Activates the smart tooltip. Takes no effect if it is already active.
	 * For updating when needed, call update().
	 */
	protected void enable() {
		if(control.isDisposed() || controlListener != null) {
			//Either cannot be enabled or already enabled.
			return;
		}
		shell = control.getShell();
		shell.addControlListener(controlListener = new CL());
		control.addControlListener(controlListener);
		control.getParent().addControlListener(controlListener);
		if(control instanceof Text) {
			((Text)control).addModifyListener(modifyListener = new ML());
		} else if(control instanceof StyledText) {
			((StyledText)control).addModifyListener(modifyListener = new ML());
		} else if(control instanceof Combo) {
			((Combo)control).addModifyListener(modifyListener = new ML());
		} else {
			//update manually when setting new text to the control.
		}
		update();
	}

	/**
	 * Sets default tooltip to the control and turns off and deactivates the smart tooltip.
	 */
	protected void disable() {
		if(control.isDisposed()) {
			controlListener = null;
			modifyListener = null;
			return;
		}
		if(controlListener != null) {
			shell.removeControlListener(controlListener);
			control.getParent().removeControlListener(controlListener);
			control.removeControlListener(controlListener);
			controlListener = null;
		}
		if(modifyListener != null) {
			if(control instanceof Text) {
				((Text)control).removeModifyListener(modifyListener);
			} else if(control instanceof StyledText) {
				((StyledText)control).removeModifyListener(modifyListener);
			} else if(control instanceof Combo) {
				((Combo)control).removeModifyListener(modifyListener);
			}
			modifyListener = null;
		}
		control.setToolTipText(tooltip);
	}

	/**
	 * Activates and deactivates the smart tooltip. Takes no effect if the 
	 * provided value is the same as the current state, or if control is disposed.  
	 * @param value
	 */
	public void setEnabled(boolean value) {
		if(value != isEnabled()) {
			if(value) {
				enable();
			} else {
				disable();
			}
		}
	}

	/**
	 * Returns true if control is not disposed and the smart tooltip is activated.
	 * @return
	 */
	public boolean isEnabled() {
		return controlListener != null && !control.isDisposed();
	}

	class CL implements ControlListener {
		@Override
		public void controlResized(ControlEvent e) {
			update();
		}
		@Override
		public void controlMoved(ControlEvent e) {
		}
	}
	
	class ML implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			update();
		}
	}

	/**
	 * When managing control's tooltip with this class, do not set a tooltip directly to the control, 
	 * call this method.
	 * @param tooltip
	 */
	public void setToolTip(String tooltip) {
		this.tooltip = tooltip;
		if(!control.isDisposed()) {
			if(isEnabled()) {
				update();
			} else {
				control.setToolTipText(tooltip);
			}
		}
	}

	/**
	 * This method is called by this class on shell resize
	 * and for Text, StyledText, Combo on modification.
	 * For other controls (e.g. Label, Button, etc), when new text is set, 
	 * this method should be called explicitly.
	 */
	public void update() {
		if(!isEnabled()) {
			return;
		}
		int a = control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
		int x = control.getBounds().x;
		Control parent = control.getParent();
		while(parent != null) {
			Rectangle b = parent.getBounds();
			if(b.width < x + a) {
				control.setToolTipText(getText() + (tooltip == null ? "" : "\n" + tooltip));
				return;
			}
			if(parent instanceof Shell) {
				break;
			}
			x += b.x;
			parent = parent.getParent();
		}
		control.setToolTipText(tooltip);
	}

	/**
	 * Returns text representation for the control.
	 * This implementation covers Text, StyledText, Combo, Label, Button, Link
	 * Override this method to handle other controls.
	 * 
	 * @return
	 */
	protected String getText() {
		return control instanceof Text ? ((Text)control).getText()
			: control instanceof StyledText ? ((StyledText)control).getText()
			: control instanceof Combo ? ((Combo)control).getText()
			: control instanceof Label ? ((Label)control).getText()
			: control instanceof Button ? ((Button)control).getText()
			: control instanceof Link ? ((Link)control).getText()
			: "";
	}
}
