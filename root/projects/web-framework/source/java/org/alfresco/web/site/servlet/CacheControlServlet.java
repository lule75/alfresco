/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.web.site.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.site.CacheUtil;
import org.alfresco.web.site.RequestContext;
import org.alfresco.web.site.RequestUtil;

/**
 * A servlet that can be used to invalidate the cache
 * 
 * @author muzquiano
 */
public class CacheControlServlet extends BaseServlet
{
    public void init() throws ServletException
    {
        super.init();
    }

    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        // get the request context
        RequestContext context = RequestUtil.getRequestContext(request);

        // the command
        String command = (String) request.getParameter("command");
        if ("invalidateAll".equalsIgnoreCase(command))
        {
            // invalidate the file system cache, if it exists
            CacheUtil.invalidateFileSystemCache(context);

            // invalidate the ADS object cache
            CacheUtil.invalidateADSObjectCache(context);
        }
        if ("invalidate".equalsIgnoreCase(command))
        {
            // invalidate a single object
            // TODO
        }
    }
}
