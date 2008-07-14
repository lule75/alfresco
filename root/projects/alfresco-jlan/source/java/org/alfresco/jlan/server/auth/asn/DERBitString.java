/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
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

package org.alfresco.jlan.server.auth.asn;

import java.io.IOException;

/**
 * DER Bit String Class 
 *
 * @author gkspencer
 */
public class DERBitString extends DERObject {

  // Bit flags value
  
  private long m_bits;
  
  /**
   * Default constructor
   */
  public DERBitString() {
  }

  /**
   * Class constructor
   * 
   * @param bits int
   */
  public DERBitString( long bits) {
    m_bits = bits;
  }

  /**
   * Return the value
   * 
   * @return long
   */
  public final long getValue() {
    return m_bits;
  }

  /**
   * Return the value as an integer
   * 
   * @return int
   */
  public final int intValue() {
	  return (int) m_bits;
  }
  
  /**
   * Decode the object
   * 
   * @param buf
   * @throws IOException
   */
  public void derDecode(DERBuffer buf)
    throws IOException {

    // Decode the type
    
    if ( buf.unpackType() == DER.BitString) {
      
      // Unpack the length and bytes
      
      int len = buf.unpackLength();
      int lastBits = buf.unpackByte();
      
      m_bits = 0;
      long curByt = 0L;
      len --;
      
      for ( int idx = (len - 1); idx >= 0; idx--) {
        
        // Get the value bytes

    	curByt = (long) buf.unpackByte();
        m_bits += curByt << (idx * 8);
      }
    }
    else
      throw new IOException("Wrong DER type, expected BitString");
  }

  /**
   * Encode the object
   * 
   * @param buf
   * @throws IOException
   */
  public void derEncode(DERBuffer buf)
    throws IOException {

    // Pack the type, length and bytes
    
    buf.packByte( DER.BitString);
    buf.packByte( 0);

    buf.packLength( 8);
    for ( int idx = 7; idx >= 0; idx--) {
    	long bytVal = m_bits >> ( idx * 8); 
    	buf.packByte((int) ( m_bits & 0xFF));
    }
  }
  
  /**
   * Return the bit string as a string
   * 
   * @return String
   */
  public String toString() {
    StringBuffer str = new StringBuffer();
    
    str.append("[BitString:0x");
    str.append( Long.toHexString( m_bits));
    str.append("]");
    
    return str.toString();
  }
}
