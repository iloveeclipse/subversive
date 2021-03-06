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

package org.eclipse.team.svn.core.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matcher for some string mask
 * 
 * @author Sergiy Logvin 
 */
public class StringMatcher {
    protected static final String NOT_MASKING = "^[a-zA-Z_0-9]$"; //$NON-NLS-1$

    protected Pattern filter;

    public StringMatcher(String mask) {
        this.filter = Pattern.compile(this.getRegexp(mask), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    }

    protected String getRegexp(String pattern) {
        if (pattern.length() == 0) {
			return "\\s*"; //$NON-NLS-1$
		}
        
        Pattern regexp = Pattern.compile(StringMatcher.NOT_MASKING);
        StringBuffer ret = new StringBuffer();
        
        for (int i = 0; i < pattern.length(); i++) {
            String ch = pattern.substring(i, i + 1);
            Matcher matcher = regexp.matcher(ch);
            if (matcher.matches()) {
                ret.append(ch);
            } 
            else if ("?".equals(ch)) { //$NON-NLS-1$
                ret.append("."); //$NON-NLS-1$
            } 
            else if ("*".equals(ch)) { //$NON-NLS-1$
                ret.append(".*"); //$NON-NLS-1$
            } 
            else {
                ret.append("\\"); //$NON-NLS-1$
                ret.append(ch);
            }
        }
        return ret.toString();
    }
    
    public boolean match(String text) {
        return text != null && this.filter.matcher(text).matches();
    }
    
}
