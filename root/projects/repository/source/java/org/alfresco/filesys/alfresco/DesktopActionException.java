/*
 * Copyright (C) 2005 Alfresco, Inc.
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
 */

package org.alfresco.filesys.alfresco;

/**
 * Desktop Action Exception Class
 *
 * @author gkspencer
 */
public class DesktopActionException extends Exception {

	private static final long serialVersionUID = 1006648817889605047L;
 
	// Status code
	
	private int m_stsCode;
	
	/**
	 * Class constructor
	 * 
	 * @param sts int
	 * @param msg String
	 */
	public DesktopActionException(int sts, String msg)
	{
		super(msg);
		m_stsCode = sts;
	}
	
	/**
     * Class constructor
     * 
     * @param s String
     */
    public DesktopActionException(String s)
    {
        super(s);
    }

    /**
     * Class constructor
     * 
     * @param s String
     * @param ex Exception
     */
    public DesktopActionException(String s, Throwable ex)
    {
        super(s, ex);
    }
    
    /**
     * Return the status code
     * 
     * @return int
     */
    public final int getStatusCode()
    {
    	return m_stsCode;
    }
}
