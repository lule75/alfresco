package org.alfresco.jlan.server.filesys;

/*
 * NetworkFileServer.java
 *
 * Copyright (c) 2004 Starlasoft. All rights reserved.
 */
 
import java.util.Vector;

import org.alfresco.jlan.server.NetworkServer;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.config.ServerConfiguration;


/**
 * Network File Server Class
 * 
 * <p>Base class for all network file servers. 
 */
public abstract class NetworkFileServer extends NetworkServer {

	//	File listener list

	private Vector<FileListener> m_fileListeners;
	
  // filesystems configuration
  
  private FilesystemsConfigSection m_filesysConfig;
  
	/**
	 * Class constructor
	 * 
	 * @param proto String
	 * @param config ServerConfiguration
	 */
	public NetworkFileServer(String proto, ServerConfiguration config) {
		super(proto, config);
    
    //  Get the filesystems configuration
    
    m_filesysConfig = (FilesystemsConfigSection) config.getConfigSection( FilesystemsConfigSection.SectionName);
	}
	
  /**
   * Return the filesystems configuration
   * 
   * @return FilesystemConfigSection
   */
  public final FilesystemsConfigSection getFilesystemConfiguration() {
    return m_filesysConfig;
  }
  
	/**
	 * Add a file listener
	 *
	 * @param l FileListener implementation.
	 */
	public final void addFileListener(FileListener l) {

		//  Check if the file listener list is allocated

		if (m_fileListeners == null)
			m_fileListeners = new Vector<FileListener>();
		m_fileListeners.add(l);
	}

	/**
	 * Remove a file listener from the SMB server.
	 *
	 * @param l FileListener
	 */
	public final void removeFileListener(FileListener l) {

		//  Check if the listener list is valid

		if (m_fileListeners == null)
			return;
		m_fileListeners.remove(l);
	}

	/**
	 * Fire a file closed event to all registered file listeners.
	 *
	 * @param sess SrvSession
	 * @param file NetworkFile
	 */
	public final void fireCloseFileEvent(SrvSession sess, NetworkFile file) {

		//  Check if there are any listeners

		if (m_fileListeners == null || m_fileListeners.size() == 0)
			return;

		//  Inform all registered listeners

		for (int i = 0; i < m_fileListeners.size(); i++) {
			FileListener fileListener = m_fileListeners.elementAt(i);
			try {
				fileListener.fileClosed(sess, file);
			}
			catch (Exception ex) {
			}
		}
	}

	/**
	 * Trigger a file open event to all registered file listeners.
	 *
	 * @param sess SrvSession
	 * @param file NetworkFile
	 */
	public final void fireOpenFileEvent(SrvSession sess, NetworkFile file) {

		//  Check if there are any listeners

		if (m_fileListeners == null || m_fileListeners.size() == 0)
			return;

		//  Inform all registered listeners

		for (int i = 0; i < m_fileListeners.size(); i++) {
			FileListener fileListener = m_fileListeners.elementAt(i);
			try {
				fileListener.fileOpened(sess, file);
			}
			catch (Exception ex) {
			}
		}
	}
}
