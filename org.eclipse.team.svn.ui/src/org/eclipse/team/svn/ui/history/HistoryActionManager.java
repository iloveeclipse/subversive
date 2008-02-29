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

package org.eclipse.team.svn.ui.history;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNLogPath;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.ISVNConnector.Depth;
import org.eclipse.team.svn.core.connector.SVNEntry.Kind;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.history.SVNRemoteResourceRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.GetRemoteContentsOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.UpdateOperation;
import org.eclipse.team.svn.core.operation.remote.ExportOperation;
import org.eclipse.team.svn.core.operation.remote.GetRemotePropertiesOperation;
import org.eclipse.team.svn.core.operation.remote.LocateResourceURLInHistoryOperation;
import org.eclipse.team.svn.core.operation.remote.PreparedBranchTagOperation;
import org.eclipse.team.svn.core.operation.remote.management.AddRevisionLinkOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.action.remote.BranchTagAction;
import org.eclipse.team.svn.ui.action.remote.CreatePatchAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileWithAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileWithExternalAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileWithInplaceAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.ReplaceWarningDialog;
import org.eclipse.team.svn.ui.history.data.AffectedPathsNode;
import org.eclipse.team.svn.ui.history.data.SVNChangedPathData;
import org.eclipse.team.svn.ui.history.data.SVNLocalFileRevision;
import org.eclipse.team.svn.ui.history.model.ILogNode;
import org.eclipse.team.svn.ui.operation.CompareRepositoryResourcesOperation;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;
import org.eclipse.team.svn.ui.operation.OpenRemoteFileOperation;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.operation.RemoteShowAnnotationOperation;
import org.eclipse.team.svn.ui.operation.ShowHistoryViewOperation;
import org.eclipse.team.svn.ui.operation.ShowPropertiesOperation;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.wizard.CreatePatchWizard;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * Provides actions for affected paths window
 * 
 * @author Alexander Gurov
 */
public class HistoryActionManager {
	public interface IControlActionManager {
		public void installKeyBindings(StructuredViewer viewer);
		public void installDefaultAction(StructuredViewer viewer);
		public void installMenuActions(StructuredViewer viewer, IWorkbenchPartSite site);
	}
	
	public static class HistoryAction extends Action {
		protected HistoryAction(String text) {
			super(SVNTeamUIPlugin.instance().getResource(text));
		}
		
		protected HistoryAction(String text, Object []args) {
			this(text, args, null);
		}
		
		protected HistoryAction(String text, Object []args, String imageDescriptor) {
			super(SVNTeamUIPlugin.instance().getResource(text, args));
			this.setHoverImageDescriptor(imageDescriptor == null ? null : SVNTeamUIPlugin.instance().getImageDescriptor(imageDescriptor));
		}
		
		protected HistoryAction(String text, String imageDescriptor) {
			this(text);
			this.setHoverImageDescriptor(imageDescriptor == null ? null : SVNTeamUIPlugin.instance().getImageDescriptor(imageDescriptor));
		}
		
		protected HistoryAction(String text, String imageDescriptor, int style) {
			super(SVNTeamUIPlugin.instance().getResource(text), style);
			this.setHoverImageDescriptor(imageDescriptor == null ? null : SVNTeamUIPlugin.instance().getImageDescriptor(imageDescriptor));
		}
		
	}
	
	protected ISVNHistoryView view;
	protected long selectedRevision;
	
	public final IControlActionManager affectedTableManager;
	public final IControlActionManager affectedTreeManager;
	public final IControlActionManager logMessagesManager;
	
	public HistoryActionManager(ISVNHistoryView info) {
		this.view = info;
		this.affectedTableManager = new AffectedTableActionManager();
		this.affectedTreeManager = new AffectedTreeActionManager();
		this.logMessagesManager = new LogMessagesActionManager();
	}

	public long getSelectedRevision() {
		return this.selectedRevision;
	}

	public void setSelectedRevision(long selectedRevision) {
		this.selectedRevision = selectedRevision;
	}
	
	protected class LogMessagesActionManager implements IControlActionManager {
		public void installKeyBindings(final StructuredViewer viewer) {
			viewer.getControl().addKeyListener(new KeyAdapter() {
	        	public void keyPressed(KeyEvent event) {
	        		if (event.keyCode == SWT.F5) {
	        			HistoryActionManager.this.view.refresh(ISVNHistoryView.REFRESH_ALL);
	        		}
	        		if (event.stateMask == SWT.CTRL && (event.keyCode == 'c' || event.keyCode == 'C')) {
	        			ILogNode []selection = LogMessagesActionManager.this.getSelection(viewer);
	        			if (selection.length > 0) {
		        			HistoryActionManager.this.handleCopy(selection);
	        			}
	        		}
	        	}
	        });
		}

		public void installDefaultAction(final StructuredViewer viewer) {
			viewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent e) {
					ISelection selection = e.getSelection();
					if (selection instanceof IStructuredSelection) {
						IStructuredSelection structured = (IStructuredSelection)selection;
						if (structured.size() == 1) {
							HistoryActionManager.this.handleDoubleClick((TreeViewer)viewer, (ILogNode)structured.getFirstElement(), true);
						}
					}
				}
			});
		}
		
		public void installMenuActions(final StructuredViewer viewer, IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
					
					HistoryAction refreshAction = new HistoryAction("HistoryView.Refresh", "icons/common/refresh.gif") {
						public void run() {
							HistoryActionManager.this.view.refresh(ISVNHistoryView.REFRESH_ALL);
						}
					};
					
					ILogNode []selection = LogMessagesActionManager.this.getSelection(viewer);
					if (selection.length == 0 || selection[0].getType() == ILogNode.TYPE_NONE) {
						manager.add(refreshAction);
						return;
					}
					
					boolean onlyLogEntries = true;
					boolean onlyLocalEntries = true;
					boolean containsCategory = false;
					for (ILogNode node : selection) {
						int type = node.getType();
						if (type != ILogNode.TYPE_SVN) {
							onlyLogEntries = false;
						}
						if (type != ILogNode.TYPE_LOCAL) {
							onlyLocalEntries = false;
						}
						if (type == ILogNode.TYPE_CATEGORY) {
							onlyLogEntries = false;
							onlyLocalEntries = false;
							containsCategory = true;
						}
					}
					
					if (onlyLogEntries) {
						LogMessagesActionManager.this.addRemotePart(viewer, manager, selection);
						
						manager.add(new Separator());
					}
					if (onlyLocalEntries) {
						LogMessagesActionManager.this.addLocalPart(viewer, manager, selection);
						
						manager.add(new Separator());
					}
					if (!onlyLogEntries && !onlyLocalEntries && !containsCategory) {
						LogMessagesActionManager.this.addLocalOrRemotePart(viewer, manager, selection);
						
						manager.add(new Separator());
					}
					if (!onlyLogEntries) {
						LogMessagesActionManager.this.addCommonPart(viewer, manager, selection);
						
						manager.add(new Separator());
					}
					
					manager.add(refreshAction);
				}
			});
	        menuMgr.setRemoveAllWhenShown(true);
	        viewer.getControl().setMenu(menu);
	        site.registerContextMenu(menuMgr, viewer);
		}
		
		protected void addRemotePart(final StructuredViewer viewer, IMenuManager manager, final ILogNode []selection) {
			Action tAction = null;
			if (HistoryActionManager.this.view.getRepositoryResource() instanceof IRepositoryFile) {
				String name = HistoryActionManager.this.view.getRepositoryResource().getName();
				manager.add(tAction = new HistoryAction("HistoryView.Open") {
					public void run() {
						HistoryActionManager.this.handleDoubleClick((TreeViewer)viewer, selection[0], false);
					}
				});
				tAction.setEnabled(selection.length == 1);
				tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry().getImageDescriptor(name));
				
				//FIXME: "Open with" submenu shouldn't be hardcoded after reworking of
				//       the HistoryView. Should be made like the RepositoriesView menu.
				MenuManager sub = new MenuManager(SVNTeamUIPlugin.instance().getResource("HistoryView.OpenWith"), "historyOpenWithMenu");
				sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				
				sub.add(new Separator("nonDefaultTextEditors"));
				IEditorDescriptor[] editors = SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry().getEditors(name);
				for (int i = 0; i < editors.length; i++) {
    				if (!editors[i].getId().equals(EditorsUI.DEFAULT_TEXT_EDITOR_ID)) {
    					HistoryActionManager.this.addMenuItem(viewer, sub, editors[i].getLabel(), new OpenFileWithAction(editors[i].getId(), false));
    				}
    			}
					
				sub.add(new Separator("variousEditors"));
				HistoryActionManager.this.addMenuItem(viewer, sub, SVNTeamUIPlugin.instance().getResource("HistoryView.TextEditor"), new OpenFileWithAction());
				HistoryActionManager.this.addMenuItem(viewer, sub, SVNTeamUIPlugin.instance().getResource("HistoryView.SystemEditor"), new OpenFileWithExternalAction());
				HistoryActionManager.this.addMenuItem(viewer, sub, SVNTeamUIPlugin.instance().getResource("HistoryView.InplaceEditor"), new OpenFileWithInplaceAction());
				HistoryActionManager.this.addMenuItem(viewer, sub, SVNTeamUIPlugin.instance().getResource("HistoryView.DefaultEditor"), new OpenFileAction());
					
	        	manager.add(sub);
	        	
	        	manager.add(new Separator());
			}
			
    		boolean isCompareAllowed = 
    			CoreExtensionsManager.instance().getSVNConnectorFactory().getSVNAPIVersion() == ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x ||
    			HistoryActionManager.this.view.getRepositoryResource() instanceof IRepositoryFile;
    		
			manager.add(tAction = new HistoryAction("HistoryView.CompareEachOther") {
				public void run() {
					SVNLogEntry msg0 = (SVNLogEntry)selection[0].getEntity();
					SVNLogEntry msg1 = (SVNLogEntry)selection[1].getEntity();
					IRepositoryResource next = HistoryActionManager.this.getResourceForSelectedRevision(msg0);
					IRepositoryResource prev = HistoryActionManager.this.getResourceForSelectedRevision(msg1);
					if (msg0.revision < msg1.revision) {
						IRepositoryResource tmp = prev;
						prev = next;
						next = tmp;
					}
					CompareRepositoryResourcesOperation op = new CompareRepositoryResourcesOperation(prev, next);
					op.setForceId(HistoryActionManager.this.getCompareForceId());
					UIMonitorUtility.doTaskScheduledActive(op);
				}
			});
			tAction.setEnabled(selection.length == 2 && isCompareAllowed);
			if (selection.length == 1) {
				final SVNLogEntry current = (SVNLogEntry)selection[0].getEntity();
				String revision = HistoryActionManager.this.view.getResource() != null ? String.valueOf(current.revision) : SVNTeamUIPlugin.instance().getResource("HistoryView.HEAD");
				
				manager.add(tAction = new HistoryAction("HistoryView.CompareCurrentWith", new String[] {revision}) {
					public void run() {
						HistoryActionManager.this.compareWithCurrent(current);
					}
				});
				tAction.setEnabled(isCompareAllowed);
			}
			manager.add(tAction = new HistoryAction("HistoryView.CompareWithPrevious") {
				public void run() {
					HistoryActionManager.this.compareWithPreviousRevision(null, new IRepositoryResourceProvider() {
						public IRepositoryResource[] getRepositoryResources() {
							SVNLogEntry current = (SVNLogEntry)selection[0].getEntity();
							return new IRepositoryResource[] {HistoryActionManager.this.getResourceForSelectedRevision(current)};
						}
					});
				}
			});
			boolean existsInPrevious = HistoryActionManager.this.view.getFullRemoteHistory()[HistoryActionManager.this.view.getFullRemoteHistory().length - 1] != selection[0].getEntity() || !HistoryActionManager.this.view.isAllRemoteHistoryFetched();
			tAction.setEnabled(selection.length == 1 && isCompareAllowed && existsInPrevious);
			
			manager.add(new Separator());
			
			manager.add(tAction = new HistoryAction("ShowPropertiesAction.label", "icons/views/propertiesedit.gif") {
				public void run() {
					SVNLogEntry current = (SVNLogEntry)selection[0].getEntity();
					IRepositoryResource resource = HistoryActionManager.this.getResourceForSelectedRevision(current);
					IResourcePropertyProvider provider = new GetRemotePropertiesOperation(resource);
					ShowPropertiesOperation mainOp = new ShowPropertiesOperation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), resource, provider);
					CompositeOperation op = new CompositeOperation(mainOp.getId());
					op.add(provider);
					op.add(mainOp, new IActionOperation[] {provider});
					if (!mainOp.isEditorOpened()) {
						UIMonitorUtility.doTaskScheduledActive(op);
					}
				}
			});
			tAction.setEnabled(selection.length == 1);
			if (HistoryActionManager.this.view.getRepositoryResource() instanceof IRepositoryFile) {
				manager.add(tAction = new HistoryAction("ShowAnnotationCommand.label") {
					public void run() {
				        IRepositoryResource remote = HistoryActionManager.this.getResourceForSelectedRevision((SVNLogEntry)selection[0].getEntity());
						UIMonitorUtility.doTaskScheduledDefault(new RemoteShowAnnotationOperation(remote));
					}
				});
				tAction.setEnabled(selection.length == 1);
			}
			
			manager.add(new Separator());
			
			manager.add(tAction = new HistoryAction("ExportCommand.label", "icons/common/export.gif") {
				public void run() {
					HistoryActionManager.this.doExport(selection[0]);
				}
			});
			tAction.setEnabled(selection.length == 1);
			manager.add(tAction = new HistoryAction("CreatePatchCommand.label") {
				public void run() {
					HistoryActionManager.this.createPatch(selection);
				}
			});
			tAction.setEnabled(selection.length == 1 && existsInPrevious || selection.length == 2);
			if (HistoryActionManager.this.view.getResource() != null) {
				manager.add(tAction = new HistoryAction("HistoryView.GetContents") {
					public void run() {
						if (HistoryActionManager.this.confirmReplacement()) {
							HistoryActionManager.this.getRevisionContents((SVNLogEntry)selection[0].getEntity());
						}
					}
				});
				tAction.setEnabled(selection.length == 1);
			}
			if (HistoryActionManager.this.view.getResource() != null)  {
				manager.add(tAction = new HistoryAction("HistoryView.UpdateTo") {
					public void run() {
						if (HistoryActionManager.this.confirmReplacement()) {
							HistoryActionManager.this.updateTo((SVNLogEntry)selection[0].getEntity());
						}
					}
				});
				tAction.setEnabled(selection.length == 1);
			}
			
			manager.add(new Separator());

			String branchFrom = SVNTeamUIPlugin.instance().getResource("HistoryView.BranchFromRevision");
			String tagFrom = SVNTeamUIPlugin.instance().getResource("HistoryView.TagFromRevision");
			if (selection.length == 1) {
				String revision = String.valueOf(((SVNLogEntry)selection[0].getEntity()).revision);
				branchFrom = SVNTeamUIPlugin.instance().getResource("HistoryView.BranchFrom", new String[] {revision});
				tagFrom = SVNTeamUIPlugin.instance().getResource("HistoryView.TagFrom", new String[] {revision});
			}
			manager.add(tAction = new HistoryAction(branchFrom, "icons/common/actions/branch.gif") {
				public void run() {
					PreparedBranchTagOperation op = BranchTagAction.getBranchTagOperation(new IRepositoryResource[] {HistoryActionManager.this.getResourceForSelectedRevision((SVNLogEntry)selection[0].getEntity())}, UIMonitorUtility.getShell(), BranchTagAction.BRANCH_ACTION, false);
					if (op != null) {
						UIMonitorUtility.doTaskScheduledActive(op);
					}
				}
			});
			tAction.setEnabled(selection.length == 1);
			manager.add(tAction = new HistoryAction(tagFrom, "icons/common/actions/tag.gif") {
				public void run() {
					PreparedBranchTagOperation op = BranchTagAction.getBranchTagOperation(new IRepositoryResource[] {HistoryActionManager.this.getResourceForSelectedRevision((SVNLogEntry)selection[0].getEntity())}, UIMonitorUtility.getShell(), BranchTagAction.TAG_ACTION, false);
					if (op != null) {
						UIMonitorUtility.doTaskScheduledActive(op);
					}
				}
			});
			tAction.setEnabled(selection.length == 1);
			manager.add(tAction = new HistoryAction("AddRevisionLinkAction.label") {
				public void run() {
					HistoryActionManager.this.addRevisionLinks(selection);
				}
			});
			tAction.setEnabled(selection.length == 1);
			
			manager.add(new Separator());
			
			manager.add(tAction = new HistoryAction("HistoryView.CopyHistory", "icons/common/copy.gif") {
				public void run() {
					HistoryActionManager.this.handleCopy(selection);
				}
			});
			tAction.setEnabled(selection.length > 0);
			
			manager.add(new Separator());
			
		    manager.add(tAction = new HistoryAction("HistoryView.QuickFilter", "icons/views/history/filter.gif") {
		        public void run() {
		            HistoryActionManager.this.view.setFilter();
		        }
		    });
		    manager.add(tAction = new HistoryAction("HistoryView.ClearFilter", "icons/views/history/clear_filter.gif") {
		        public void run() {
		        	HistoryActionManager.this.view.clearFilter();
		        }
		    });
		    tAction.setEnabled(HistoryActionManager.this.view.isFilterEnabled());
		}
		
		protected void addLocalPart(final StructuredViewer viewer, IMenuManager manager, final ILogNode []selection) {
			Action tAction = null;
			manager.add(tAction = new HistoryAction("HistoryView.Open") {
				public void run() {
					HistoryActionManager.this.handleDoubleClick((TreeViewer)viewer, selection[0], false);
				}
			});
			tAction.setEnabled(selection.length == 1);
			tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry().getImageDescriptor(((SVNLocalFileRevision)selection[0].getEntity()).getName()));
			
			manager.add(new Separator());
			
			manager.add(tAction = new HistoryAction("HistoryView.CompareEachOther") {
				public void run() {
					HistoryActionManager.this.runCompareForLocal(selection);
				}
			});
			tAction.setEnabled(selection.length == 2);
			manager.add(tAction = new HistoryAction("HistoryView.CompareCurrentWith", new String[] {SVNTeamUIPlugin.instance().getResource("HistoryView.RevisionLocal")}) {
				public void run() {
					HistoryActionManager.this.runCompareForLocal(selection);
				}
			});
			tAction.setEnabled(selection.length == 1);
			manager.add(tAction = new HistoryAction("HistoryView.CompareWithPrevious") {
				public void run() {
					SVNLocalFileRevision [] localHistory = HistoryActionManager.this.view.getLocalHistory();
					SVNLocalFileRevision currentSelected = (SVNLocalFileRevision)selection[0].getEntity();
					ArrayList<SVNLocalFileRevision> toOperate = new ArrayList<SVNLocalFileRevision>();
					toOperate.add(currentSelected);
					for (int i = 0; i < localHistory.length - 1; i++) {
						if (currentSelected.equals(localHistory[i])) {
							toOperate.add(localHistory[i + 1]);
							break;
						}
					}
					HistoryActionManager.this.runCompareForLocal(toOperate.toArray());
				}
			});
			tAction.setEnabled(selection.length == 1 && !selection[0].getEntity().equals(HistoryActionManager.this.view.getLocalHistory()[HistoryActionManager.this.view.getLocalHistory().length - 1]));
			
			manager.add(new Separator());
			
			manager.add(tAction = new HistoryAction("ExportCommand.label", "icons/common/export.gif") {
				public void run() {
					HistoryActionManager.this.doExport(selection[0]);
				}
			});
			tAction.setEnabled(selection.length == 1);
			manager.add(tAction = new HistoryAction("HistoryView.GetContents") {
				public void run() {
					try {
						((IFile)HistoryActionManager.this.view.getResource()).setContents(((SVNLocalFileRevision)selection[0].getEntity()).getState(), true, true, new NullProgressMonitor());
					}
					catch (CoreException ex) {
						UILoggedOperation.reportError(this.getText(), ex);
					}
				}
			});
			tAction.setEnabled(selection.length == 1 && (!((SVNLocalFileRevision)selection[0].getEntity()).isCurrentState()));
		}
		
		protected void addLocalOrRemotePart(StructuredViewer viewer, IMenuManager manager, final ILogNode []selection) {
			Action tAction = null;
			manager.add(tAction = new HistoryAction("HistoryView.CompareEachOther") {
				public void run() {
					ArrayList<Object> selected = new ArrayList<Object>();
					for (ILogNode item : selection) {
						if (item.getType() == ILogNode.TYPE_LOCAL) {
							selected.add(item);
						}
						else {
							selected.add(new SVNRemoteResourceRevision(
									HistoryActionManager.this.getResourceForSelectedRevision((SVNLogEntry)item.getEntity()),
									(SVNLogEntry)item.getEntity()));
						}
					}
					HistoryActionManager.this.runCompareForLocal(selected.toArray());
				}
			});
			tAction.setEnabled(selection.length == 2);
		}
		
		protected void addCommonPart(StructuredViewer viewer, IMenuManager manager, final ILogNode []selection) {
			Action tAction = null;
			manager.add(tAction = new HistoryAction("HistoryView.CopyHistory", "icons/common/copy.gif") {
				public void run() {
					HistoryActionManager.this.handleCopy(selection);
				}
			});
			tAction.setEnabled(selection.length > 0);
		}
		
		protected ILogNode []getSelection(StructuredViewer viewer) {
			List selection = ((IStructuredSelection)viewer.getSelection()).toList();
			return (ILogNode [])selection.toArray(new ILogNode[selection.size()]);
		}

	}
	
	protected void addMenuItem(StructuredViewer viewer, MenuManager menuManager, String label, final AbstractRepositoryTeamAction action) {
		IStructuredSelection tSelection = (IStructuredSelection)viewer.getSelection();
		Action wrapper = new Action(label) {
			public void run() {
				action.run(this);
			}
		};
		IStructuredSelection resourceSelection;
		if (tSelection.size() == 1 && (((ILogNode)tSelection.getFirstElement()).getType() == ILogNode.TYPE_SVN)) {
			resourceSelection = new StructuredSelection(new RepositoryFile(null, this.getResourceForSelectedRevision((SVNLogEntry)((ILogNode)tSelection.getFirstElement()).getEntity())));
		}
		else {
			resourceSelection = new StructuredSelection(StructuredSelection.EMPTY);
		}
		action.selectionChanged(wrapper, resourceSelection);
		menuManager.add(wrapper);
	}
	
	protected void getRevisionContents(SVNLogEntry item) {
		IRepositoryResource remote = this.getResourceForSelectedRevision(item);
		
		// propose user to lock the file if it needs lock
		boolean canWrite = true;
		
		if (this.view.getResource() instanceof IFile) {
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(this.view.getResource());
			if (local != null && !local.isLocked() && IStateFilter.SF_NEEDS_LOCK.accept(local)) {
				canWrite = LockProposeUtility.proposeLock(new IResource[] {this.view.getResource()});
			}
		}
		if (canWrite) {
			GetRemoteContentsOperation mainOp = new GetRemoteContentsOperation(new IResource[] {this.view.getResource()}, new IRepositoryResource[] {remote});
			
			CompositeOperation op = new CompositeOperation(mainOp.getId());
			op.add(mainOp);
			op.add(new RefreshResourcesOperation(new IResource[] {this.view.getResource()}));

			UIMonitorUtility.doTaskScheduledWorkspaceModify(op);
		}
	}
	
	protected void doExport(ILogNode item) {
		if (item.getType() == ILogNode.TYPE_LOCAL) {
			final SVNLocalFileRevision revision = (SVNLocalFileRevision)item.getEntity();
		    FileDialog dlg = new FileDialog(UIMonitorUtility.getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
			dlg.setText(SVNTeamUIPlugin.instance().getResource("ExportPanel.Title"));
			dlg.setFileName(revision.getName());
			dlg.setFilterExtensions(new String[] {"*.*"});
			final String file = dlg.open();
			if (file != null) {
				IActionOperation op = new AbstractActionOperation("Operation.ExportLocalHistory") {
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						FileOutputStream output = new FileOutputStream(file);
						InputStream input = null;
						try {
							IStorage storage = revision.getStorage(monitor);
							input = storage.getContents();
							byte []data = new byte[2048];
							int len = 0;
							while ((len = input.read(data)) > 0) {
								output.write(data, 0, len);
							}
						}
						finally {
							output.close();
							if (input != null) {
								input.close();
							}
						}
					}
				};
		    	UIMonitorUtility.doTaskScheduledDefault(op);
			}
		}
		else {
			DirectoryDialog fileDialog = new DirectoryDialog(UIMonitorUtility.getShell());
			fileDialog.setText(SVNTeamUIPlugin.instance().getResource("ExportPanel.ExportFolder"));
			fileDialog.setMessage(SVNTeamUIPlugin.instance().getResource("ExportPanel.ExportFolder.Msg"));
			String path = fileDialog.open();
			if (path != null) {
				IRepositoryResource resource = this.getResourceForSelectedRevision((SVNLogEntry)item.getEntity());
		    	UIMonitorUtility.doTaskScheduledDefault(new ExportOperation(resource, path));
		    }
		}
	}
	
	protected void addRevisionLinks(ILogNode []tSelection) {
		IRepositoryLocation location = this.view.getRepositoryResource().getRepositoryLocation();
		
		CompositeOperation op = new CompositeOperation("Operation.HAddSelectedRevision");
		for (ILogNode node : tSelection) {
			SVNLogEntry item = (SVNLogEntry)node.getEntity();
			IRepositoryResource resource = SVNUtility.copyOf(this.view.getRepositoryResource());
			resource.setSelectedRevision(SVNRevision.fromNumber(item.revision));
			LocateResourceURLInHistoryOperation locateOp = new LocateResourceURLInHistoryOperation(new IRepositoryResource[] {resource}, true);
			op.add(locateOp);
			op.add(new AddRevisionLinkOperation(locateOp, item.revision), new IActionOperation[] {locateOp});
		}
		op.add(new SaveRepositoryLocationsOperation());
		op.add(new RefreshRepositoryLocationsOperation(new IRepositoryLocation [] {location}, true));
		UIMonitorUtility.doTaskScheduledDefault(op);
	}
	
	protected void updateTo(final SVNLogEntry item) {	    
		IResource []resources = new IResource[] {this.view.getResource()};
	    CompositeOperation op = new CompositeOperation("Operation.HUpdateTo");
		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
		op.add(saveOp);
	    op.add(new UpdateOperation(resources, SVNRevision.fromNumber(item.revision), true));
		op.add(new RestoreProjectMetaOperation(saveOp));
	    op.add(new RefreshResourcesOperation(resources));
		UIMonitorUtility.doTaskScheduledWorkspaceModify(op);
	}
	
	protected void createPatch(ILogNode []selected) {
		CreatePatchWizard wizard = new CreatePatchWizard(this.view.getRepositoryResource().getName());
		WizardDialog dialog = new WizardDialog(UIMonitorUtility.getShell(), wizard);
		if (dialog.open() == DefaultDialog.OK) {
			SVNLogEntry msg0 = (SVNLogEntry)selected[0].getEntity();
			IRepositoryResource next = this.getResourceForSelectedRevision(msg0);
			IRepositoryResource prev = null;
			if (selected.length == 1) {
				//FIXME peg revisions for renamed resources: (rev - 1) works only if the revision really exists in repository for current resource
				// use LocateUrlInHistory
				prev = this.getResourceForSelectedRevision(new SVNLogEntry(msg0.revision - 1, 0, null, null, null));
			}
			else {
				SVNLogEntry msg1 = (SVNLogEntry)selected[1].getEntity();
				prev = this.getResourceForSelectedRevision(msg1);
				if (msg0.revision < msg1.revision) {
					IRepositoryResource tmp = next;
					next = prev;
					prev = tmp;
				}
			}
			UIMonitorUtility.doTaskScheduledDefault(UIMonitorUtility.getActivePart(), CreatePatchAction.getCreatePatchOperation(prev, next, wizard));
		}
	}
	
	protected void handleCopy(ILogNode []selection) {
		String historyText = this.getSelectedMessagesAsString(selection);
		Clipboard clipboard = new Clipboard(UIMonitorUtility.getDisplay());
		try {
			clipboard.setContents(new Object[] {historyText}, new Transfer[] {TextTransfer.getInstance()});
		}
		finally {
			clipboard.dispose();
		}
	}
	
	protected String getSelectedMessagesAsString(ILogNode []selection) {
		String historyText = "";
		HashSet<ILogNode> processed = new HashSet<ILogNode>();
		long revision = this.view.getCurrentRevision();
		for (ILogNode node : selection) {
			historyText += this.toString(processed, node, revision);
			if (node.hasChildren()) {
				ILogNode []children = node.getChildren();
				for (int j = 0; j < children.length; j++) {
					historyText += this.toString(processed, children[j], revision);
				}
			}
		}
		return historyText;
	}
	
	protected String toString(HashSet<ILogNode> processed, ILogNode node, long revision) {
		if (processed.contains(node)) {
			return "";
		}
		processed.add(node);
		String historyText = node.getLabel(0, ILogNode.LABEL_FLAT, revision);
		for (int i = 1; i < ILogNode.NUM_OF_COLUMNS; i++) {
			historyText += "\t" + node.getLabel(i, ILogNode.LABEL_FLAT, revision);
		}
		return historyText + System.getProperty("line.separator");
	}
	
	protected void handleDoubleClick(TreeViewer viewer, ILogNode item, boolean doubleClick) {
		int type = item.getType();
		if (type == ILogNode.TYPE_CATEGORY) {
			if (viewer.getExpandedState(item)) {
				viewer.collapseToLevel(item, TreeViewer.ALL_LEVELS);
			}
			else {
				viewer.expandToLevel(item, TreeViewer.ALL_LEVELS);
			}
			return;
		}
		boolean isCompareAllowed = 
			CoreExtensionsManager.instance().getSVNConnectorFactory().getSVNAPIVersion() == ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x ||
			this.view.getRepositoryResource() instanceof IRepositoryFile;
		if ((this.view.getOptions() & ISVNHistoryView.COMPARE_MODE) != 0 && doubleClick && isCompareAllowed) {
			if (type == ILogNode.TYPE_SVN) {
				this.compareWithCurrent((SVNLogEntry)item.getEntity());
			}
			if (type == ILogNode.TYPE_LOCAL) {
				this.runCompareForLocal(new Object[] {item});
			}
		}
		else if (type == ILogNode.TYPE_LOCAL) {
			try {
				SVNLocalFileRevision selected = (SVNLocalFileRevision)item.getEntity();
				Utils.openEditor(UIMonitorUtility.getActivePage(), selected, new NullProgressMonitor());
			}
			catch (CoreException ex) {
				UILoggedOperation.reportError("Open Editor", ex);
			}
		}
		else if (!(this.view.getRepositoryResource() instanceof IRepositoryContainer) && type == ILogNode.TYPE_SVN) {
			UIMonitorUtility.doTaskScheduledActive(new OpenRemoteFileOperation(new IRepositoryFile[] {(IRepositoryFile)this.getResourceForSelectedRevision((SVNLogEntry)item.getEntity())}, OpenRemoteFileOperation.OPEN_DEFAULT));
		}
	}
	
	protected void compareWithCurrent(SVNLogEntry item) {
		IRepositoryResource resource = this.getResourceForSelectedRevision(item);
		if (this.view.getResource() != null || this.view.getCompareWith() != null) {
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(this.view.getCompareWith());
			CompareResourcesOperation op = new CompareResourcesOperation(local, resource);
			op.setForceId(this.getCompareForceId());
			UIMonitorUtility.doTaskScheduledActive(op);
		}
		else {
			CompareRepositoryResourcesOperation op = new CompareRepositoryResourcesOperation(this.getResourceForHeadRevision(), resource);
			op.setForceId(this.getCompareForceId());
			UIMonitorUtility.doTaskScheduledActive(op);
		}
	}
	
	protected void runCompareForLocal(Object []selection) {
		//FIXME reimplement without internals usage
		ArrayList<FileRevision> newSelection = new ArrayList<FileRevision>();
		for (Object item : selection) {
			if (item instanceof ILogNode) {
				newSelection.add((SVNLocalFileRevision)((ILogNode)item).getEntity());
			}
			else {
				newSelection.add((FileRevision)item);
			}
		}
		CompareRevisionAction compare = null;
		try {
			compare = CompareRevisionAction.class.getConstructor((Class [])null).newInstance((Object [])null);
			compare.getClass().getMethod("setPage", new Class[] {HistoryPage.class}).invoke(compare, this.view.getHistoryPage());
		}
		catch (Exception ex) {
			try {
				compare = CompareRevisionAction.class.getConstructor(new Class[] {String.class, HistoryPage.class}).newInstance(new Object[] {"", this.view.getHistoryPage()});
			}
			catch (RuntimeException ex1) {
				throw ex1;
			}
			catch (Exception ex1) {
				throw new RuntimeException(ex1);
			}
		}
		compare.selectionChanged(new StructuredSelection(newSelection));
		compare.setCurrentFileRevision(new SVNLocalFileRevision((IFile)this.view.getResource()));
		compare.run();
	}
	
	protected IRepositoryResource getResourceForHeadRevision() {
		IRepositoryResource res =
			this.view.getRepositoryResource() instanceof IRepositoryFile ? 
			(IRepositoryResource)((IRepositoryRoot)this.view.getRepositoryResource().getRoot()).asRepositoryFile(this.view.getRepositoryResource().getUrl(), false) : 
			((IRepositoryRoot)this.view.getRepositoryResource().getRoot()).asRepositoryContainer(this.view.getRepositoryResource().getUrl(), false);
		res.setSelectedRevision(SVNRevision.HEAD);
		res.setPegRevision(SVNRevision.HEAD);
		return res;
	}
	
	protected String getCompareForceId() {
		return this.toString();
	}
	
	protected IRepositoryResource getResourceForSelectedRevision(SVNLogEntry item) {
		long revNum = item.revision;
		IRepositoryResource res =
			this.view.getRepositoryResource() instanceof IRepositoryFile ? 
			(IRepositoryResource)((IRepositoryRoot)this.view.getRepositoryResource().getRoot()).asRepositoryFile(this.view.getRepositoryResource().getUrl(), false) : 
			((IRepositoryRoot)this.view.getRepositoryResource().getRoot()).asRepositoryContainer(this.view.getRepositoryResource().getUrl(), false);
		res.setSelectedRevision(SVNRevision.fromNumber(revNum));
		res.setPegRevision(this.view.getRepositoryResource().getPegRevision());
		return res;
	}
	
	protected boolean confirmReplacement() {
		ReplaceWarningDialog dialog = new ReplaceWarningDialog(UIMonitorUtility.getShell());
		return dialog.open() == 0;
	}
	
	protected class AffectedTableActionManager implements IControlActionManager {
		public void installKeyBindings(StructuredViewer viewer) {
		}

		public void installDefaultAction(StructuredViewer viewer) {
			//add double click listener for the table viewer
			viewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent e) {
					if (e.getSelection() instanceof IStructuredSelection) {
						IStructuredSelection selection = (IStructuredSelection)e.getSelection();
						if (selection.size() == 1) {
							SVNChangedPathData data = (SVNChangedPathData)selection.getFirstElement();
							if ((HistoryActionManager.this.view.getOptions() & ISVNHistoryView.COMPARE_MODE) != 0) {
								boolean isPreviousExists = data.action == SVNLogPath.ChangeType.MODIFIED || data.action == SVNLogPath.ChangeType.REPLACED;
								if (isPreviousExists) {
									FromChangedPathDataProvider provider = new FromChangedPathDataProvider(data, false);
									HistoryActionManager.this.compareWithPreviousRevision(provider, provider);
								}
								else {
									MessageDialog dialog = new MessageDialog(e.getViewer().getControl().getShell(), 
											SVNTeamUIPlugin.instance().getResource("AffectedPathsComposite.NoPreviousRevision.Title"), 
											null, 
											SVNTeamUIPlugin.instance().getResource("AffectedPathsComposite.NoPreviousRevision.Message"),
											MessageDialog.INFORMATION, 
											new String[] {IDialogConstants.OK_LABEL}, 
											0);
									dialog.open();								
								}
							}
							else {
								HistoryActionManager.this.openRemoteResource(data, OpenRemoteFileOperation.OPEN_DEFAULT, null);
							}
						}
					}
				}
			});
		}

		public void installMenuActions(final StructuredViewer viewer, IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
					final IStructuredSelection affectedTableSelection = (IStructuredSelection)viewer.getSelection();
					if (affectedTableSelection.size() == 0) {
						return;
					}
					final SVNChangedPathData firstData = (SVNChangedPathData)affectedTableSelection.getFirstElement();
					Action tAction = null;
					
					IEditorRegistry editorRegistry = SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry();
					manager.add(tAction = new HistoryAction("HistoryView.Open") {
						public void run() {
							HistoryActionManager.this.openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_DEFAULT, null);
						}
					});
					String name = firstData.resourceName;
					tAction.setImageDescriptor(editorRegistry.getImageDescriptor(name));
					tAction.setEnabled(affectedTableSelection.size() == 1);
					
					//FIXME: "Open with" submenu shouldn't be hardcoded after reworking of
					//       the HistoryView. Should be made like the RepositoriesView menu.
					MenuManager sub = new MenuManager(SVNTeamUIPlugin.instance().getResource("HistoryView.OpenWith"), "historyOpenWithMenu");
					sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
					
					sub.add(new Separator("nonDefaultTextEditors"));
					IEditorDescriptor[] editors = editorRegistry.getEditors(name);
					for (int i = 0; i < editors.length; i++) {
						final String id = editors[i].getId();
	    				if (!id.equals(EditorsUI.DEFAULT_TEXT_EDITOR_ID)) {
	    					sub.add(tAction = new HistoryAction(editors[i].getLabel()) {
	    						public void run() {
	    							HistoryActionManager.this.openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_SPECIFIED, id);
	    						}
	    					});
	    					tAction.setImageDescriptor(editors[i].getImageDescriptor());
	    					tAction.setEnabled(affectedTableSelection.size() == 1);
	    				}
	    			}
						
					sub.add(new Separator("variousEditors"));
					IEditorDescriptor descriptor = null;
					sub.add(tAction = new HistoryAction("HistoryView.TextEditor") {
						public void run() {
							HistoryActionManager.this.openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_SPECIFIED, EditorsUI.DEFAULT_TEXT_EDITOR_ID);
						}
					});
					descriptor = editorRegistry.findEditor(EditorsUI.DEFAULT_TEXT_EDITOR_ID);
					tAction.setImageDescriptor(descriptor.getImageDescriptor());
					tAction.setEnabled(affectedTableSelection.size() == 1);
					sub.add(tAction = new HistoryAction("HistoryView.SystemEditor") {
						public void run() {
							HistoryActionManager.this.openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_EXTERNAL, null);
						}
					});
					if (editorRegistry.isSystemExternalEditorAvailable(name)) {
						tAction.setImageDescriptor(editorRegistry.getSystemExternalEditorImageDescriptor(name));
						tAction.setEnabled(affectedTableSelection.size() == 1);
					}
					else {
						tAction.setEnabled(false);
					}
					sub.add(tAction = new HistoryAction("HistoryView.InplaceEditor") {
						public void run() {
							HistoryActionManager.this.openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_INPLACE, null);
						}
					});
					if (editorRegistry.isSystemInPlaceEditorAvailable(name)) {
						tAction.setImageDescriptor(editorRegistry.getSystemExternalEditorImageDescriptor(name));
						tAction.setEnabled(affectedTableSelection.size() == 1);
					}
					else {
						tAction.setEnabled(false);
					}
					sub.add(tAction = new HistoryAction("HistoryView.DefaultEditor") {
						public void run() {
							HistoryActionManager.this.openRemoteResource(firstData, OpenRemoteFileOperation.OPEN_DEFAULT, null);
						}
					});
					tAction.setImageDescriptor(editorRegistry.getImageDescriptor(name));
					tAction.setEnabled(affectedTableSelection.size() == 1);
					
		        	manager.add(sub);
		        	
		        	manager.add(new Separator());
		        	
					boolean isPreviousExists = false;
					if (affectedTableSelection.size() == 1) {
						//FIXME copied resources also must be handled
//						isPreviousExists = !(firstData.action == SVNLogPath.ChangeType.ADDED && firstData.copiedFromRevision == SVNRevision.INVALID_REVISION_NUMBER);
						isPreviousExists = firstData.action == SVNLogPath.ChangeType.MODIFIED || firstData.action == SVNLogPath.ChangeType.REPLACED;
					}
					manager.add(tAction = new HistoryAction("AffectedPathsComposite.CompareWithPreviousRevision") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.compareWithPreviousRevision(provider, provider);
						}
					});
					tAction.setEnabled(isPreviousExists);
					
					manager.add(new Separator());
					
					manager.add(tAction = new HistoryAction("ShowPropertiesAction.label", "icons/views/propertiesedit.gif") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.showProperties(provider, provider);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() == 1);
					manager.add(tAction = new HistoryAction("ShowResourceHistoryCommand.label", "icons/views/history.gif") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.showHistory(provider, provider);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() == 1);
					manager.add(tAction = new HistoryAction("ShowAnnotationCommand.label") {
						public void run() {
							HistoryActionManager.this.showAnnotation(firstData);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() == 1);
					
					manager.add(new Separator());
					
					manager.add(tAction = new HistoryAction("ExportCommand.label", "icons/common/export.gif") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.doExport(viewer.getControl().getShell(), provider, provider);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() > 0);
					manager.add(tAction = new HistoryAction("CreatePatchCommand.label") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.createPatchToPrevious(viewer.getControl().getShell(), provider, provider);
						}
					});
					tAction.setEnabled(isPreviousExists);
					
					manager.add(new Separator());
					
					String branchFrom = SVNTeamUIPlugin.instance().getResource("HistoryView.BranchFrom", new String [] {String.valueOf(HistoryActionManager.this.selectedRevision)});
					String tagFrom = SVNTeamUIPlugin.instance().getResource("HistoryView.TagFrom", new String [] {String.valueOf(HistoryActionManager.this.selectedRevision)});
					manager.add(tAction = new HistoryAction(branchFrom, "icons/common/actions/branch.gif") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.createBranchTag(viewer.getControl().getShell(), provider, provider, BranchTagAction.BRANCH_ACTION);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() > 0);
					manager.add(tAction = new HistoryAction(tagFrom, "icons/common/actions/tag.gif") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.createBranchTag(viewer.getControl().getShell(), provider, provider, BranchTagAction.TAG_ACTION);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() > 0);
					manager.add(tAction = new HistoryAction("AddRevisionLinkAction.label") {
						public void run() {
							FromChangedPathDataProvider provider = new FromChangedPathDataProvider(firstData, false);
							HistoryActionManager.this.addRevisionLink(provider, provider);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() > 0);
				}
			});
			menuMgr.setRemoveAllWhenShown(true);
			viewer.getControl().setMenu(menu);
			site.registerContextMenu(menuMgr, viewer);
		}
	}
	
	protected class AffectedTreeActionManager implements IControlActionManager {
		public void installKeyBindings(StructuredViewer viewer) {
		}

		public void installDefaultAction(StructuredViewer viewer) {
		}

		public void installMenuActions(final StructuredViewer viewer, IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
	        menuMgr.addMenuListener(new IMenuListener() {
	            public void menuAboutToShow(IMenuManager manager) {
	                manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	                
	        		final IStructuredSelection affectedTableSelection = (IStructuredSelection)viewer.getSelection();
					if (affectedTableSelection.size() == 0) {
						return;
					}
	        		final AffectedPathsNode node = (AffectedPathsNode)affectedTableSelection.getFirstElement();
	        		
	        		Action tAction = null;
	        		manager.add(tAction = new HistoryAction("AffectedPathsComposite.CompareWithPreviousRevision") {
						public void run() {
							FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.compareWithPreviousRevision(provider, provider);
						}
	        		});
	        		boolean isCompareFoldersAllowed = CoreExtensionsManager.instance().getSVNConnectorFactory().getSVNAPIVersion() == ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x;
	        		tAction.setEnabled(isCompareFoldersAllowed && HistoryActionManager.this.selectedRevision != 0 && affectedTableSelection.size() == 1 && (node.getStatus() == '\0' || node.getStatus() == SVNLogPath.ChangeType.MODIFIED));
	        		
	        		manager.add(new Separator());
	        		
	        		manager.add(tAction = new HistoryAction("ShowPropertiesAction.label", "icons/views/propertiesedit.gif") {
						public void run() {
							FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.showProperties(provider, provider);
						}
	        		});
	        		tAction.setEnabled(HistoryActionManager.this.selectedRevision != 0 && affectedTableSelection.size() == 1 /*&& (node.getStatus() == null || node.getStatus().charAt(0) == 'M')*/);
	        		manager.add(tAction = new HistoryAction("ShowResourceHistoryCommand.label", "icons/views/history.gif") {
						public void run() {
							FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.showHistory(provider, provider);
						}
	        		});
	        		tAction.setEnabled(HistoryActionManager.this.selectedRevision != 0 && affectedTableSelection.size() == 1);
	        		
					manager.add(new Separator());
					
					manager.add(tAction = new HistoryAction("ExportCommand.label", "icons/common/export.gif") {
						public void run() {
							FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.doExport(viewer.getControl().getShell(), provider, provider);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() > 0);
	        		manager.add(tAction = new HistoryAction("CreatePatchCommand.label") {
						public void run() {					
							FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.createPatchToPrevious(viewer.getControl().getShell(), provider, provider);
						}
					});
	        		tAction.setEnabled(affectedTableSelection.size() == 1 && HistoryActionManager.this.selectedRevision != 0 && affectedTableSelection.size() == 1 && (node.getStatus() == '\0' || node.getStatus() == SVNLogPath.ChangeType.MODIFIED));
	        		
	        		manager.add(new Separator());
	        		
	        		manager.add(tAction = new HistoryAction("HistoryView.BranchFrom", new String [] {String.valueOf(HistoryActionManager.this.selectedRevision)}, "icons/common/actions/branch.gif") {
	        			public void run() {
	        				FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.createBranchTag(viewer.getControl().getShell(), provider, provider, BranchTagAction.BRANCH_ACTION);
	        			}
	        		});
	        		tAction.setEnabled(affectedTableSelection.size() > 0);
	        		manager.add(tAction = new HistoryAction("HistoryView.TagFrom", new String [] {String.valueOf(HistoryActionManager.this.selectedRevision)}, "icons/common/actions/tag.gif") {
	        			public void run() {
	        				FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.createBranchTag(viewer.getControl().getShell(), provider, provider, BranchTagAction.TAG_ACTION);
	        			}
	        		});
	        		tAction.setEnabled(affectedTableSelection.size() > 0);
	        		manager.add(tAction = new HistoryAction("AddRevisionLinkAction.label") {
						public void run() {
							FromAffectedPathsNodeProvider provider = new FromAffectedPathsNodeProvider(node);
							HistoryActionManager.this.addRevisionLink(provider, provider);
						}
					});
					tAction.setEnabled(affectedTableSelection.size() > 0);
	            }
	        });
	        menuMgr.setRemoveAllWhenShown(true);
	        viewer.getControl().setMenu(menu);
	        site.registerContextMenu(menuMgr, viewer);
		}
		
	}
	
	protected void createPatchToPrevious(Shell shell, IActionOperation preOp, IRepositoryResourceProvider provider) {
		if (UIMonitorUtility.doTaskNowDefault(shell, preOp, true).isCancelled()) {
			return;
		}
		
		IRepositoryResource current = provider.getRepositoryResources()[0];
		CreatePatchWizard wizard = new CreatePatchWizard(current.getName());
		WizardDialog dialog = new WizardDialog(shell, wizard);
		if (dialog.open() == DefaultDialog.OK) {
			IRepositoryResource previous = (current instanceof RepositoryFolder) ? current.asRepositoryContainer(current.getUrl(), false) : current.asRepositoryFile(current.getUrl(), false);
			previous.setSelectedRevision(SVNRevision.fromNumber(HistoryActionManager.this.selectedRevision - 1));
			previous.setPegRevision(SVNRevision.fromNumber(HistoryActionManager.this.selectedRevision));
			UIMonitorUtility.doTaskScheduledDefault(CreatePatchAction.getCreatePatchOperation(previous, current, wizard));
		}
	}
	
	protected void createBranchTag(Shell shell, IActionOperation preOp, IRepositoryResourceProvider provider, int type) {
		if (UIMonitorUtility.doTaskNowDefault(shell, preOp, true).isCancelled()) {
			return;
		}
		
		boolean respectProjectStructure = SVNTeamPreferences.getRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_NAME);
		
		IRepositoryResource []resources = provider.getRepositoryResources();
		PreparedBranchTagOperation op = BranchTagAction.getBranchTagOperation(resources, shell, type, respectProjectStructure);

		if (op != null) {
			CompositeOperation composite = new CompositeOperation(op.getId());
			composite.add(op);
			RefreshRemoteResourcesOperation refreshOp = new RefreshRemoteResourcesOperation(new IRepositoryResource[] {op.getDestination().getParent()});
			composite.add(refreshOp, new IActionOperation[] {op});
			UIMonitorUtility.doTaskScheduledDefault(op);
		}
	}
	
	protected void doExport(Shell shell, IActionOperation preOp, IRepositoryResourceProvider provider) {
		DirectoryDialog fileDialog = new DirectoryDialog(shell);
		fileDialog.setText(SVNTeamUIPlugin.instance().getResource("ExportPanel.ExportFolder"));
		fileDialog.setMessage(SVNTeamUIPlugin.instance().getResource("ExportPanel.ExportFolder.Msg"));
		String path = fileDialog.open();
		if (path != null) {
			ExportOperation mainOp = new ExportOperation(provider, path);
			CompositeOperation op = new CompositeOperation(mainOp.getId());
			op.add(preOp);
			op.add(mainOp, new IActionOperation[] {preOp});
	    	UIMonitorUtility.doTaskScheduledDefault(op);
		}		
	}
	
	protected void openRemoteResource(SVNChangedPathData selectedPath, int openType, String openWith) {
		FromChangedPathDataProvider provider = new FromChangedPathDataProvider(selectedPath, true);
		OpenRemoteFileOperation openOp = new OpenRemoteFileOperation(provider, openType, openWith);
		
		CompositeOperation composite = new CompositeOperation(openOp.getId(), true);
		composite.add(provider);
		composite.add(openOp, new IActionOperation[] {provider});
		UIMonitorUtility.doTaskScheduledActive(composite);
	}
	
	protected void showAnnotation(SVNChangedPathData selectedPath) {
		FromChangedPathDataProvider provider = new FromChangedPathDataProvider(selectedPath, true);
		RemoteShowAnnotationOperation mainOp = new RemoteShowAnnotationOperation(provider);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), true);
		op.add(provider);
		op.add(mainOp, new IActionOperation[] {provider});
		UIMonitorUtility.doTaskScheduledActive(op);
	}
	
	protected void showHistory(IActionOperation preOp, IRepositoryResourceProvider provider) {
		ShowHistoryViewOperation mainOp = new ShowHistoryViewOperation(provider, 0, 0);
		CompositeOperation op = new CompositeOperation(mainOp.getId());
		op.add(preOp);
		op.add(mainOp, new IActionOperation[] {preOp});
		UIMonitorUtility.doTaskScheduledActive(op);
	}
	
	protected void showProperties(IActionOperation preOp, IRepositoryResourceProvider provider) {
		IResourcePropertyProvider propertyProvider = new GetRemotePropertiesOperation(provider);
		ShowPropertiesOperation showOp = new ShowPropertiesOperation(UIMonitorUtility.getActivePage(), provider, propertyProvider);
		CompositeOperation op = new CompositeOperation(showOp.getId());
		op.add(preOp);
		op.add(propertyProvider, new IActionOperation[] {preOp});
		op.add(showOp, new IActionOperation[] {preOp, propertyProvider});
		UIMonitorUtility.doTaskScheduledActive(op);
	}
	
	protected void addRevisionLink(IActionOperation preOp, IRepositoryResourceProvider provider) {
		CompositeOperation op = new CompositeOperation("Operation.HAddSelectedRevision");
		op.add(preOp);
		IActionOperation []condition = new IActionOperation[] {preOp};
		op.add(new AddRevisionLinkOperation(provider, this.selectedRevision), condition);
		op.add(new SaveRepositoryLocationsOperation(), condition);
		op.add(new RefreshRepositoryLocationsOperation(new IRepositoryLocation [] {this.view.getRepositoryResource().getRepositoryLocation()}, true), condition);
		UIMonitorUtility.doTaskScheduledDefault(op);
	}
	
	protected void compareWithPreviousRevision(IActionOperation preOp, final IRepositoryResourceProvider provider) {
		CompareRepositoryResourcesOperation mainOp = new CompareRepositoryResourcesOperation(new IRepositoryResourceProvider() {
			public IRepositoryResource[] getRepositoryResources() {
				IRepositoryResource next = provider.getRepositoryResources()[0];
				IRepositoryResource prev = SVNUtility.copyOf(next);
				prev.setSelectedRevision(SVNRevision.fromNumber(((SVNRevision.Number)next.getSelectedRevision()).getNumber() - 1));
				return new IRepositoryResource[] {prev, next};
			}
		});
		mainOp.setForceId(this.getCompareForceId());
		if (preOp != null) {
			CompositeOperation op = new CompositeOperation(mainOp.getId());
			op.add(preOp);
			op.add(mainOp, new IActionOperation[] {preOp});
			UIMonitorUtility.doTaskScheduledActive(op);
		}
		else {
			UIMonitorUtility.doTaskScheduledActive(mainOp);
		}
	}
	
	protected class FromAffectedPathsNodeProvider extends AbstractActionOperation implements IRepositoryResourceProvider {
		protected AffectedPathsNode affectedPathsItem;
		protected IRepositoryResource returnResource;
		
		public FromAffectedPathsNodeProvider(AffectedPathsNode affectedPathsItem) {
			super("Operation.GetRepositoryResource");
			this.affectedPathsItem = affectedPathsItem;
		}
		
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			String rootUrl = HistoryActionManager.this.view.getRepositoryResource().getRepositoryLocation().getRepositoryRootUrl();
			String path = this.affectedPathsItem.getFullPath();

			String resourceUrl = rootUrl + (path.startsWith("/") ? "" : "/") + path;
			SVNRevision revision = SVNRevision.fromNumber(HistoryActionManager.this.selectedRevision);
			
			IRepositoryLocation location = HistoryActionManager.this.view.getRepositoryResource().getRepositoryLocation();
			this.returnResource = location.asRepositoryContainer(resourceUrl, false);
			this.returnResource.setSelectedRevision(revision);
			this.returnResource.setPegRevision(revision);
		}
		public IRepositoryResource[] getRepositoryResources() {
			return new IRepositoryResource[] {this.returnResource};
		}
	}
	
	protected class FromChangedPathDataProvider extends AbstractActionOperation implements IRepositoryResourceProvider {
		protected IRepositoryResource []repositoryResources;
		protected SVNChangedPathData affectedPathsItem;
		protected boolean filesOnly;
		
		public FromChangedPathDataProvider(SVNChangedPathData affectedPathsItem, boolean filesOnly) {
			super("Operation.GetRepositoryResource");
			this.affectedPathsItem = affectedPathsItem;
			this.filesOnly = filesOnly;
		}
		
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			String affectedPath = this.affectedPathsItem.getFullResourcePath();
			String rootUrl = HistoryActionManager.this.view.getRepositoryResource().getRepositoryLocation().getRepositoryRootUrl();
			String resourceUrl = rootUrl + "/" + affectedPath;
			SVNRevision revision = SVNRevision.fromNumber(this.affectedPathsItem.action == SVNLogPath.ChangeType.DELETED ? HistoryActionManager.this.selectedRevision - 1 : HistoryActionManager.this.selectedRevision);
			
			SVNEntryInfo info = null;
			IRepositoryLocation location = HistoryActionManager.this.view.getRepositoryResource().getRepositoryLocation();
			ISVNConnector proxy = location.acquireSVNProxy();
			try {
				SVNEntryInfo []infos = SVNUtility.info(proxy, new SVNEntryRevisionReference(SVNUtility.encodeURL(resourceUrl), revision, revision), Depth.EMPTY, new SVNProgressMonitor(this, monitor, null));
				if (infos == null || infos.length == 0) {
					return;
				}
				info = infos[0];
			}
			finally {
				location.releaseSVNProxy(proxy);
			}
			
			if (info.kind == Kind.DIR && this.filesOnly) {
				final String message = SVNTeamUIPlugin.instance().getResource("AffectedPathsComposite.Open.Message", new String[] {SVNUtility.decodeURL(info.url)});
				UIMonitorUtility.getDisplay().syncExec(new Runnable() {
					public void run() {
						MessageDialog dialog = new MessageDialog(UIMonitorUtility.getDisplay().getActiveShell(), 
								SVNTeamUIPlugin.instance().getResource("AffectedPathsComposite.Open.Title"), 
								null, 
								message,
								MessageDialog.INFORMATION, 
								new String[] {IDialogConstants.OK_LABEL}, 
								0);
						dialog.open();								
					}
				});
				this.reportStatus(new Status(IStatus.WARNING, SVNTeamPlugin.NATURE_ID, IStatus.OK, message, null));
				return;					
			}
			this.repositoryResources = new IRepositoryResource[1];
			this.repositoryResources[0] = info.kind == Kind.FILE ? (IRepositoryResource)location.asRepositoryFile(resourceUrl, false) : location.asRepositoryContainer(resourceUrl, false);
			this.repositoryResources[0].setSelectedRevision(revision);
			this.repositoryResources[0].setPegRevision(revision);
		}
		
		public IRepositoryResource[] getRepositoryResources() {
			return this.repositoryResources;
		}
		
	}

}