/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
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

package org.alfresco.repo.transfer;

import java.io.File;
import java.util.Set;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.transfer.TransferException;
import org.alfresco.service.cmr.transfer.TransferProgress;
import org.alfresco.service.cmr.transfer.TransferTarget;

/**
 * @author brian
 *
 */
public interface TransferTransmitter
{
    /**
     * Verify that the target is available
     * @param target
     * @throws TransferException
     */
    void verifyTarget(TransferTarget target) throws TransferException;
    
    /**
     * Begin a transfer, the transfer object returned will be used by subsequent 
     * calls to the transfer service.
     * 
     * @param target definition of where to transfer to.
     * @return the transfer object or null if the target cannot be locked.
     * @throws TransferException
     */
    Transfer begin(TransferTarget target) throws TransferException;
    
    /**
     * @param manifest, the transfer manifest file
     * @param transfer the transfer object returned by an earlier call to begin
     * @return the delta list.
     * @throws TransferException
     */
    DeltaList sendManifest(Transfer transfer, File manifest) throws TransferException;
    
    /**
     * Send the content of the specified urls
     * 
     * @param transfer the transfer object returned by an earlier call to begin
     * @param data the content to send
     * @throws TransferException
     */
    void sendContent(Transfer transfer, Set<ContentData> data);
    
    /**
     *
     * @param transfer the transfer object returned by an earlier call to begin
     * @throws TransferException
     */
    void prepare(Transfer transfer) throws TransferException;
    
    /**
     * @param transfer the transfer object returned by an earlier call to begin
     * @throws TransferException 
     */
    void commit(Transfer transfer) throws TransferException;
    
    /**
     * Abort the transfer
     * @param transfer the transfer object returned by an earlier call to begin 
     * @throws TransferException 
     */
    void abort(Transfer transfer) throws TransferException;
    
    /**
     * Get the status of an in process transfer
     */
    TransferProgress getStatus(Transfer transfer) throws TransferException;
    
}