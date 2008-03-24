/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.MarkAsMergedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.AbstractNonRecursiveTeamAction;

/**
 * Mark selected resource as merged action implementation
 * 
 * @author Alexander Gurov
 */
public class MarkAsMergedAction extends AbstractNonRecursiveTeamAction {
	public MarkAsMergedAction() {
		super();
	}

	public void runImpl(IAction action) {
		IResource []resources = this.getSelectedResources(IStateFilter.SF_CONFLICTING);
		
		MarkAsMergedOperation mainOp = new MarkAsMergedOperation(resources, false, null);
		CompositeOperation op = new CompositeOperation(mainOp.getId());
		op.add(mainOp);
		op.add(new RefreshResourcesOperation(FileUtility.getParents(resources, false)));
		
		this.runScheduled(op);
	}
	
	public boolean isEnabled() {
		return 
			this.getSelectedResources().length == 1 &&
			this.checkForResourcesPresence(IStateFilter.SF_CONFLICTING);
	}

}
