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
package org.alfresco.repo.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache backed by a simple <code>HashMap</code>.
 * <p>
 * <b>Note:</b> This cache is not transaction- or thread-safe.  Use it for single-threaded tests only.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public class MemoryCache<K extends Serializable, V extends Object> implements SimpleCache<K, V>
{
    private Map<K, V> map;
    
    public MemoryCache()
    {
        map = new HashMap<K, V>(15);
    }

    public boolean contains(K key)
    {
        return map.containsKey(key);
    }

    public Collection<K> getKeys()
    {
        return map.keySet();
    }

    public V get(K key)
    {
        return map.get(key);
    }

    public void put(K key, V value)
    {
        map.put(key, value);
    }

    public void remove(K key)
    {
        map.remove(key);
    }

    public void clear()
    {
        map.clear();
    }
}