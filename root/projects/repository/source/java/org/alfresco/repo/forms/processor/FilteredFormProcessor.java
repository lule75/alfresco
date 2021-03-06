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

package org.alfresco.repo.forms.processor;

import java.util.List;
import java.util.Map;

import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.Item;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class for all FormProcessor implementations that wish to use
 * the filter mechanism.
 * 
 * @author Gavin Cornwell
 */
public abstract class FilteredFormProcessor<ItemType, PersistType> extends AbstractFormProcessor
{
    private static final Log logger = LogFactory.getLog(FilteredFormProcessor.class);

    protected FilterRegistry<ItemType, PersistType> filterRegistry;

    /**
     * Sets the filter registry
     * 
     * @param filterRegistry The FilterRegistry instance
     */
    public void setFilterRegistry(FilterRegistry<ItemType, PersistType> filterRegistry)
    {
        this.filterRegistry = filterRegistry;

        if (logger.isDebugEnabled())
            logger.debug("Set filter registry: " + this.filterRegistry + " for processor: " + this);
    }

    /*
     * @see
     * org.alfresco.repo.forms.processor.FormProcessor#generate(org.alfresco
     * .repo.forms.Item, java.util.List, java.util.List, java.util.Map)
     */
    public Form generate(Item item, List<String> fields, List<String> forcedFields, Map<String, Object> context)
    {
        // get the typed object representing the item
        ItemType typedItem = getTypedItem(item);

        // create an empty Form
        Form form = new Form(item);

        // inform all regsitered filters the form is about to be generated
        if (this.filterRegistry != null)
        {
            for (Filter<ItemType, PersistType> filter : this.filterRegistry.getFilters())
            {
                filter.beforeGenerate(typedItem, fields, forcedFields, form, context);
            }
        }

        // perform the actual generation of the form
        internalGenerate(typedItem, fields, forcedFields, form, context);

        // inform all regsitered filters the form has been generated
        if (this.filterRegistry != null)
        {
            for (Filter<ItemType, PersistType> filter : this.filterRegistry.getFilters())
            {
                filter.afterGenerate(typedItem, fields, forcedFields, form, context);
            }
        }

        return form;
    }

    /**
     * Persists the given form data for the given item, completed by calling
     * each applicable registered handler
     * 
     * @see org.alfresco.repo.forms.processor.FormProcessor#persist(org.alfresco.repo.forms.Item,
     *      org.alfresco.repo.forms.FormData)
     * @param item The item to save the form for
     * @param data The object representing the form data
     * @return The object persisted
     */
    public Object persist(Item item, FormData data)
    {
        // get the typed object representing the item
        ItemType typedItem = getTypedItem(item);

        // inform all regsitered filters the form is about to be persisted
        if (this.filterRegistry != null)
        {
            for (Filter<ItemType, PersistType> filter : this.filterRegistry.getFilters())
            {
                filter.beforePersist(typedItem, data);
            }
        }

        // perform the actual persistence of the form
        PersistType persistedObject = internalPersist(typedItem, data);

        // inform all regsitered filters the form has been persisted
        if (this.filterRegistry != null)
        {
            for (Filter<ItemType, PersistType> filter : this.filterRegistry.getFilters())
            {
                filter.afterPersist(typedItem, data, persistedObject);
            }
        }

        return persistedObject;
    }

    /**
     * Returns a typed Object representing the given item.
     * <p>
     * Subclasses that represent a form type will return a typed object that is
     * then passed to each of it's handlers, the handlers can therefore safely
     * cast the Object to the type they expect.
     * 
     * @param item The item to get a typed object for
     * @return The typed object
     */
    protected abstract ItemType getTypedItem(Item item);

    /**
     * Generates the form.
     * 
     * @param item The object to generate a form for
     * @param fields Restricted list of fields to include
     * @param forcedFields List of fields to forcibly include
     * @param form The form object being generated
     * @param context Map representing optional context that can be used during
     *            retrieval of the form
     */
    protected abstract void internalGenerate(ItemType item, List<String> fields, List<String> forcedFields, Form form,
                Map<String, Object> context);

    /**
     * Persists the form data.
     * 
     * @param item The object to persist the form for
     * @param data The data to persist
     * @return The object that got created or modified
     */
    protected abstract PersistType internalPersist(ItemType item, FormData data);

}
