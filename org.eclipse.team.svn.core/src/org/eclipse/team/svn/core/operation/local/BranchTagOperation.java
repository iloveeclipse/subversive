/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Alexey Mikoyan - Initial implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Branch and Tag from working copy operation implementation
 *
 * @author Alexey Mikoyan
 *
 */
public class BranchTagOperation extends AbstractWorkingCopyOperation {
	protected IRepositoryResource destination;
	protected String message;
	
	public BranchTagOperation(String operationName, IResource[] resources, IRepositoryResource destination, String message) {
		super("Operation_" + operationName, SVNMessages.class, resources); //$NON-NLS-1$
		this.destination = destination;
		this.message = message;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = this.operableData();
		
		ProgressMonitorUtility.setTaskInfo(monitor, this, FileUtility.getNamesListAsString(resources));
		IRepositoryLocation location = this.destination.getRepositoryLocation();
		final ISVNConnector proxy = location.acquireSVNProxy();
		try {
			final String destinationUrl = SVNUtility.encodeURL(this.destination.getUrl());
			for (int i = 0; i < resources.length; i++) {
				final String wcPath = FileUtility.getWorkingCopyPath(resources[i]);
				
				ISVNNotificationCallback notify = new ISVNNotificationCallback() {
					public void notify(SVNNotification info) {
						if (info.revision != -1) {
							String message = SVNMessages.format(SVNMessages.Console_CommittedRevision, new String[] {String.valueOf(info.revision)});
							BranchTagOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
						}
					}
				};
				SVNUtility.addSVNNotifyListener(proxy, notify);
				
				this.protectStep(new IUnprotectedOperation() {
					public void run(IProgressMonitor monitor) throws Exception {
						BranchTagOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn copy \"" + wcPath + "\" \"" + destinationUrl + "\" -r " + SVNRevision.WORKING + " -m \"" + BranchTagOperation.this.message + "\"" + ISVNConnector.Options.asCommandLine(ISVNConnector.Options.INCLUDE_PARENTS) + FileUtility.getUsernameParam(BranchTagOperation.this.destination.getRepositoryLocation().getUsername()) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
						SVNEntryRevisionReference []src = new SVNEntryRevisionReference[] {new SVNEntryRevisionReference(wcPath, SVNRevision.WORKING, SVNRevision.WORKING)};
						proxy.copyRemote(src, destinationUrl, BranchTagOperation.this.message, ISVNConnector.Options.INCLUDE_PARENTS, null, ISVNConnector.NO_EXTERNALS_TO_PIN, new SVNProgressMonitor(BranchTagOperation.this, monitor, null));
					}
				}, monitor, resources.length);
				
				SVNUtility.removeSVNNotifyListener(proxy, notify);
			}
		}
		finally {
			location.releaseSVNProxy(proxy);
		}
	}

}
