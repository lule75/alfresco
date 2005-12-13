/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.service.cmr.lock;

import java.text.MessageFormat;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Node locked exception class
 * 
 * @author Roy Wetherall
 */
public class NodeLockedException extends AlfrescoRuntimeException
{    
    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 3762254149525582646L;
    
    /**
     * Error message
     */
    private static final String ERROR_MESSAGE = I18NUtil.getMessage("lock_service.no_op");
    private static final String ERROR_MESSAGE_2 = I18NUtil.getMessage("lock_service.no_op2");

    /**
     * @param message
     */
    public NodeLockedException(NodeRef nodeRef)
    {
        super(MessageFormat.format(ERROR_MESSAGE, new Object[]{nodeRef.getId()}));
    }   
    
    public NodeLockedException(NodeRef nodeRef, String operation)
    {
        super(MessageFormat.format(ERROR_MESSAGE_2, new Object[]{operation, nodeRef.getId()}));
    }
}
