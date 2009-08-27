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
package org.alfresco.repo.domain.propval;

import java.io.Serializable;

import org.alfresco.repo.domain.propval.PropertyValueEntity.PersistedType;

/**
 * Interface for converters that to translate between persisted values and external values.
 * <p/>
 * Implementations must be able to convert between values being stored and Long, Double, String -
 * and back again.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public interface PropertyTypeConverter
{
    /**
     * When external to persisted type mappings are not obvious, the persistence framework,
     * before persisting as {@link PersistedType#SERIALIZABLE}, will give the converter
     * a chance to choose how the value must be persisted:
     * <ul>
     *   <li>{@link PersistedType#LONG}</li>
     *   <li>{@link PersistedType#DOUBLE}</li>
     *   <li>{@link PersistedType#STRING}</li>
     *   <li>{@link PersistedType#SERIALIZABLE}</li>
     * </ul>
     * The converter should return {@link PersistedType#SERIALIZABLE} if no further conversions
     * are possible.  Implicit in the return value is the converter's ability to do the
     * conversion when required.
     * 
     * @param value             the value that does not have an obvious persistence slot
     * @return                  Returns the type of persistence to use
     */
    PersistedType getPersistentType(Serializable value);

    /**
     * Convert a value to a given type.
     * 
     * @param value             the value to convert
     * @return                  Returns the persisted type and value to persist
     */
    <T> T convert(Class<T> targetClass, Object value);
}
