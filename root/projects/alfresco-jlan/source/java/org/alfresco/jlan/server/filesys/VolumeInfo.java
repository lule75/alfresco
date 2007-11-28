package org.alfresco.jlan.server.filesys;

/*
 * VolumeInfo.java
 *
 * Copyright (c) 2004 Starlasoft. All rights reserved.
 */
 
import java.util.Date;

/**
 * Disk Volume Information Class
 */
public class VolumeInfo {

	//	Volume label
	
	private String m_label;
	
	//	Serial number
	
	private int m_serno = -1;
	
	//	Creation date/time
	
	private Date m_created;
	
	/**
	 * Default constructor
	 */
	public VolumeInfo() {
	}
	
	/**
	 * Class constructor
	 * 
	 * @param label String
	 */
	public VolumeInfo(String label) {
		setVolumeLabel(label);
	}
	
	/**
	 * Class constructor
	 * 
	 * @param label String
	 * @param serno int
	 * @param created Date
	 */
	public VolumeInfo(String label, int serno, Date created) {
		setVolumeLabel(label);
		setSerialNumber(serno);
		setCreationDateTime(created);
	}
	
	/**
	 * Return the volume label
	 * 
	 * @return String
	 */
	public final String getVolumeLabel() {
		return m_label;
	}
	
	/**
	 * Determine if the serial number is valid
	 * 
	 * @return boolean
	 */
	public final boolean hasSerialNumber() {
		return m_serno != -1 ? true : false;
	}
	
	/**
	 * Return the serial number
	 * 
	 * @return int
	 */
	public final int getSerialNumber() {
		return m_serno; 
	}

	/**
	 * Determine if the creation date/time is valid
	 * 
	 * @return boolean
	 */
	public final boolean hasCreationDateTime() {
		return m_created != null ? true : false;
	}
		
	/**
	 * Return the volume creation date/time
	 * 
	 * @return Date
	 */
	public final Date getCreationDateTime() {
		return m_created;
	}
	
	/**
	 * Set the volume label
	 * 
	 * @param label
	 */
	public final void setVolumeLabel(String label) {
		m_label = label;
	}
	
	/**
	 * Set the serial number
	 * 
	 * @param serno int
	 */
	public final void setSerialNumber(int serno) {
		m_serno = serno;
	}
	
	/**
	 * Set the volume creation date/time
	 * 
	 * @param created Date
	 */
	public final void setCreationDateTime(Date created) {
		m_created = created;
	}
	
	/**
	 * Return the volume information as a string
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		
		str.append("[");
		str.append(getVolumeLabel());
		str.append(",");
		str.append(getSerialNumber());
		str.append(",");
		str.append(getCreationDateTime());
		str.append("]");
		
		return str.toString();
	}
}
