/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.operation.ShowPropertiesOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.properties.PropertiesView;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Show properties action implementation for Synchronize view.
 * 
 * @author Alexei Goncharov
 */
public class ShowOutgoingPropertiesAction extends AbstractSynchronizeModelAction {

	public ShowOutgoingPropertiesAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	public ShowOutgoingPropertiesAction(String text, ISynchronizePageConfiguration configuration, ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
	}
	
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		if (selection.size() == 1) {
			ISynchronizeModelElement element = (ISynchronizeModelElement)selection.getFirstElement();
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(element.getResource());
			// null for change set nodes
			return local != null && IStateFilter.SF_VERSIONED.accept(local);
		}
	    return false;
	}
	
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IResource selectedResource = ShowOutgoingPropertiesAction.this.getSelectedResource();
		IResourcePropertyProvider provider = new GetPropertiesOperation(selectedResource);
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		boolean usePropertiesView = SVNTeamPreferences.getPropertiesBoolean(store, SVNTeamPreferences.PROPERTY_USE_VIEW_NAME);
		if (usePropertiesView) {
			try {
				PropertiesView view = (PropertiesView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(PropertiesView.VIEW_ID);
				view.setResource(selectedResource, provider, false);
			}
			catch (PartInitException ex) {
				//unreachable code
			}
		}
		else {
			ShowPropertiesOperation showOp = new ShowPropertiesOperation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), selectedResource, provider);
			CompositeOperation composite = new CompositeOperation(showOp.getId());
			composite.add(provider);
			composite.add(showOp, new IActionOperation[] {provider});
			if (!showOp.isEditorOpened()) {
				return composite;
			}
		}
		return null;
	}

}
