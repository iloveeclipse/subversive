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

package org.eclipse.team.svn.core.synchronize.variant;

import org.eclipse.team.core.variants.CachedResourceVariant;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Abstract resource variant implementation
 * 
 * @author Alexander Gurov
 */
public abstract class ResourceVariant extends CachedResourceVariant {

	protected ILocalResource local;
	
	public ResourceVariant(ILocalResource local) {
		super();
		
		this.local = local;
	}
	
	public ILocalResource getResource() {
		return this.local;
	}

	protected String getCachePath() {
		IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(this.local.getResource());
		return location.getId() + this.local.getResource().getFullPath().toString() + " " + this.getContentIdentifier();
	}

	protected String getCacheId() {
		return IRemoteStorage.class.getName();
	}

	public String getName() {
		return this.local.getName();
	}

	public byte []asBytes() {
		return this.getContentIdentifier().getBytes();
	}

	public String getStatus() {
		return this.local.getStatus();
	}
	
	public String getContentIdentifier() {
	    long revision = this.local.getRevision();
	    if (revision == SVNRevision.INVALID_REVISION_NUMBER && (IStateFilter.SF_ONREPOSITORY.accept(this.local) || this.local.isCopied())) {
			SVNEntryInfo []st = SVNUtility.info(new SVNEntryRevisionReference(FileUtility.getWorkingCopyPath(this.local.getResource())));
			if (st != null && st.length > 0) {
				revision = this.local.isCopied() ? st[0].copyFromRevision : st[0].lastChangedRevision;
			}
	    }
	    if (revision == SVNRevision.INVALID_REVISION_NUMBER) {
		    if (this.isNotOnRepository()) {
		        return SVNMessages.ResourceVariant_unversioned;
		    }
		    if (IStateFilter.SF_DELETED.accept(this.local)) {
		    	return SVNMessages.ResourceVariant_deleted;
		    }
	    }
		return String.valueOf(revision); 
	}

    protected boolean isNotOnRepository() {
        return IStateFilter.SF_UNVERSIONED.accept(this.local);
    }
    
}
