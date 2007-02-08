/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.alfresco.service.cmr.lock;

import java.text.MessageFormat;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Runtime exception class
 * 
 * @author Roy Wetherall
 */
public class UnableToReleaseLockException extends RuntimeException
{
    /**
     * Serial verison UID
     */
    private static final long serialVersionUID = 3257565088071432243L;
    
    /**
     * Error message
     */
    private static final String ERROR_MESSAGE = I18NUtil.getMessage("lock_service.insufficent_privileges");

    /**
     * Constructor
     */
    public UnableToReleaseLockException(NodeRef nodeRef)
    {
        super(MessageFormat.format(ERROR_MESSAGE, new Object[]{nodeRef.getId()}));
    }
}
