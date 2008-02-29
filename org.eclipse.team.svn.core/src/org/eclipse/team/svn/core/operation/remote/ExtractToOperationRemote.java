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

package org.eclipse.team.svn.core.operation.remote;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Extract selected resources to location (only remote resources)
 * Used from synchronize view ExtractTo incoming action
 * 
 * @author Alexei Goncharov
 */
public class ExtractToOperationRemote extends AbstractRepositoryOperation {
	private HashSet<String> toDelete;
	private String path;
	private boolean delitionAllowed;
	
	public ExtractToOperationRemote(IRepositoryResource []incomingResources, HashSet<String> markedForDelition, String path, boolean delitionAllowed) {
		super("Operation.ExtractTo", incomingResources);
		this.path = path;
		this.delitionAllowed = delitionAllowed;
		this.toDelete = markedForDelition;
	}
	
	public ExtractToOperationRemote(IRepositoryResourceProvider incomingResourcesProvider, HashSet<String> markedForDelition, String path, boolean delitionAllowed) {
		super("Operation.ExtractTo", incomingResourcesProvider);
		this.path = path;
		this.delitionAllowed = delitionAllowed;
		this.toDelete = markedForDelition;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		HashSet<IRepositoryResource> operableFolders = new HashSet<IRepositoryResource>();
		HashSet<IRepositoryResource> operableFiles = new HashSet<IRepositoryResource>();
		IRepositoryResource []resources = this.operableData();
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IRepositoryContainer) {
				operableFolders.add(resources[i]);
			}
			else {
				operableFiles.add(resources[i]);
			}
		}
		
		//to report progress
		int processed = 0;
		
		//folders first - to create all needed
		IRepositoryResource [] toOperateFurhter = operableFolders.toArray(new IRepositoryResource[0]);
		HashMap<String, String> previous = new HashMap<String, String>();
		SVNUtility.reorder(toOperateFurhter, true);
		
		for (IRepositoryResource current : toOperateFurhter) {
			String currentURL = current.getUrl();
			monitor.subTask(SVNTeamPlugin.instance().getResource("Operation.ExtractTo.Folders", new String [] {currentURL}));
			String toOperate = "";
			File operatingDirectory = null;
			if (current.getParent() == null
					|| !previous.keySet().contains(current.getParent().getUrl())) {
				toOperate = this.path + "/" + current.getName();
			}
			else {
				toOperate = previous.get(current.getParent().getUrl()) + "/" + current.getName();
			}
			operatingDirectory = new File(toOperate);
			if (toDelete.contains(current.getUrl())) {
				if (operatingDirectory.exists() && this.delitionAllowed)
				{
					FileUtility.deleteRecursive(operatingDirectory);
				}
			}
			else {
				operatingDirectory.mkdir();
				previous.put(currentURL, toOperate);
			}
			ProgressMonitorUtility.progress(monitor, processed++, resources.length);
		}

		//then files
		toOperateFurhter = operableFiles.toArray(new IRepositoryResource[0]);
		for (IRepositoryResource current : toOperateFurhter) {
			ProgressMonitorUtility.progress(monitor, processed++, resources.length);
			monitor.subTask(SVNTeamPlugin.instance().getResource("Operation.ExtractTo.RemoteFile", new String [] {current.getUrl()}));
			String pathToOperate = previous.keySet().contains(current.getParent().getUrl()) 
									? previous.get(current.getParent().getUrl())
									: this.path;
			
			if (this.toDelete.contains(current.getUrl())) {
				File to = new File(pathToOperate + "/" + current.getName());
				if (to.exists() && this.delitionAllowed) {
					to.delete();
				}
			}
			else {					
				this.downloadFile(current, pathToOperate, monitor);
			}
		}
	}
	
	protected void downloadFile(IRepositoryResource remote, String downloadTo, IProgressMonitor monitor) throws Exception {
		FileOutputStream stream = null;
		IRepositoryLocation location = remote.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			try {
				stream = new FileOutputStream(downloadTo + "/" + remote.getName());
				proxy.streamFileContent(SVNUtility.getEntryRevisionReference(remote), 2048, stream, new SVNProgressMonitor(this, monitor, null));
			}
			finally {
				if (stream != null) {
					try {stream.close();} catch (Exception ex) {}
				}
			}
		}
		finally {
			location.releaseSVNProxy(proxy);
		}
	}

}