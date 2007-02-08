/*-----------------------------------------------------------------------------
*  Copyright 2006 Alfresco Inc.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    CacheControlFilterInfoBean.java
*----------------------------------------------------------------------------*/
package org.alfresco.filter;

import java.util.*;

public class CacheControlFilterInfoBean
{
    // Spring creates a LinkedHashMap, so rules order is preserved
    private Map<String,String> cacheControlRules_; 

    public void setCacheControlRules( Map<String,String> cacheControlRules )
    {
        cacheControlRules_ = cacheControlRules;
    }

    public Set<Map.Entry<String,String>> getCacheControlRulesEntrySet() 
    { 
        return ( Set<Map.Entry<String,String>>) cacheControlRules_.entrySet();
    }
}


