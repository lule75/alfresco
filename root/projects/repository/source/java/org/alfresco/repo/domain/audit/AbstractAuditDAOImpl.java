/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.repo.domain.audit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.CRC32;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.domain.contentdata.ContentDataDAO;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract helper DAO for <b>alf_audit_XXX</b> tables.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public abstract class AbstractAuditDAOImpl implements AuditDAO 
{
    private static final Log logger = LogFactory.getLog(AbstractAuditDAOImpl.class);
    
    private ContentService contentService;
    private ContentDataDAO contentDataDAO;
    
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setContentDataDAO(ContentDataDAO contentDataDAO)
    {
        this.contentDataDAO = contentDataDAO;
    }

    /**
     * {@inheritDoc}
     */
    public Pair<Long, ContentData> getOrCreateAuditConfig(URL url)
    {
        InputStream is = null;
        try
        {
            is = url.openStream();
            // Calculate the CRC and find an entry that matches
            CRC32 crcCalc = new CRC32();
            byte[] buffer = new byte[1024];
            int read = -1;
            do
            {
                read = is.read(buffer);
                if (read < 0)
                {
                    break;
                }
                crcCalc.update(buffer, 0, read);
            }
            while (true);
            long crc = crcCalc.getValue();
            // Find an existing entry
            AuditConfigEntity existingEntity = getAuditConfigByCrc(crc);
            if (existingEntity != null)
            {
                Long existingEntityId = existingEntity.getId();
                // Locate the content
                ContentData existingContentData = contentDataDAO.getContentData(
                        existingEntity.getContentDataId()
                        ).getSecond();
                Pair<Long, ContentData> result = new Pair<Long, ContentData>(existingEntityId, existingContentData);
                // Done
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Found existing configuration with same CRC: \n" +
                            "   URL:    " + url + "\n" +
                            "   CRC:    " + crc + "\n" +
                            "   Result: " + result);
                }
                return result;
            }
            else
            {
                // Upload the content afresh
                is.close();
                is = url.openStream();
                ContentWriter writer = contentService.getWriter(null, null, false);
                writer.setEncoding("UTF-8");
                writer.setMimetype(MimetypeMap.MIMETYPE_XML);
                writer.putContent(is);
                ContentData newContentData = writer.getContentData();
                Long newContentDataId = contentDataDAO.createContentData(newContentData).getFirst();
                AuditConfigEntity newEntity = createAuditConfig(newContentDataId, crc);
                Pair<Long, ContentData> result = new Pair<Long, ContentData>(newEntity.getId(), newContentData);
                // Done
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Created new audit config: \n" +
                            "   URL:    " + url + "\n" +
                            "   CRC:    " + crc + "\n" +
                            "   Result: " + result);
                }
                return result;
            }
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("Failed to read Audit configuration: " + url);
        }
        finally
        {
            if (is != null)
            {
                try { is.close(); } catch (Throwable e) {}
            }
        }
    }
    
    protected abstract AuditConfigEntity getAuditConfigByCrc(long crc);
    protected abstract AuditConfigEntity createAuditConfig(Long contentDataId, long crc);
}