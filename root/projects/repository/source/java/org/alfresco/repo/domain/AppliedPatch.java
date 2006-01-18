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
package org.alfresco.repo.domain;

import java.util.Date;

/**
 * Interface for persistent patch application information.
 * 
 * @author Derek Hulley
 */
public interface AppliedPatch
{
    public String getId();
    public void setId(String id);

    public String getDescription();
    public void setDescription(String description);
    
    public String getApplyToVersion();
    public void setApplyToVersion(String version);
    
    public boolean getSucceeded();
    public void setSucceeded(boolean succeeded);
    
    public String getAppliedOnVersion();
    public void setAppliedOnVersion(String version);
    
    public Date getAppliedOnDate();
    public void setAppliedOnDate(Date date);
    
    public String getReport();
    public void setReport(String report);
}
