/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConflictResolution;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Performs "mark resolved" operation
 * 
 * @author Alexander Gurov
 */
public class MarkResolvedOperation extends AbstractWorkingCopyOperation {
	protected SVNConflictResolution.Choice conflictResult;
	protected SVNDepth depth;
	
	public MarkResolvedOperation(IResource[] resources, SVNConflictResolution.Choice conflictResult, SVNDepth depth) {
		super("Operation_MarkResolved", SVNMessages.class, resources); //$NON-NLS-1$
		this.conflictResult = conflictResult;
		this.depth = depth;
	}

	public MarkResolvedOperation(IResourceProvider provider, SVNConflictResolution.Choice conflictResult, SVNDepth depth) {
		super("Operation_MarkResolved", SVNMessages.class, provider); //$NON-NLS-1$
		this.conflictResult = conflictResult;
		this.depth = depth;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource []resources = this.operableData();
		
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resources[i]);
			final String path = FileUtility.getWorkingCopyPath(resources[i]);
			final ISVNConnector proxy = location.acquireSVNProxy();
			
			this.protectStep(new IUnprotectedOperation() {
				public void run(IProgressMonitor monitor) throws Exception {
					proxy.resolve(path, MarkResolvedOperation.this.conflictResult, MarkResolvedOperation.this.depth, new SVNProgressMonitor(MarkResolvedOperation.this, monitor, null));
				}
			}, monitor, resources.length);
			
			location.releaseSVNProxy(proxy);
		}
	}

}
