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
package org.alfresco.repo.version;

import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.Tenant;
import org.alfresco.repo.tenant.TenantAdminService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Cleanup of Version Store Migration - to delete old/migrated version histories from old version store. Typically this is configured to run once on startup.
 */
public class MigrationCleanupJob implements Job
{
    private static Log logger = LogFactory.getLog(MigrationCleanupJob.class);
    
    private static final String KEY_VERSION_MIGRATOR = "versionMigrator";
    private static final String KEY_TENANT_ADMIN_SERVICE = "tenantAdminService";
    
    private static final String KEY_BATCHSIZE = "batchSize";
    
    private int batchSize = 1;
    
    public void execute(JobExecutionContext context) throws JobExecutionException
    { 
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        
        final VersionMigrator migrationCleanup = (VersionMigrator)jobData.get(KEY_VERSION_MIGRATOR);
        final TenantAdminService tenantAdminService = (TenantAdminService)jobData.get(KEY_TENANT_ADMIN_SERVICE);
        
        if (migrationCleanup == null)
        {
            throw new JobExecutionException("Missing job data: " + KEY_VERSION_MIGRATOR);
        }
        
        String batchSizeStr = (String)jobData.get(KEY_BATCHSIZE);
        if (batchSizeStr != null)
        {
            try
            {
                batchSize = new Integer(batchSizeStr);
            }
            catch (Exception e)
            {
                logger.warn("Invalid batchsize, using default: " + batchSize, e);
            }
        }
        
        if (batchSize < 1)
        {
            String errorMessage = "batchSize ("+batchSize+") cannot be less than 1";
            logger.error(errorMessage);
            throw new AlfrescoRuntimeException(errorMessage);
        }
        
        // perform the cleanup of the old version store
        migrationCleanup.executeCleanup(batchSize);
        
    	if ((tenantAdminService != null) && tenantAdminService.isEnabled())
        {
        	List<Tenant> tenants = tenantAdminService.getAllTenants();	                            	
            for (Tenant tenant : tenants)
            {          
            	String tenantDomain = tenant.getTenantDomain();
            	AuthenticationUtil.runAs(new RunAsWork<Object>()
                {
            		public Object doWork() throws Exception
                    {
            			migrationCleanup.executeCleanup(batchSize);
            			return null;
                    }
                }, tenantAdminService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantDomain));
            }
        }
    }
}
