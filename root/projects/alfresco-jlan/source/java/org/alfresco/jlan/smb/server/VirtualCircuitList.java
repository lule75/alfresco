/*
 * Copyright (C) 2006-2010 Alfresco Software Limited.
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

package org.alfresco.jlan.smb.server;

import java.util.Enumeration;
import java.util.Hashtable;

import org.alfresco.jlan.server.SrvSession;



/**
 * Virtual Circuit List Class
 * 
 * <p>Contains a list of virtual circuits that belong to a session.
 *
 * @author gkspencer
 */
public class VirtualCircuitList {

  //  Default and maximum number of virtual circuits

  public static final int DefaultCircuits = 4;
  public static final int MaxCircuits     = 16;

  //  UIDs are 16bit values
  
  private static final int UIDMask        = 0x0000FFFF;

  // Active virtual circuits
  
  private Hashtable<Integer, VirtualCircuit> m_vcircuits;
  private int m_UID;
  
  /**
   * Default constructor
   */
  public VirtualCircuitList() {
    
  }
  
  /**
   * Add a new virtual circuit to this session. Return the allocated UID for the new
   * circuit.
   *
   * @param vcircuit VirtualCircuit
   * @return int   Allocated UID.
   */
  public int addCircuit( VirtualCircuit vcircuit) {

    //  Check if the circuit table has been allocated

    if (m_vcircuits == null)
      m_vcircuits = new Hashtable<Integer, VirtualCircuit>(DefaultCircuits);

    //  Allocate an id for the tree connection
    
    int uid = 0;
    
    synchronized ( m_vcircuits) {
    
      //  Check if the virtual circuit table is full
      
      if ( m_vcircuits.size() == MaxCircuits)
        return VirtualCircuit.InvalidUID;

      //  Find a free slot in the circuit table
  
      uid = (m_UID++ & UIDMask);
      Integer key = new Integer( uid);
      
      while (m_vcircuits.contains(key)) {

        //  Try another user id for the new virtual circuit
        
        uid = (m_UID++ & UIDMask);
        key = new Integer( uid);
      }

      //  Store the new virtual circuit
      
      vcircuit.setUID( uid);
      m_vcircuits.put(key, vcircuit);
    }
    
    //  Return the allocated UID
    
    return uid;
  }

  /**
   * Return the virtual circuit details for the specified UID.
   *
   * @param uid int
   * @return VirtualCircuit
   */
  public final VirtualCircuit findCircuit(int uid) {

    //  Check if the circuit table is valid

    if (m_vcircuits == null)
      return null;

    //  Get the required tree connection details

    return m_vcircuits.get(new Integer(uid));
  }

  /**
   * Return the virtual circuit details for the specified UID.
   *
   * @param uid Integer
   * @return VirtualCircuit
   */
  public final VirtualCircuit findCircuit(Integer uid) {

    //  Check if the circuit table is valid

    if (m_vcircuits == null)
      return null;

    //  Get the required tree connection details

    return (VirtualCircuit) m_vcircuits.get( uid);
  }

  /**
   * Enumerate the virtual circiuts
   * 
   * @return Enumeration<Integer>
   */
  public final Enumeration<Integer> enumerateUIDs() {
    return m_vcircuits.keys();
  }
  
  /**
   * Remove the specified virtual circuit from the active circuit list.
   *
   * @param uid int
   * @param sess SrvSession
   */
  public void removeCircuit(int uid, SrvSession sess) {

    //  Check if the circuit table is valid

    if (m_vcircuits == null)
      return;

    //  Close the circuit and remove from the circuit table
    
    synchronized ( m_vcircuits) {

      //  Get the circuit
      
      Integer key = new Integer(uid);
      VirtualCircuit vc = m_vcircuits.get(key);
      
      //  Close the virtual circuit, release resources

      if ( vc != null) {
        
        //  Close the circuit
        
        vc.closeCircuit( sess);
        
        //  Remove the circuit from the circuit table
    
        m_vcircuits.remove(key);
      }
    }
  }
  
  /**
   * Return the active tree connection count
   * 
   * @return int
   */
  public final int getCircuitCount() {
    return m_vcircuits != null ? m_vcircuits.size() : 0;
  }
  
  /**
   * Clear the virtual circuit list
   */
  public final void clearCircuitList() {
    m_vcircuits.clear();
  }
  
  /**
   * Return the virtual circuit list details as a string
   * 
   * @return String
   */
  public String toString() {
    StringBuffer str = new StringBuffer();
    
    str.append("[VCs=");
    str.append( getCircuitCount());
    str.append( "]");
    
    return str.toString();
  }
}
