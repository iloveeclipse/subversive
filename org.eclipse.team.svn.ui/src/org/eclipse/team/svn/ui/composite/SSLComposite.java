/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sergiy Logvin - Initial API and implementation
 *    Markus Oberlassnig  (ilogs information logistics GmbH) - MSCAPI support via SVNKit 
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import java.security.Provider;
import java.security.Security;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.SSLCertificateResourceVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * SSL Host properties editor panel
 *
 * @author Sergiy Logvin
 */
public class SSLComposite extends Composite implements IPropertiesPanel {
	protected boolean callback;
	
	protected Button enableAuthenticationCheckBox;
	protected Text certificateFileText;
	protected Button browseButton;
	protected Text certificatePassphraseText;
	protected Button savePassphraseCheckBox;
	protected Button mscapiButton;
	protected Button aliasButton;
	protected Text aliasText;
	protected boolean mscapiSupport;
	
	protected IValidationManager validationManager;
	
	protected SSLSettings credentialsInput;

	public SSLComposite(Composite parent, int style, IValidationManager validationManager) {
		this(parent, style, validationManager, false);
	}
	
	public SSLComposite(Composite parent, int style, IValidationManager validationManager, boolean callback) {
		super(parent, style);
		this.validationManager = validationManager;
		this.callback = callback;
		this.credentialsInput = new SSLSettings();
	}

	public SSLSettings getSSLSettingsDirect() {
		SSLSettings settings = new SSLSettings();
		this.getSSLSettingsDirectImpl(settings);
		return settings;
	}
	
	public void setSSLSettingsDirect(SSLSettings settings) {
		this.savePassphraseCheckBox.setSelection(settings.isPassPhraseSaved());
		this.enableAuthenticationCheckBox.setSelection(settings.isAuthenticationEnabled());
		String text = settings.getPassPhrase();
		this.certificatePassphraseText.setText(text == null ? "" : text); //$NON-NLS-1$
		text = settings.getCertificatePath();
		this.certificateFileText.setText(text == null ? "" : text); //$NON-NLS-1$
		
		if (this.callback) {
			if (text != null && text.length() > 0) {
				this.certificatePassphraseText.setFocus();
				this.certificatePassphraseText.selectAll();
			}
			else {
				this.certificateFileText.setFocus();
			}
		}
		
		this.refreshControlsEnablement();
	}
	
	public void initialize() {
		GridLayout layout = null;
		GridData data = null;
		
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 7;
		layout.verticalSpacing = 3;
		data = new GridData(GridData.FILL_BOTH);
		this.setLayout(layout);
		this.setLayoutData(data);
		
		this.enableAuthenticationCheckBox = new Button(this, SWT.CHECK);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		this.enableAuthenticationCheckBox.setLayoutData(data);
		this.enableAuthenticationCheckBox.setText(SVNUIMessages.SSLComposite_EnableAuthentication);
		this.enableAuthenticationCheckBox.addSelectionListener(new SelectionListener() {			
			public void widgetSelected(SelectionEvent e) {
				SSLComposite.this.refreshControlsEnablement();
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}			
		});
		this.mscapiSupport = false;
		Provider pjacapi = Security.getProvider("CAPI"); //$NON-NLS-1$
        Provider pmscapi = Security.getProvider("SunMSCAPI"); //$NON-NLS-1$
        // Check that Java supports MSCAPI
        if (pmscapi != null) {
        	try {
        		ClassLoader.getSystemClassLoader().loadClass("sun.security.mscapi.NONEwithRSASignature");
			} catch (Exception e1) {
				pmscapi = null;
			}
        }
        // ms capi is only suported for windows and for provider SunMSCAPI and JACAPI from keyon
        // further ms capi is only supported from svnkit as client!
        // JAVAHL supports this feature, with version > 1.6.16 and windows 32 platforms 
        String svnClientText = CoreExtensionsManager.instance().getSVNConnectorFactory().getId();
        if (FileUtility.isWindows() && (pjacapi != null || pmscapi != null) && svnClientText.contains("svnkit")) { //$NON-NLS-1$
        	this.mscapiSupport = true;
		}
		if (this.mscapiSupport) {
			this.mscapiButton = new Button(this, SWT.CHECK);
			data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			data.horizontalSpan = 2;
			this.mscapiButton.setLayoutData(data);
			this.mscapiButton.setText("Use MSCAPI");
	    	SelectionListener mscapiSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (mscapiButton.getSelection()) {
						certificateFileText.setText("MSCAPI");
						certificateFileText.setEnabled(false);
						certificatePassphraseText.setEnabled(false);
						browseButton.setEnabled(false);
						aliasButton.setEnabled(true);					
					} else {
						certificateFileText.setEnabled(true);
						certificatePassphraseText.setEnabled(true);
						browseButton.setEnabled(true);
						aliasButton.setEnabled(false);
						aliasText.setText(""); //$NON-NLS-1$
						certificateFileText.setText("");
					}
				}
			};
			this.mscapiButton.addSelectionListener(mscapiSelectionListener);
		}
		/*
		Group group1 = new Group(this, SWT.NONE);
		group1.setText("MSCAPI"+FileUtility.isWindows()+" - JACAPI:"+pjacapi + " - MSCAPI:"+ pmscapi + " - SVN Clien:" + svnClientText);
		layout = new GridLayout();
		layout.verticalSpacing = 12;
		group1.setLayout(layout);
		*/
		Group group = new Group(this, SWT.NONE);
		group.setText(SVNUIMessages.SSLComposite_ClientCertificate);
		layout = new GridLayout();
		layout.verticalSpacing = 12;
		group.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		group.setLayoutData(data);
		
		Composite fileAndPassphrase = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		fileAndPassphrase.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fileAndPassphrase.setLayoutData(data);
		
		Label description = new Label(fileAndPassphrase, SWT.NULL);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.SSLComposite_File);
		
		Composite inner = new Composite(fileAndPassphrase, SWT.FILL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		inner.setLayoutData(data);
		
		this.certificateFileText = new Text(inner, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		this.certificateFileText.setLayoutData(data);
		this.validationManager.attachTo(this.certificateFileText, new AbstractVerifierProxy(new SSLCertificateResourceVerifier(SVNUIMessages.SSLComposite_File_Verifier, true)) {
			protected boolean isVerificationEnabled(Control input) {
				return SSLComposite.this.enableAuthenticationCheckBox.getSelection();
			}
		});
		this.browseButton = new Button (inner, SWT.PUSH);
		this.browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);	
		data.widthHint = DefaultDialog.computeButtonWidth(this.browseButton);
		this.browseButton.setLayoutData(data);
		this.browseButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog fileDialog = new FileDialog(SSLComposite.this.getShell(), SWT.OPEN);
				String res = fileDialog.open();
				if (res != null) {
					SSLComposite.this.certificateFileText.setText(res);
					SSLComposite.this.validationManager.validateContent();
				}
			}
		});
				
		description = new Label(fileAndPassphrase, SWT.NULL);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.SSLComposite_Passphrase);
		
		this.certificatePassphraseText = new Text(fileAndPassphrase, SWT.BORDER | SWT.PASSWORD);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		this.certificatePassphraseText.setLayoutData(data);
		
		if (this.mscapiSupport) { //$NON-NLS-1$
			description = new Label(fileAndPassphrase, SWT.NULL);
			data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			description.setLayoutData(data);
			description.setText("Certificate Alias:"); //$NON-NLS-1$
			
			inner = new Composite(fileAndPassphrase, SWT.FILL);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = layout.marginWidth = 0;
			inner.setLayout(layout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			inner.setLayoutData(data);
			
			this.aliasText = new Text(inner, SWT.BORDER);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
			this.aliasText.setLayoutData(data);			
			aliasText.setEnabled(false);
			
			this.aliasButton = new Button (inner, SWT.PUSH);
			this.aliasButton.setText("Select alias"); //$NON-NLS-1$
			data = new GridData(GridData.HORIZONTAL_ALIGN_END);	
			data.widthHint = DefaultDialog.computeButtonWidth(this.aliasButton);
			this.aliasButton.setLayoutData(data);
			SelectionListener msCapiCertificateSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					SSLClientCertificatesMSCapi dialog = new SSLClientCertificatesMSCapi(Display.getCurrent().getActiveShell(), "Certifacte selection");
			        if (dialog.open() == SSLClientCertificatesMSCapi.OK) {
			            aliasText.setText(dialog.getAlias());
			            certificateFileText.setText("MSCAPI;"+dialog.getAlias());
			        }
				}
			};
			this.aliasButton.addSelectionListener(msCapiCertificateSelectionListener);
			this.aliasButton.setEnabled(false);
		}
		
		inner = new Composite(group, SWT.FILL);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		inner.setLayoutData(data);

		this.savePassphraseCheckBox = new Button(inner, SWT.CHECK);
		this.savePassphraseCheckBox.setText(SVNUIMessages.SSLComposite_SavePassphrase);
		
		new SecurityWarningComposite(inner);
	}
	
	public void setCredentialsInput(SSLSettings input) {
		this.credentialsInput = input;
	}

	public void saveChanges() {
		this.getSSLSettingsDirectImpl(this.credentialsInput);
	}

	public void resetChanges() {
		this.setSSLSettingsDirect(this.credentialsInput);
	}

	public void cancelChanges() {
		
	}

	protected void getSSLSettingsDirectImpl(SSLSettings settings) {
		settings.setAuthenticationEnabled(this.enableAuthenticationCheckBox.getSelection());		
		settings.setCertificatePath(this.certificateFileText.getText());
		settings.setPassPhrase(this.certificatePassphraseText.getText());
		settings.setPassPhraseSaved(this.savePassphraseCheckBox.getSelection());
	}
	
	protected void refreshControlsEnablement() {
		boolean enabled = this.enableAuthenticationCheckBox.getSelection();
		if (enabled && this.mscapiSupport && this.certificateFileText != null && this.certificateFileText.getText() != null && this.certificateFileText.getText().startsWith("MSCAPI")) {
			this.certificateFileText.setEnabled(false);
			this.certificatePassphraseText.setEnabled(false);
			this.browseButton.setEnabled(false);
			this.savePassphraseCheckBox.setEnabled(false);
			this.aliasButton.setEnabled(true);
			this.mscapiButton.setEnabled(true);
			this.mscapiButton.setSelection(true);
			String[] certAlias = this.certificateFileText.getText().split(";");
			if (certAlias.length>1) {
				this.aliasText.setText(certAlias[1]);
			}
		} else {
			this.certificateFileText.setEnabled(enabled);
			this.browseButton.setEnabled(enabled);
			this.certificatePassphraseText.setEnabled(enabled);
			this.savePassphraseCheckBox.setEnabled(enabled);
			if (this.mscapiSupport) {
				this.mscapiButton.setEnabled(enabled);
			}
		}
		this.validationManager.validateContent();
	}

}
