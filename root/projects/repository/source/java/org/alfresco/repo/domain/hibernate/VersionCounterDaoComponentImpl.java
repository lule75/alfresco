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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.domain.hibernate;

import org.alfresco.repo.domain.Store;
import org.alfresco.repo.domain.VersionCount;
import org.alfresco.repo.version.common.counter.VersionCounterService;
import org.alfresco.service.cmr.repository.InvalidStoreRefException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Version counter DAO service implemtation using Hibernate.
 * <p>
 * The object should execute within its own transaction, and is limited to single-thread
 * entry.  If it becomes a bottleneck, the transaction synchronization should be moved
 * over to reentrant locks and/or the hibernate mappings should be optimized for better
 * read-write access. 
 * 
 * @author Derek Hulley
 */
public class VersionCounterDaoComponentImpl
        extends HibernateDaoSupport
        implements VersionCounterService
{
    private static final String QUERY_GET_VERSION_COUNT_FOR_STORE = "versionCount.GetVersionCountForStore";
    private static final String QUERY_GET_STORE_BY_ALL = "store.GetStoreByAll";
    
    /**
     * Retrieves or creates a version counter.  This locks the counter against updates for the
     * current transaction.
     * 
     * @param storeKey      the primary key of the counter
     * @param create        <tt>true</tt> to create on demand
     * @return              Returns a current or new version counter
     */
    private VersionCount getVersionCounter(final StoreRef storeRef, boolean create)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(VersionCounterDaoComponentImpl.QUERY_GET_VERSION_COUNT_FOR_STORE)
                    .setString("protocol", storeRef.getProtocol())
                    .setString("identifier", storeRef.getIdentifier());
                return query.uniqueResult();
            }
        };
        VersionCount versionCount = (VersionCount) getHibernateTemplate().execute(callback);
        
        // Done if it exists
        if (versionCount != null)
        {
            // Debug
            if (logger.isDebugEnabled())
            {
                logger.debug("Got version counter: \n" +
                        "   Thread: " + Thread.currentThread().getName() + "\n" +
                        "   Version count: " + versionCount.getVersionCount());
            }
            // Done
            return versionCount;
        }
        else if (!create)
        {
            return null;
        }
        
        // We have permission to create
        callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(VersionCounterDaoComponentImpl.QUERY_GET_STORE_BY_ALL)
                    .setString("protocol", storeRef.getProtocol())
                    .setString("identifier", storeRef.getIdentifier());
                return query.uniqueResult();
            }
        };
        Store store = (Store) getHibernateTemplate().execute(callback);
        if (store == null)
        {
            throw new InvalidStoreRefException(storeRef);
        }
        versionCount = new VersionCountImpl();
        versionCount.setStore(store);
        getHibernateTemplate().save(versionCount);
        
        // Debug
        if (logger.isDebugEnabled())
        {
            logger.debug("Created version counter: \n" +
                    "   Thread: " + Thread.currentThread().getName() + "\n" +
                    "   Version count: " + versionCount.getVersionCount());
        }

        // Done
        return versionCount;
    }
    
    /**
     * Get the next available version number for the specified store.
     * 
     * @param storeRef  the version store id
     * @return          the next version number
     */
    public int nextVersionNumber(StoreRef storeRef)
    {
        // get the version counter
        VersionCount versionCount = getVersionCounter(storeRef, true);
        // get an incremented count
        int nextCount = versionCount.incrementVersionCount();
        
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Incremented version count: \n" +
                    "   Thread: " + Thread.currentThread().getName() + "\n" +
                    "   New version count: " + versionCount.getVersionCount());
        }
        return nextCount;
    }
    
    /**
     * Gets the current version number for the specified store.
     * 
     * @param storeRef  the store reference
     * @return          the current version number, zero if no version yet allocated.
     */
    public int currentVersionNumber(StoreRef storeRef)
    {
        // get the version counter
        VersionCount versionCounter = getVersionCounter(storeRef, false);
        // get an incremented count
        return versionCounter == null ? 0 : versionCounter.getVersionCount();
    }
    
    /**
     * Resets the version number for a the specified store.
     * 
     * WARNING: calling this method will completely reset the current 
     * version count for the specified store and cannot be undone.  
     *
     * @param storeRef  the store reference
     */
    public synchronized void resetVersionNumber(StoreRef storeRef)
    {
        // get the version counter
        VersionCount versionCounter = getVersionCounter(storeRef, true);
        // get an incremented count
        versionCounter.resetVersionCount();
    }
    
    /**
     * Sets the version number for a specified store.
     * 
     * WARNING: calling this method will completely reset the current 
     * version count for the specified store and cannot be undone.  
     *
     * @param storeRef  the store reference
     * @param versionCount  the new version count
     */
    public synchronized void setVersionNumber(StoreRef storeRef, int versionCount)
    {
        // get the version counter
        VersionCount versionCounter = getVersionCounter(storeRef, true);
        // get an incremented count
        versionCounter.setVersionCount(versionCount);
    }
}
