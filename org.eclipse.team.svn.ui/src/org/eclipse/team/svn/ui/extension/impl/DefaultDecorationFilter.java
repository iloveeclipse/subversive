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

package org.eclipse.team.svn.ui.extension.impl;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.ui.decorator.IDecorationFilter;

/**
 * Default decoration filter implementation
 * 
 * @author Alexander Gurov
 */
public class DefaultDecorationFilter implements IDecorationFilter {
	public boolean isAcceptable(IResource resource) {
		return true;
	}

}
