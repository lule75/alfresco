/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.web.scripts.rule;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.web.scripts.DeclarativeWebScript;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.WebScriptException;
import org.alfresco.web.scripts.WebScriptRequest;

/**
 * Web Script to DELETE the rule identified by the given rule node id.
 * 
 * @author glen johnson at alfresco dot com
 */
public class RuleDelete extends DeclarativeWebScript
{
    // private constants 
    private static final String REQ_TEMPL_VAR_STORE_TYPE = "store_type";
    private static final String REQ_TEMPL_VAR_STORE_ID = "store_id";
    private static final String REQ_TEMPL_VAR_RULE_NODE_ID = "rule_id";
    private static final String REQ_TEMPL_VAR_RULE_OWNING_NODE_ID = "id";
    
    // properties for services
    private RuleService ruleService;
    
    // dependencies
    private RulesHelper rulesHelper;

    /**
     * Set the ruleService property.
     * 
     * @param ruleService The rule service instance to set
     */
    public void setRuleService(RuleService ruleService)
    {
        this.ruleService = ruleService;
    }
    
    /**
     * Set the rules helper property
     * 
     * @param rulesHelper the rulesHelper to set
     */
    public void setRulesHelper(RulesHelper rulesHelper)
    {
        this.rulesHelper = rulesHelper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.web.scripts.DeclarativeWebScript#executeImpl(
     *      org.alfresco.web.scripts.WebScriptRequest,
     *      org.alfresco.web.scripts.WebScriptResponse)
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status)
    {
        String storeType = req.getServiceMatch().getTemplateVars().get(REQ_TEMPL_VAR_STORE_TYPE);
        // Handle if 'store_type' URL template token not provided
        if ((storeType == null) || (storeType.length() == 0))
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "The 'store_type' URL template token has not been provided in URL");
        }                
        
        String storeId = req.getServiceMatch().getTemplateVars().get(REQ_TEMPL_VAR_STORE_ID);
        // Handle if 'storeId' URL template token not provided
        if ((storeId == null) || (storeId.length() == 0))
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "The 'storeId' URL template token has not been provided in URL");
        }                        
        
        String ruleNodeId = req.getServiceMatch().getTemplateVars().get(REQ_TEMPL_VAR_RULE_NODE_ID);
        // Handle if 'ruleNodeId' URL template token not provided
        if ((ruleNodeId == null) || (ruleNodeId.length() == 0))
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "The 'rule_id' URL template token has not been provided in URL");
        }                        
        
        String owningNodeId = req.getServiceMatch().getTemplateVars().get(REQ_TEMPL_VAR_RULE_OWNING_NODE_ID);
        // Handle if 'owningNodeId' URL template token not provided
        if ((owningNodeId == null) || (owningNodeId.length() == 0))
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "The 'id' URL template token has not been provided in URL");
        }                        
        
        // create the rule's identifying node reference from the given 
        // URL template tokens
        NodeRef ruleNodeRef = this.rulesHelper.getNodeRefFromWebScriptUrl(req, storeType, storeId, ruleNodeId);
        
        // create the rule owning node reference from the given 
        // URL template tokens
        NodeRef ruleOwningNodeRef = this.rulesHelper.getNodeRefFromWebScriptUrl(req, storeType, storeId, owningNodeId);
        
        // get the rule using it's unique identifying node reference
        Rule rule = this.ruleService.getRule(ruleNodeRef);
        
        // delete rule
        this.ruleService.removeRule(ruleOwningNodeRef, rule);
        
        return null;
    }        
}
