/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sergiy Logvin - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Property name verifier
 * 
 * @author Sergiy Logvin
 */
public class PropertyNameVerifier extends AbstractFormattedVerifier {
    protected static String ERROR_MESSAGE_LETTER;
    protected static String ERROR_MESSAGE_SYMBOLS;
    protected HashSet<String> ignoreStrings;
        
    public PropertyNameVerifier(String fieldName) {
        super(fieldName);
        PropertyNameVerifier.ERROR_MESSAGE_LETTER = SVNUIMessages.format(SVNUIMessages.Verifier_PropertyName_Letter, new String[] {AbstractFormattedVerifier.FIELD_NAME});
        PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS = SVNUIMessages.format(SVNUIMessages.Verifier_PropertyName_Symbols, new String[] {AbstractFormattedVerifier.FIELD_NAME});
        this.ignoreStrings = new HashSet<String>();
        this.ignoreStrings.add(SVNUIMessages.AbstractPropertyEditPanel_svn_description);
        this.ignoreStrings.add(SVNUIMessages.PropertyEditPanel_tsvn_description);
        this.ignoreStrings.add(SVNUIMessages.PropertyEditPanel_bugtraq_description);
        this.ignoreStrings.add(SVNUIMessages.AbstractPropertyEditPanel_custom_description);
        this.ignoreStrings.add("    "  + SVNUIMessages.AbstractPropertyEditPanel_custom_hint); //$NON-NLS-1$
    }

    protected String getErrorMessageImpl(Control input) {
        String property = this.getText(input);
        if (property.trim().length() == 0) {
            return null;
        }
        if (this.ignoreStrings.contains(property)) {
        	return SVNUIMessages.AbstractPropertyEditPanel_Name_Verifier_IgnoreStrings;
        }
        Pattern pattern = Pattern.compile("[a-zA-Z].*"); //$NON-NLS-1$
        Matcher matcher = pattern.matcher(property);
        if (!matcher.matches()) {
        	return PropertyNameVerifier.ERROR_MESSAGE_LETTER;
        }
        pattern = Pattern.compile("[a-zA-Z0-9:\\-_.]*"); //$NON-NLS-1$
        if (!pattern.matcher(property).matches()) {
        	return PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS;
        }
        
        return null;
    }

    protected String getWarningMessageImpl(Control input) {
        return null;
    }

}


