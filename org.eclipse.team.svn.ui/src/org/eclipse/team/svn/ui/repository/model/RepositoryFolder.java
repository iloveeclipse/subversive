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

package org.eclipse.team.svn.ui.repository.model;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.remote.GetRemoteFolderChildrenOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.DefaultOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Repository folder node representation 
 * 
 * @author Alexander Gurov
 */
public class RepositoryFolder extends RepositoryResource implements IParentTreeNode {
	protected GetRemoteFolderChildrenOperation childrenOp;
	protected Object []wrappedChildren;
	
	public RepositoryFolder(RepositoryResource parent, IRepositoryResource resource) {
		super(parent, resource);
	}

	public void refresh() {
		this.childrenOp = null;
		this.wrappedChildren = null;
		super.refresh();
	}
	
	public boolean hasChildren() {
		return true;
	}
	
	public Object []getChildren(Object o) {
		final IRepositoryContainer container = (IRepositoryContainer)this.resource;
		
		if (this.wrappedChildren != null) {
			return this.wrappedChildren;
		}
		
		if (this.childrenOp != null) {
			Object []retVal = RepositoryFolder.wrapChildren(this, this.childrenOp.getChildren(), this.childrenOp);
			if (retVal != null) {
				this.wrappedChildren = retVal;
			}
			else if (this.childrenOp.getExecutionState() != IActionOperation.ERROR) {
				retVal = new Object[] {new RepositoryPending(this)};
			}
			else {
				retVal = this.wrappedChildren = new Object[] {new RepositoryError(this.childrenOp.getStatus())};
			}
			return retVal;
		}
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		this.childrenOp = new GetRemoteFolderChildrenOperation(container, SVNTeamPreferences.getRepositoryBoolean(store, SVNTeamPreferences.REPOSITORY_SHOW_EXTERNALS_NAME), SVNTeamPreferences.getBehaviourBoolean(store, SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME));
		
		CompositeOperation op = new CompositeOperation(this.childrenOp.getId(), this.childrenOp.getMessagesClass());
		op.add(this.childrenOp);
		op.add(this.getRefreshOperation(this.getViewer()));

		UIMonitorUtility.doTaskScheduled(op, new DefaultOperationWrapperFactory() {
            public IActionOperation getLogged(IActionOperation operation) {
        		return new LoggedOperation(operation);
            }
        });
		
		return new Object[] {new RepositoryPending(this)};
	}
	
	public Object []peekChildren(Object o) {
		if (this.childrenOp == null) {
			return this.getChildren(o);
		}
		Object []retVal = RepositoryFolder.wrapChildren(this, this.childrenOp.getChildren(), this.childrenOp);
		return retVal == null ? new Object[] {this.childrenOp.getExecutionState() != IActionOperation.ERROR ? (Object)new RepositoryPending(this) : new RepositoryError(this.childrenOp.getStatus())} : retVal;
	}
	
	public static RepositoryResource []wrapChildren(RepositoryResource parent, IRepositoryResource []resources, GetRemoteFolderChildrenOperation childrenOp) {
		if (resources == null) {
			return null;
		}
		RepositoryResource []wrappers = new RepositoryResource[resources.length];
		for (int i = 0; i < resources.length; i++) {
			String externalsName = childrenOp != null ? childrenOp.getExternalsName(i) : null;
			wrappers[i] = RepositoryFolder.wrapChild(parent, resources[i], externalsName);
		}
		return wrappers;
	}
	
	public static RepositoryResource wrapChild(RepositoryResource parent, IRepositoryResource resource, String externalsName) {
		RepositoryResource retVal = null;
		if (resource instanceof IRepositoryRoot && externalsName == null) {
			IRepositoryRoot tmp = (IRepositoryRoot)resource;
			switch (tmp.getKind()) {
				case IRepositoryRoot.KIND_TRUNK: {
					retVal = new RepositoryTrunk(parent, tmp);
					break;
				}
				case IRepositoryRoot.KIND_BRANCHES: {
					retVal = new RepositoryBranches(parent, tmp);
					break;
				}
				case IRepositoryRoot.KIND_TAGS: {
					retVal = new RepositoryTags(parent, tmp);
					break;
				}
				default: {
					retVal = new RepositoryRoot(parent, tmp);
					break;
				}
			}
		}
		else {
			retVal = resource instanceof IRepositoryFile ? (RepositoryResource)new RepositoryFile(parent, resource) : new RepositoryFolder(parent, resource);
			if (externalsName != null) {
				retVal.setLabel(externalsName);
				retVal.setExternals(true);
			}
		}
		return retVal;
	}
	
	protected ImageDescriptor getImageDescriptorImpl() {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}
	
}
