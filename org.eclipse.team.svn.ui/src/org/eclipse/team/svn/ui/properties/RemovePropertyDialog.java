/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.properties;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Remove property dialog
 * 
 * @author Alexander Gurov
 */
public class RemovePropertyDialog extends MessageDialog {
	
	protected boolean recursive;
	protected boolean isFile;

	public RemovePropertyDialog(Shell parentShell, boolean oneProperty, boolean isFile) {
		super(parentShell, 
			oneProperty ? SVNUIMessages.RemoveProperty_Title_Single : SVNUIMessages.RemoveProperty_Title_Multi, 
			null, 
			oneProperty ? SVNUIMessages.RemoveProperty_Message_Single : SVNUIMessages.RemoveProperty_Message_Multi,
			MessageDialog.QUESTION, 
			new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 
			0);
		
		this.recursive = false;
		this.isFile = isFile;
	}
	
	public boolean isRecursive() {
		return this.recursive;
	}

	protected Control createCustomArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (!this.isFile) {
			Button recursive = new Button(composite, SWT.CHECK);
			recursive.setLayoutData(new GridData());
			recursive.setSelection(false);
			recursive.setText(SVNUIMessages.RemoveProperty_Recursively);
			recursive.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					Button button = (Button)event.widget;
					RemovePropertyDialog.this.recursive = button.getSelection();
				}
			});
		}
		
		return composite;
	}
	
}
