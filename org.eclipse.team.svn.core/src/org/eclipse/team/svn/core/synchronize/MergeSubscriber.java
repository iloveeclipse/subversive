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

package org.eclipse.team.svn.core.synchronize;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind;
import org.eclipse.team.svn.core.connector.SVNMergeStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.local.AbstractMergeSet;
import org.eclipse.team.svn.core.operation.local.IRemoteStatusOperation;
import org.eclipse.team.svn.core.operation.local.MergeSet1URL;
import org.eclipse.team.svn.core.operation.local.MergeSet2URL;
import org.eclipse.team.svn.core.operation.local.MergeSetReintegrate;
import org.eclipse.team.svn.core.operation.local.MergeStatusOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.resource.IChangeStateProvider;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Merge view data provider
 * 
 * @author Alexander Gurov
 */
public class MergeSubscriber extends AbstractSVNSubscriber {
	private static MergeSubscriber instance = null;
	
	protected MergeScopeHelper mergeScopeHelper;
	protected MergeStatusOperation mergeStatusOp;
    protected IRemoteStatusCache baseStatusCache;
	
	public static synchronized MergeSubscriber instance() {
		if (MergeSubscriber.instance == null) {
		    MergeSubscriber.instance = new MergeSubscriber();
		}
		return MergeSubscriber.instance;
	}

	public MergeScopeHelper getMergeScopeHelper() {
		return this.mergeScopeHelper;
	}
	
    public void setMergeScopeHelper(MergeScopeHelper scope) {
        this.mergeScopeHelper = scope;
    }
    
    protected SyncInfo getSVNSyncInfo(ILocalResource localStatus, IResourceChange remoteStatus) throws TeamException {
    	IResourceChange baseStatus = SVNRemoteStorage.instance().resourceChangeFromBytes(this.baseStatusCache.getBytes(localStatus.getResource()));
    	AbstractSVNSyncInfo syncInfo;
    	if (remoteStatus == null && baseStatus == null) {
    		syncInfo = new MergeSyncInfo(localStatus, null, null, this.getResourceComparator());
    	} else {
    		SVNMergeStatus mergeStatus = this.getStatusFor(localStatus.getResource());
    		if (mergeStatus != null) {
        		if (mergeStatus.skipped) {
        			syncInfo = new MergeSyncInfo(localStatus, baseStatus, remoteStatus, this.getResourceComparator());
        		} else {
        			syncInfo = new UpdateSyncInfoForMerge(localStatus, baseStatus, remoteStatus, this.getResourceComparator());
        		}	
    		} else {
    			//should never happen
    			syncInfo = new UpdateSyncInfo(localStatus, null, this.getResourceComparator());	
    		}    		
    	}
    	return syncInfo;
    }
    
	protected SVNMergeStatus getStatusFor(IResource resource) {
		SVNMergeStatus []statuses = this.mergeScopeHelper.getMergeSet().getStatuses();
		IPath target = FileUtility.getResourcePath(resource);
		for (int i = 0; i < statuses.length; i++) {
			if (target.equals(new Path(statuses[i].path))) {
				return statuses[i];
			}
		}
		return null;
	}

    protected IRemoteStatusOperation addStatusOperation(CompositeOperation op, IResource[] resources, int depth) {
    	MergeStatusOperation mergeOp = this.mergeStatusOp = (this.mergeScopeHelper == null ? null : new MergeStatusOperation(this.mergeScopeHelper.getMergeSet(), resources));
    	if (mergeOp == null) {
    		return null;
    	}
		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
		op.add(saveOp);
    	op.add(mergeOp);
		op.add(new RestoreProjectMetaOperation(saveOp));
    	op.add(new RefreshResourcesOperation(resources, depth, RefreshResourcesOperation.REFRESH_CHANGES));
        return mergeOp;
    }
    
	protected HashSet<IResource> clearRemoteStatusesImpl(IResource []resources) throws TeamException {
		this.clearRemoteStatusesImpl(this.baseStatusCache, resources);
		return super.clearRemoteStatusesImpl(resources);
	}
	
    public void refresh(final IResource []resources, final int depth, IProgressMonitor monitor) throws TeamException {
		if (this.mergeScopeHelper != null) {
			this.baseStatusCache.clearAll();
			this.mergeScopeHelper.getMergeSet().setStatuses(new SVNMergeStatus[0]);
		}
    	super.refresh(resources, depth, monitor);
    }
	
	protected IResourceChange handleResourceChange(IRemoteStatusOperation rStatusOp, SVNEntryStatus status) {
		final SVNMergeStatus current = (SVNMergeStatus)status;
		IChangeStateProvider endProvider = new IChangeStateProvider() {
			public long getChangeDate() {
				return current.date;
			}
			public String getChangeAuthor() {
				return current.author;
			}
			public SVNRevision.Number getChangeRevision() {
				return current.endRevision == SVNRevision.INVALID_REVISION_NUMBER ? null : SVNRevision.fromNumber(current.endRevision);
			}
			public Kind getTextChangeType() {
				return current.textStatus;
			}
			public SVNEntryStatus.Kind getPropertiesChangeType() {
				return current.propStatus;
			}
			public SVNEntry.Kind getNodeKind() {
				SVNEntry.Kind kind = SVNUtility.getNodeKind(current.path, current.nodeKind, true);
				// if not exists on repository try to check it with WC kind...
				return kind == SVNEntry.Kind.NONE && !current.hasTreeConflict ? SVNUtility.getNodeKind(current.path, current.nodeKind, false) : kind;
			}
			public String getLocalPath() {
				return current.path;
			}
			public String getComment() {
				return current.comment;
			}
			public boolean isCopied() {
				return false;
			}
			public boolean isSwitched() {
				return false;
			}
			public IResource getExact(IResource []set) {
				return FileUtility.selectOneOf(MergeSubscriber.this.mergeScopeHelper.getRoots(), set);
			}		
			public SVNConflictDescriptor getTreeConflictDescriptor() {
				return current.treeConflictDescriptor;
			}
		};
		if (endProvider.getNodeKind() == SVNEntry.Kind.NONE && !current.hasTreeConflict) {
			return null;
		}
		IResourceChange endResourceChange = SVNRemoteStorage.instance().asResourceChange(endProvider, false);
		if (endResourceChange == null) {
			return null;
		}
		if (endResourceChange.getRevision() != SVNRevision.INVALID_REVISION_NUMBER) {
			IRepositoryResource originator = this.getEndOriginator();
			String decodedUrl = SVNUtility.decodeURL(current.endUrl);
			originator = endProvider.getNodeKind() == SVNEntry.Kind.DIR ? (IRepositoryResource)originator.asRepositoryContainer(decodedUrl, false) : originator.asRepositoryFile(decodedUrl, false);
			originator.setSelectedRevision(SVNRevision.fromNumber(current.textStatus == SVNEntryStatus.Kind.DELETED ? current.endRevision - 1 : current.endRevision));
			endResourceChange.setOriginator(originator);
		}
		
		IChangeStateProvider startProvider = new IChangeStateProvider() {
			public long getChangeDate() {
				return current.date;
			}
			public String getChangeAuthor() {
				return null;
			}
			public SVNRevision.Number getChangeRevision() {
				return current.startRevision == SVNRevision.INVALID_REVISION_NUMBER ? null : SVNRevision.fromNumber(current.startRevision);
			}
			public Kind getTextChangeType() {
				return current.startRevision == SVNRevision.INVALID_REVISION_NUMBER ? SVNEntryStatus.Kind.NONE : SVNEntryStatus.Kind.NORMAL;
			}
			public SVNEntryStatus.Kind getPropertiesChangeType() {
				return SVNEntryStatus.Kind.NONE;
			}
			public SVNEntry.Kind getNodeKind() {
				SVNEntry.Kind kind = SVNUtility.getNodeKind(current.path, current.nodeKind, true);
				// if not exists on repository try to check it with WC kind...
				return kind == SVNEntry.Kind.NONE && !current.hasTreeConflict ? SVNUtility.getNodeKind(current.path, current.nodeKind, false) : kind;
			}
			public String getLocalPath() {
				return current.path;
			}
			public String getComment() {
				return null;
			}
			public boolean isCopied() {
				return false;
			}
			public boolean isSwitched() {
				return false;
			}
			public IResource getExact(IResource []set) {
				return FileUtility.selectOneOf(MergeSubscriber.this.mergeScopeHelper.getRoots(), set);
			}			
			public SVNConflictDescriptor getTreeConflictDescriptor() {
				return current.treeConflictDescriptor;
			}
		};
		IResourceChange startResourceChange = SVNRemoteStorage.instance().asResourceChange(startProvider, false);
		if (startResourceChange.getRevision() != SVNRevision.INVALID_REVISION_NUMBER) {
			String decodedUrl = SVNUtility.decodeURL(current.startUrl);
			IRepositoryResource originator = this.getStartOriginator();
			originator = startProvider.getNodeKind() == SVNEntry.Kind.DIR ? (IRepositoryResource)originator.asRepositoryContainer(decodedUrl, false) : originator.asRepositoryFile(decodedUrl, false);
			originator.setSelectedRevision(SVNRevision.fromNumber(current.startRevision));
			startResourceChange.setOriginator(originator);
		}
		try {
			this.baseStatusCache.setBytes(startResourceChange.getResource(), SVNRemoteStorage.instance().resourceChangeAsBytes(startResourceChange));
		} catch (TeamException e) {
			LoggedOperation.reportError(this.getClass().getName(), e);
		}
		
		return endResourceChange;
	}
	
	protected IRepositoryResource getEndOriginator() {
		AbstractMergeSet mergeSet = this.mergeScopeHelper.getMergeSet();
		if (mergeSet instanceof MergeSet1URL) {
			return ((MergeSet1URL)mergeSet).from[0];
		}
		else if (mergeSet instanceof MergeSet2URL) {
			return ((MergeSet2URL)mergeSet).fromEnd[0];
		}
		else {
			return ((MergeSetReintegrate)mergeSet).from[0];
		}
	}
	
	protected IRepositoryResource getStartOriginator() {
		AbstractMergeSet mergeSet = this.mergeScopeHelper.getMergeSet();
		if (mergeSet instanceof MergeSet1URL) {
			return ((MergeSet1URL)mergeSet).from[0];
		}
		else if (mergeSet instanceof MergeSet2URL) {
			return ((MergeSet2URL)mergeSet).fromStart[0];
		}
		else {
			return ((MergeSetReintegrate)mergeSet).from[0];
		}
	}
	
	protected boolean isIncoming(SVNEntryStatus status) {
		return true;
	}
	
    private MergeSubscriber() {
        super(false, SVNMessages.MergeSubscriber_Name);
		this.baseStatusCache = new RemoteStatusCache();
    }

}
