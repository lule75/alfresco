/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.event;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.EventCompletionDetails;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.OnCreateReference;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.action.impl.CompleteEventAction;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import sun.security.action.GetBooleanAction;

/**
 * On reference create event type
 * 
 * @author Roy Wetherall
 */
public class OnReferenceCreateEventType extends SimpleRecordsManagementEventTypeImpl
                                        implements RecordsManagementModel,
                                                   OnCreateReference
{
    /** Records management service */
    private RecordsManagementService recordsManagementService;
    
    /** Records managment action service */
    private RecordsManagementActionService recordsManagementActionService;
    
    /** Policy component */
    private PolicyComponent policyComponent;
    
    /** Reference */
    private QName reference;
    
    /**
     * @param recordsManagementService  the records management service to set
     */
    public void setRecordsManagementService(RecordsManagementService recordsManagementService)
    {
        this.recordsManagementService = recordsManagementService;
    }    
    
    /**
     * @param recordsManagementActionService the recordsManagementActionService to set
     */
    public void setRecordsManagementActionService(RecordsManagementActionService recordsManagementActionService)
    {
        this.recordsManagementActionService = recordsManagementActionService;
    }
    
    /**
     * Set policy components
     * 
     * @param policyComponent   policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * Set the reference
     * 
     * @param reference
     */
    public void setReferenceName(String reference)
    {
        this.reference = QName.createQName(reference);
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.event.SimpleRecordsManagementEventTypeImpl#init()
     */
    public void init()
    {
        super.init();
        
        // Register interest in the on create reference policy
        policyComponent.bindClassBehaviour(RecordsManagementPolicies.ON_CREATE_REFERENCE, 
                                           ASPECT_RECORD, 
                                           new JavaBehaviour(this, "onCreateReference", NotificationFrequency.TRANSACTION_COMMIT));
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.event.SimpleRecordsManagementEventTypeImpl#isAutomaticEvent()
     */
    @Override
    public boolean isAutomaticEvent()
    {
        return true;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementPolicies.OnCreateReference#onCreateReference(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void onCreateReference(NodeRef fromNodeRef, NodeRef toNodeRef, QName reference)
    {
        // Check whether it is the reference type we care about
        if (reference.equals(this.reference) == true)
        {
            DispositionAction da = recordsManagementService.getNextDispositionAction(fromNodeRef);
            if (da != null)
            {
                List<EventCompletionDetails> events = da.getEventCompletionDetails();
                for (EventCompletionDetails event : events)
                {
                    RecordsManagementEvent rmEvent = recordsManagementEventService.getEvent(event.getEventName());
                    if (event.isEventComplete() == false &&
                        rmEvent.getType().equals(getName()) == true)
                    {
                        // Complete the event
                        Map<String, Serializable> params = new HashMap<String, Serializable>(3);
                        params.put(CompleteEventAction.PARAM_EVENT_NAME, event.getEventName());
                        params.put(CompleteEventAction.PARAM_EVENT_COMPLETED_BY, AuthenticationUtil.getFullyAuthenticatedUser());
                        params.put(CompleteEventAction.PARAM_EVENT_COMPLETED_AT, new Date());
                        recordsManagementActionService.executeRecordsManagementAction(fromNodeRef, "completeEvent", params);
                        
                        break;
                    }
                }
            }
        }
    }
}