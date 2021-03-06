/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.admin.patch;

import java.util.Date;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.service.transaction.TransactionService;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;

/**
 * This component is responsible for ensuring that patches are applied
 * at the appropriate time.
 * 
 * @author Derek Hulley
 */
public class PatchExecuter extends AbstractLifecycleBean
{
    private static final String MSG_CHECKING = "patch.executer.checking";
    private static final String MSG_NO_PATCHES_REQUIRED = "patch.executer.no_patches_required";
    private static final String MSG_SYSTEM_READ_ONLY = "patch.executer.system_readonly";
    private static final String MSG_NOT_EXECUTED = "patch.executer.not_executed";
    private static final String MSG_EXECUTED = "patch.executer.executed";
    private static final String MSG_FAILED = "patch.executer.failed";
    
    private static Log logger = LogFactory.getLog(PatchExecuter.class);
    
    private PatchService patchService;
    private TransactionService transactionService;

    /**
     * @param patchService the server that actually executes the patches
     */
    public void setPatchService(PatchService patchService)
    {
        this.patchService = patchService;
    }

    /**
     * @param transactionService provides the system read-only state
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * Ensures that all outstanding patches are applied.
     */
    public void applyOutstandingPatches()
    {
        // Avoid read-only systems
        if (!patchService.validatePatches() || transactionService.isReadOnly())
        {
            logger.warn(I18NUtil.getMessage(MSG_SYSTEM_READ_ONLY));
            return;
        }
        
        logger.info(I18NUtil.getMessage(MSG_CHECKING));
        
        Date before = new Date(System.currentTimeMillis() - 60000L);  // 60 seconds ago
        patchService.applyOutstandingPatches();
        Date after = new Date(System .currentTimeMillis() + 20000L);  // 20 seconds ahead
        
        // get all the patches executed in the time
        List<AppliedPatch> appliedPatches = patchService.getPatches(before, after);
        
        // don't report anything if nothing was done
        if (appliedPatches.size() == 0)
        {
            logger.info(I18NUtil.getMessage(MSG_NO_PATCHES_REQUIRED));
        }
        else
        {
            boolean succeeded = true;
            // list all patches applied, including failures
            for (AppliedPatch patchInfo : appliedPatches)
            {
                if (!patchInfo.getWasExecuted())
                {
                    // the patch was not executed
                    logger.debug(I18NUtil.getMessage(MSG_NOT_EXECUTED, patchInfo.getId(), patchInfo.getReport()));
                }
                else if (patchInfo.getSucceeded())
                {
                    logger.info(I18NUtil.getMessage(MSG_EXECUTED, patchInfo.getId(), patchInfo.getReport()));
                }
                else
                {
                    succeeded = false;
                    logger.error(I18NUtil.getMessage(MSG_FAILED, patchInfo.getId(), patchInfo.getReport()));
               }
            }
            // generate an error if there was a failure
            if (!succeeded)
            {
                throw new AlfrescoRuntimeException("Not all patches could be applied");
            }
        }
    }

    @Override
    protected void onBootstrap(ApplicationEvent event)
    {
        applyOutstandingPatches();
    }

    @Override
    protected void onShutdown(ApplicationEvent event)
    {
        // NOOP
    }

}
