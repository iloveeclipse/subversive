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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.annotate.AnnotateView;
import org.eclipse.team.svn.ui.annotate.BuiltInAnnotate;
import org.eclipse.team.svn.ui.annotate.CheckPerspective;
import org.eclipse.team.svn.ui.dialog.PromptOptionDialog;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

/**
 * The operation shows annotation for the local resource.
 * 
 * @author Alexander Gurov
 */
public class LocalShowAnnotationOperation extends AbstractWorkingCopyOperation {
	protected IWorkbenchPage page;
	protected SVNRevision revision;
	
	public LocalShowAnnotationOperation(IResource resource, IWorkbenchPage page) {
		this(resource, page, null);
	}

	public LocalShowAnnotationOperation(IResource resource, IWorkbenchPage page, SVNRevision revision) {
		super("Operation.ShowAnnotation", new IResource[] {resource});
		this.page = page;
		this.revision = revision;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource resource = this.operableData()[0];
    	ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
    	boolean notExists = local == null || IStateFilter.SF_NOTEXISTS.accept(local);
    	SVNRevision revision = this.revision;
    	if (revision == null) {
    		revision = notExists || local.getRevision() == -1 ? SVNRevision.HEAD : SVNRevision.fromNumber(local.getRevision());
    	}
    	
    	final int []viewType = new int[] {SVNTeamPreferences.getAnnotateInt(SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.ANNOTATE_USE_QUICK_DIFF_NAME)};
    	
    	if (notExists) {
    		viewType[0] = SVNTeamPreferences.ANNOTATE_DEFAULT_VIEW;
    	}
    	else if (viewType[0] == SVNTeamPreferences.ANNOTATE_PROMPT_VIEW) {
    		new PromptOptionDialog(
    				this.page.getWorkbenchWindow().getShell(), 
    				this.getOperationResource("Prompt.Title"), 
    				this.getOperationResource("Prompt.Message"), 
    				this.getOperationResource("Prompt.Remember"), 
    				new PromptOptionDialog.AbstractOptionManager() {
						public void buttonPressed(IPreferenceStore store, int idx, boolean toggle) {
							viewType[0] = idx == 0 ? SVNTeamPreferences.ANNOTATE_QUICK_DIFF_VIEW : SVNTeamPreferences.ANNOTATE_DEFAULT_VIEW;
							if (toggle) {
								SVNTeamPreferences.setAnnotateInt(store, SVNTeamPreferences.ANNOTATE_USE_QUICK_DIFF_NAME, viewType[0]);
							}
						}
					}).open();
    	}
    	
    	IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resource);
	    remote.setSelectedRevision(revision);
		CorrectRevisionOperation correctOp = new CorrectRevisionOperation(null, remote, local.getRevision(), resource);
		
		if (!UIMonitorUtility.doTaskNowDefault(correctOp, true).isCancelled()) {
	    	if (viewType[0] == SVNTeamPreferences.ANNOTATE_DEFAULT_VIEW) {
	    		CheckPerspective.run(this.page.getWorkbenchWindow());
			    IViewPart viewPart = this.page.showView(AnnotateView.VIEW_ID);
			    if (viewPart != null && viewPart instanceof AnnotateView) {
					((AnnotateView)viewPart).showEditor(resource, revision, remote.getPegRevision());
			    }
	    	}
	    	else {
			    new BuiltInAnnotate().open(this.page, remote, (IFile)resource);
	    	}
		}
	}

}
