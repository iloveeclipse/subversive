/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.compare;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Compare panel
 * 
 * @author Sergiy Logvin
 */
public class ComparePanel extends AbstractDialogPanel {
	
	protected CompareEditorInput compareInput;
	protected IResource resource;
	
	public ComparePanel(CompareEditorInput compareInput, IResource resource) {
		super(new String[] {SVNUIMessages.CompareLocalPanel_Save, IDialogConstants.CANCEL_LABEL});
		this.compareInput = compareInput;
		this.resource = resource;
		this.dialogTitle = SVNUIMessages.CompareLocalPanel_Title;
		this.dialogDescription = SVNUIMessages.CompareLocalPanel_Description;
		this.defaultMessage = SVNUIMessages.CompareLocalPanel_Message;
	}
	
	public void createControlsImpl(Composite parent) {
		Control control = this.compareInput.createContents(parent);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		Shell shell= control.getShell();
		shell.setText(this.compareInput.getTitle());
		shell.setImage(this.compareInput.getTitleImage());
	}

	protected void cancelChangesImpl() {

	}

	protected void saveChangesImpl() {
		
		RefreshResourcesOperation refreshOp = new RefreshResourcesOperation(new IResource[] {this.resource.getProject()});
		AbstractWorkingCopyOperation mainOp = new AbstractWorkingCopyOperation("Operation_SaveChanges", SVNUIMessages.class, new IResource[] {this.resource.getProject()}) { //$NON-NLS-1$
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				ComparePanel.this.compareInput.saveChanges(monitor);
			}
		};
		CompositeOperation composite = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		composite.add(mainOp);
		composite.add(refreshOp);
		UIMonitorUtility.doTaskBusyWorkspaceModify(composite);
	}
	
	public Point getPrefferedSizeImpl() {
        return new Point(650, 500);
    }

}
