/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
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

package org.alfresco.deployment.impl.asr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.alfresco.deployment.FSDeploymentRunnable;
import org.alfresco.deployment.FileDescriptor;
import org.alfresco.deployment.FileType;
import org.alfresco.deployment.impl.DeploymentException;
import org.alfresco.deployment.impl.server.DeployedFile;
import org.alfresco.deployment.impl.server.Deployment;
import org.alfresco.deployment.impl.server.DeploymentReceiverAuthenticator;
import org.alfresco.deployment.impl.server.DeploymentState;
import org.alfresco.deployment.DeploymentTarget;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.avm.AVMNodeType;
import org.alfresco.repo.avm.util.SimplePath;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.springframework.extensions.surf.util.PropertyCheck;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This implements a deployment target for deployment to an AVM Store  
 */
public class AVMDeploymentTarget implements Serializable, DeploymentTarget
{   
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1257869549338878302L;
   
    /**
     * Deployments in progress
     */
    private Map<String, Deployment> fDeployments = Collections.synchronizedMap(new HashMap<String, Deployment>());
    
    /**
     * The logger for this target
     */
    private static Log logger = LogFactory.getLog(AVMDeploymentTarget.class);

    /**
     * The name of this target.
     */
    private String fTargetName;
    
    /**
     * The pattern for stores generated by this 
     */
    private String storeNamePattern = "%storeName%-live";
    
    /**
     * The pattern for the root of the destination
     */
    private String rootPath = "/www/avm_webapps";
    
    /**
     * Who should this target run as ?
     */
    private String proxyUser = AuthenticationUtil.SYSTEM_USER_NAME;
    
    /**
     * The authenticator for this target
     */
    private DeploymentReceiverAuthenticator authenticator;
       
    /**
     * Runnables that will be invoked after commit.
     */
    private List<FSDeploymentRunnable> postCommit;
    
    /**
     * Runnables that will be invoked during prepare phase.
     */
    private List<FSDeploymentRunnable> prepare;
    
    public void setAuthenticator(DeploymentReceiverAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	public DeploymentReceiverAuthenticator getAuthenticator() {
		return authenticator;
	}
	
    /**
     * The local AVMService Instance.
     */
    private AVMService fAVMService;
    
    /**
     * Setter.
     * @param service The instance to set.
     */
    public void setAvmService(AVMService service)
    {
        fAVMService = service;
    }
    
    private TransactionService trxService;
    
    /**
     * initialise this target
     */
    public void init() 
    {
        PropertyCheck.mandatory(this, "authenticator", authenticator);
        PropertyCheck.mandatory(this, "avmService", fAVMService); 
        PropertyCheck.mandatory(this, "trxService", trxService); 
    }
    
    /**
     * Get the target name.
     * @return
     */
    public String getName()
    {
        return fTargetName;
    }
    
    /**
     * Implementation of begin for ASR
     */
	public synchronized String begin(String targetName, String storeName, int version, String user, char[] password) 
	{ 
		// Authenticate with the user and password
		if(!authenticator.logon(user, password))
		{
			logger.warn("Invalid user name or password");
			throw new DeploymentException("Invalid user name or password.");
		}
    
        String ticket = GUID.generate();
        logger.debug("begin deploy, target:" + targetName + ", ticket:" + ticket);
           
        String localStoreName = getLocalStoreName(storeName);
        
        AVMStoreDescriptor storeDesc = fAVMService.getStore(localStoreName);

        /**
         * Create the local store if it does not already exist
         */
        if (storeDesc == null)
        {
            logger.debug("Store does not exist, create new store," + localStoreName);
        	fAVMService.createStore(localStoreName);
            logger.debug("Store created," + localStoreName);
            
            /**
             *  Create the "synthetic" directories if required
             */
            SimplePath simpPath = new SimplePath(getRootPath());
            if (simpPath.size() > 0)
            {
            	String prevPath = localStoreName + ":/";
            	for (int i = 0; i < simpPath.size(); i++)
            	{
            		String currPath = AVMNodeConverter.ExtendAVMPath(prevPath, simpPath.get(i));
            		AVMNodeDescriptor desc = fAVMService.lookup(-1, currPath);
            		if (desc == null)
            		{
            			fAVMService.createDirectory(prevPath, simpPath.get(i));
            		}
            		prevPath = currPath;
            	}
            }
        }
     
        try
        {
            Deployment deployment = new Deployment(ticket, targetName, storeName, version);
            fDeployments.put(ticket, deployment);
        }
        catch (IOException e)
        {
        	logger.error("Could not create logfile", e);
            throw new DeploymentException("Could not create logfile; Deployment cannot continue", e);
        }
        return ticket;
	}
	
	
	public void prepare(String ticket) 
	{
	    final String fTicket = ticket;
	    AuthenticationUtil.runAs(	            
	            new AuthenticationUtil.RunAsWork<Void>()
	            {
                    public Void doWork() throws Exception
                    {
                        prepareImpl(fTicket);
                        return null;
                    }
	            }
	            , proxyUser);
	}
	    
    private void prepareImpl(final String ticket)
	{
	  	logger.info("Prepare ticket: " + ticket);
        Deployment deployment = fDeployments.get(ticket);
        if (deployment == null)
        {
        	logger.debug("Could not prepare: invalid token ticket:" + ticket);
        	// We are most likely to get here because we are aborting an already aborted ticket
        	return;
        }
        if (deployment.getState() != DeploymentState.WORKING)
        {
            throw new DeploymentException("Deployment cannot be prepared: already aborting, or committing.");
        }
        try
        {
            /**
             * Check that the temporary files are still there
             */
            for(DeployedFile file : deployment)
            {
            	if(file.getType() == FileType.FILE)
            	{
            		File content = new File(file.getPreLocation());            		
            		if(!content.exists())
            		{
            			throw new DeploymentException("Unable to prepare, missing temporary file." + content.getAbsolutePath());
            		}
            	}
            }
            
            /**
             *  Run any end user callbacks
             */
            if (prepare != null && prepare.size() > 0)
            {
                for (FSDeploymentRunnable runnable : prepare)
                {
                    try
                    {
                        runnable.init(deployment);
                        runnable.run();
                    }
                    catch (Throwable t)
                    {
                        String msg = "Error thrown in prepare; rolled back";
                        
                        if(t.getCause() != null)
                        {
                           msg = msg + " :" + t.getCause().getMessage(); 
                        }
                        logger.error(msg, t);
               
                        throw new DeploymentException(msg, t);
                    }
                }
            }
        	
            // Mark the deployment as prepared
            deployment.prepare();
            
            logger.debug("prepared successfully ticket:" + ticket);
        }
        catch (IOException e)
        {
        	logger.error("Error while preparing ticket:" + ticket, e);
            throw new DeploymentException("Could not prepare.", e);
        }
	}
    
    public void abort(String ticket) 
    {
        final String fTicket = ticket;
        AuthenticationUtil.runAs(               
                new AuthenticationUtil.RunAsWork<Void>()
                {
                    public Void doWork() throws Exception
                    {
                        abortImpl(fTicket);
                        return null;
                    }
                }
                , proxyUser);
    }


	private void abortImpl(String ticket) 
	{
	  	logger.info("Abort ticket: " + ticket);
        AuthenticationUtil.setRunAsUser(proxyUser);
        
        Deployment deployment = fDeployments.get(ticket);
        if (deployment == null)
        {
        	logger.debug("Could not abort: invalid token ticket:" + ticket);
        	// We are most likely to get here because we are aborting an already aborted ticket
        	return;
        }
        if (deployment.getState() != DeploymentState.WORKING && deployment.getState() != DeploymentState.PREPARED)
        {
            throw new DeploymentException("Deployment cannot be aborted: already aborting, or committing.");
        }
        try
        {
        	// Mark the deployment
            deployment.abort();
            
            String localStoreName = getLocalStoreName(deployment.getAuthoringStoreName());   
            	
    		int lastSnapshot = fAVMService.getLatestSnapshotID(localStoreName);
    		
    		if (lastSnapshot > 0)
    		{
    			logger.debug("reverting to old snapshot" + lastSnapshot);
    			{
    	            for(DeployedFile file : deployment)
    	            {
    	            	String dst = localStoreName + ":" + file.getPath();
    	            	AVMNodeDescriptor oldNode = fAVMService.lookup(lastSnapshot, dst);
    	            	
    	            	if(oldNode != null) 
    	            	{
    	    				fAVMService.revert(dst, oldNode);
    	    				logger.debug("reverted :" + dst);
    	            		
    	            	}
    	            }
    				logger.debug("store reverted");
    			}
    		}
    		
            /**
             * Delete any temporary files that may have been transferred over.
             */
            for(DeployedFile file : deployment)
            {
            	if(file.getType() == FileType.FILE)
            	{
            		File content = new File(file.getPreLocation());            		
            		content.delete();
            	}
            }
        }
        catch (IOException e)
        {
        	logger.error("Error while aborting ticket:" + ticket, e);
            throw new DeploymentException("Could not abort.", e);
        }
        finally 
        {
            fDeployments.remove(ticket);           
        }		
	}
	

    public void commit(String ticket)
    {
        final String fTicket = ticket;
        AuthenticationUtil.runAs(               
                new AuthenticationUtil.RunAsWork<Void>()
                {
                    public Void doWork() throws Exception
                    {
                        commitImpl(fTicket);
                        return null;
                    }
                }
                , proxyUser);
    }

	private void commitImpl(String ticket) 
	{	
	       AuthenticationUtil.setRunAsUser(proxyUser);
	       
	       Deployment deployment = fDeployments.get(ticket);
	        if (deployment == null)
	        {
	        	String msg = "Could not commit because invalid ticket:" + ticket;
	        	logger.error(msg);
	            throw new DeploymentException(msg);
	        }
	        logger.debug("commit ticket:" + ticket);
	        
	        try
	        {
	        	
	            String localStoreName = getLocalStoreName(deployment.getAuthoringStoreName());   
	            
	            /**
	             * Need to write content into nodes.
	             */
	            for(DeployedFile file : deployment)
	            {
	            	if(file.getType() == FileType.FILE)
	            	{
	            		String dst = localStoreName + ":" + file.getPath();
	            		ContentWriter writer = fAVMService.getContentWriter(dst);
	            		
	            		File content = new File(file.getPreLocation());
	            		writer.putContent(content);            		
	            		content.delete();
	            		
	            		/**
	       	    	  	  * Set GUID
	       	    	  	  */
	       	    	    fAVMService.setGuid(dst, file.getGuid()); 
	            	}
	            }
	            	
	        	/**
	        	 * Need to snapshot the local store
	        	 */
	            Object[] objs = {deployment.getAuthoringStoreName(), new Integer(deployment.getAuthoringVersion())};
	            
	            MessageFormat tagPattern = new MessageFormat("Deployment from store:{0}, version:{1}");
	            MessageFormat descriptionPattern = new MessageFormat("Deployment from store: {0}, version:{1}");
	            
    			logger.debug("finished copying, snapshot remote");
    			fAVMService.createSnapshot(localStoreName, tagPattern.format(objs), descriptionPattern.format(objs));
    			
	            // Mark the deployment as committed
	            deployment.commit();
	        	
	            /**
	             * Now run the post commit runnables.
	             */
	            if (postCommit != null && postCommit.size() > 0)
	            {
	                for (FSDeploymentRunnable runnable : postCommit)
	                {
	                    try
	                    {
	                        runnable.init(deployment);
	                        runnable.run();
	                    }
	                    catch (Throwable t)
	                    {
	                    	logger.error("Error from postCommit event t:" + t.toString(), t);
	                    }
	                }
	            }

	            logger.debug("commited successfully ticket:" + ticket);
	        }
	        catch (Exception e)
	        {
	        	throw new DeploymentException("Could not commit", e);
	        	//TODO Need to rework such that we can never get here
	        }
	        finally
	        {
	            fDeployments.remove(ticket);
	        }
	}

	/**
	 * Delete a file or directory
	 */
	public void delete(String ticket, String path)
	{
	  final String fTicket = ticket;
	  final String fPath = path;
      AuthenticationUtil.runAs(               
              new AuthenticationUtil.RunAsWork<Void>()
              {
                  public Void doWork() throws Exception
                  {
                      deleteImpl(fTicket, fPath);
                      return null;
                  }
              }
              , proxyUser);
    }

	
	private void deleteImpl(String ticket, String path) 
	{
		Deployment deployment = fDeployments.get(ticket);
		if (deployment == null)
		{
			String msg = "Could not delete because invalid ticket:" + ticket;
			throw new DeploymentException(msg);
		}
		try
		{
			final String localStoreName = getLocalStoreName(deployment.getAuthoringStoreName());
			final String fpath = path;

			RetryingTransactionCallback<Integer> deleteCB = new RetryingTransactionCallback<Integer>()
			{
				public Integer execute() throws Throwable
				{
					String dst = localStoreName + ":" + getLocalPath(fpath);
					
					/**
					 * Remove the file or directory
					 */
					fAVMService.removeNode(dst);
					logger.debug("Delete file" + dst);
					return null;
				}
			};


			RetryingTransactionHelper trn = trxService.getRetryingTransactionHelper();
			trn.doInTransaction(deleteCB);


			/**
			 * Update the deployment record
			 */
			DeployedFile file = new DeployedFile(FileType.DELETED, 
					null,
					path,
					null,
					false);
			deployment.add(file);
		}
		catch (IOException e)
		{
			throw new DeploymentException("Could not update log.", e);
		}		
	}

	/**
	 * Get listing
	 */
	public List<FileDescriptor> getListing(String ticket, String path)
	{
	     final String fTicket = ticket;
	     final String fPath = path;
	     return AuthenticationUtil.runAs(               
	              new AuthenticationUtil.RunAsWork<List<FileDescriptor>>()
	              {
	                  public List<FileDescriptor> doWork() throws Exception
	                  {
	                      return getListingImpl(fTicket, fPath);
	                  }
	              }
	              , proxyUser);
	}
	
	private List<FileDescriptor> getListingImpl(String ticket, String path) 
	{
		Deployment deployment = fDeployments.get(ticket);
	    if (deployment == null)
	    {
	        throw new DeploymentException("getListing invalid ticket. ticket:" + ticket);
	    }
	    
	    String localStoreName = getLocalStoreName(deployment.getAuthoringStoreName());
	    
	    String dst = localStoreName + ":" + getLocalPath(path);

	    // Get the listing for the destination.
        SortedMap<String, AVMNodeDescriptor> dstList = fAVMService.getDirectoryListing(-1, dst);
	    
        List<FileDescriptor> returnVal = new LinkedList<FileDescriptor>();
        for(AVMNodeDescriptor file : dstList.values())
        {
            returnVal.add(new FileDescriptor(file.getName(), mapFileTypeFromAVM(file.getType()), file.getGuid()));
        }
	        
	    return returnVal;
	}
	
	private FileType mapFileTypeFromAVM(int val)
	{
		switch (val)
		{
			case AVMNodeType.PLAIN_FILE:
				return FileType.FILE;
			case AVMNodeType.PLAIN_DIRECTORY:
				return FileType.DIR;
		}
		
		throw new UnsupportedOperationException("Unknown file type in AVM" + val);
	}

	/**
	 * Create new directory
	 */
    public void createDirectory(String ticket, String path, String guid, Set<String>aspects, Map<String, Serializable> props) 
    {
        final String fTicket = ticket;
        final String fpath = path; 
        final String fguid = guid;
        final Set<String>faspects = aspects; 
        final Map<String, Serializable> fprops = props;
        AuthenticationUtil.runAs(               
                new AuthenticationUtil.RunAsWork<Void>()
                {
                    public Void doWork() throws Exception
                    {
                        createDirectoryImpl(fTicket, fpath, fguid, faspects, fprops) ;
                        return null;
                    }
                }
                , proxyUser);
    }

	private void createDirectoryImpl(String ticket, String path, String guid, Set<String>aspects, Map<String, Serializable> props) 
	{
		Deployment deployment = fDeployments.get(ticket);
		if (deployment == null)
		{
			throw new DeploymentException("mkdir invalid ticket. ticket:" + ticket);
		}

		try
		{
			final String localStoreName = getLocalStoreName(deployment.getAuthoringStoreName());
			final String dst = localStoreName + ":" + getLocalPath(path);
			
			final Set<String> faspects = aspects;
			final Map<String, Serializable> fprops = props;
			final String fguid = guid;
        	RetryingTransactionHelper trn = trxService.getRetryingTransactionHelper();
			
        	RetryingTransactionCallback<Boolean> createDirectoryCB = new RetryingTransactionCallback<Boolean>() {
				public Boolean execute() throws Throwable {

					AVMNodeDescriptor dest = fAVMService.lookup(-1, dst);
					if (dest != null && dest.isFile()) 
					{
						// The new directory replaces the file
						fAVMService.removeNode(dst);
						dest = null;
					}

					List<QName> aspectList = new ArrayList<QName>(faspects
							.size());
					for (String aspect : faspects) 
					{
						aspectList.add(QName.createQName(aspect));
					}

					Map<QName, PropertyValue> propertyMap = new HashMap<QName, PropertyValue>();
					for (String key : fprops.keySet()) 
					{
						propertyMap.put(QName.createQName(key),
								new PropertyValue(null, fprops.get(key)));
					}

					fAVMService.createDirectory(getParentPath(dst),
							getFileName(dst), aspectList, propertyMap);
					fAVMService.setGuid(dst, fguid);

					return Boolean.TRUE;
				}

			};
    		
    		/**
    		 * Now do the create directory transaction
    		 */
    		trn.doInTransaction(createDirectoryCB);

			DeployedFile file = new DeployedFile(FileType.DIR,
					null,
					path,
					guid,
					true);

			deployment.add(file);
		}
		catch (IOException e)
		{
			throw new DeploymentException("Could not log mkdir of " + path + " error: " + e.toString(), e);
		}
	}

	/**
	 * Send 
	 */
	public OutputStream send(String ticket, String path, String guid, String encoding, String mimeType, Set<String>aspects, Map<String, Serializable> props)
	{
        final String fTicket = ticket;
        final String fpath = path; 
        final String fguid = guid;
        final String fencoding = encoding; 
        final String fmimeType = mimeType;
        final Set<String>faspects = aspects; 
        final Map<String, Serializable> fprops = props;
        return AuthenticationUtil.runAs(               
                new AuthenticationUtil.RunAsWork<OutputStream>()
                {
                    public OutputStream  doWork() throws Exception
                    {
                        return sendImpl(fTicket, fpath, fguid, fencoding, fmimeType, faspects, fprops) ;
                    }
                }
                , proxyUser);
	}
	
	private OutputStream sendImpl(String ticket, String path, String guid, String encoding, String mimeType, Set<String>aspects, Map<String, Serializable> props)
	{
        final Deployment deployment = fDeployments.get(ticket);
        if (deployment == null)
        {
            throw new DeploymentException("Deployment timed out or invalid ticket.");
        }
        
        try
        {     	        	
        	RetryingTransactionHelper trn = trxService.getRetryingTransactionHelper();
        	
			final String localStoreName = getLocalStoreName(deployment.getAuthoringStoreName());
			final String dst = localStoreName + ":" + getLocalPath(path);
			final String fencoding = encoding;
			final String fmimeType = mimeType;
			final Set<String>faspects = aspects;
			final Map<String, Serializable> fprops = props;
			
        	RetryingTransactionCallback<Boolean> sendCB = new RetryingTransactionCallback<Boolean>()
    		{
    			public Boolean execute() throws Throwable
    			{
    				boolean newFile = false;
    				
    				logger.debug("send " + dst);
    				
    				AVMNodeDescriptor dest = fAVMService.lookup(-1, dst);
    				
    				if(dest != null && dest.isDirectory())
    				{
    					// The new file replaces the directory
    					fAVMService.removeNode(dst);
    					dest = null;
    				}
    				
    				if(dest == null)
    				{
    					// This is a new file
    					OutputStream out = fAVMService.createFile(getParentPath(dst), getFileName(dst));
    					out.close();
    					logger.debug("Create file" + dst);
    					newFile = true;
    				}
    				else
    				{	
    					// The file already exists
    					logger.debug("file exists - update" + dst);
    					newFile = false;
    				}
    				
    				List<QName>aspectList = new ArrayList<QName>(faspects.size());
    				for(String aspect : faspects)
    				{
    					aspectList.add(QName.createQName(aspect));
    				}
    				
    				Map<QName, PropertyValue>propertyMap = new HashMap<QName, PropertyValue>();
    				for(String key : fprops.keySet())
    				{
    					propertyMap.put(QName.createQName(key), new PropertyValue(null, fprops.get(key)));
    					
    				}
    			    
    				fAVMService.deleteNodeProperties(dst);
    				fAVMService.setNodeProperties(dst, propertyMap);
    				fAVMService.setEncoding(dst, fencoding);
    	            fAVMService.setMimeType(dst, fmimeType);
    	       
    	            
    				// Add Missing Aspects
    				for(QName aspect : aspectList)
    				{
    					if(!fAVMService.hasAspect(-1, dst, aspect))
    					{
    						fAVMService.addAspect(dst, aspect);
    					}
    				}
    				
    				// Remove Obsolete Aspects
    				for(QName aspect : fAVMService.getAspects(-1, dst))
    				{
    					if(!aspectList.contains(aspect))
    					{
    						fAVMService.removeAspect(dst, aspect);
    					}
    				}
    	            
    				return new Boolean(newFile);
    			}
    		};    		
	
    		/**
    		 * Now do the send transaction
    		 */
    		Boolean newFile = trn.doInTransaction(sendCB);
    		
    		
			/**
			 * Open a temporary file to receive the contents.
			 */
			File tempFile = TempFileProvider.createTempFile(ticket, "bin");
			OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));

	   	   /**
	   	  	* Update Deployment record
	   	  	*/
       		DeployedFile file = new DeployedFile(FileType.FILE,
                                             tempFile.getAbsolutePath(),
                                             getLocalPath(path),
                                             guid,
                                             newFile);
       		deployment.add(file);
        	
        	return out;
        } 
        catch (IOException e)	    
        {
        		throw new DeploymentException("Could not send for path:" + path, e);
	    }
	}
	
	/**
	 * 
	 */
    public void updateDirectory(String ticket, String path, String guid, Set<String>aspects, Map<String, Serializable> props) 
    {
        final String fTicket = ticket;
        final String fpath = path; 
        final String fguid = guid;
        final Set<String>faspects = aspects; 
        final Map<String, Serializable> fprops = props;
        AuthenticationUtil.runAs(               
                new AuthenticationUtil.RunAsWork<Void>()
                {
                    public Void doWork() throws Exception
                    {
                        updateDirectoryImpl(fTicket, fpath, fguid, faspects, fprops) ;
                        return null;
                    }
                }
                , proxyUser);
    }

 
	private void updateDirectoryImpl(String ticket, String path, String guid, Set<String>aspects, Map<String, Serializable> props) 
	{
		Deployment deployment = fDeployments.get(ticket);
		if (deployment == null)
		{
			throw new DeploymentException("Deployment invalid ticket.");
		}
		try
		{
			
			String localStoreName = getLocalStoreName(deployment.getAuthoringStoreName());
			String dst = localStoreName + ":" + getLocalPath(path);
			
			logger.debug("Update Directory" + dst);
			

			List<QName>aspectList = new ArrayList<QName>(aspects.size());
			for(String aspect : aspects)
			{
				aspectList.add(QName.createQName(aspect));
			}

			Map<QName, PropertyValue>propertyMap = new HashMap<QName, PropertyValue>();
			for(String key : props.keySet())
			{
				propertyMap.put(QName.createQName(key), new PropertyValue(null, props.get(key)));

			}
			
			fAVMService.deleteNodeProperties(dst);
			fAVMService.setNodeProperties(dst, propertyMap);

			// Add Missing Aspects
			for(QName aspect : aspectList)
			{
				if(!fAVMService.hasAspect(-1, dst, aspect))
				{
					fAVMService.addAspect(dst, aspect);
				}
			}

			// Remove Obsolete Aspects
			for(QName aspect : fAVMService.getAspects(-1, dst))
			{
				if(!aspectList.contains(aspect))
				{
					fAVMService.removeAspect(dst, aspect);
				}
			}

			/**
			 * Set GUID
			 */
			fAVMService.setGuid(dst, guid); 

			/**
			 * Update Deployment
			 */
			DeployedFile file = new DeployedFile(FileType.SETGUID,
					null,
					path,
					guid,
					false);
			deployment.add(file);
			logger.debug("end upate directory" + dst);

		}
		catch (Exception e)
		{
			throw new DeploymentException("Could not set guid on " + path, e);
		}		
	}
	
	private String getLocalStoreName(String sourceStoreName)
	{
		String name = getStoreNamePattern();
		return name.replace("%storeName%", sourceStoreName);	
	}
	
	private String getLocalPath(String path)
	{
		return getRootPath() + path;
	}
	
	private String getParentPath(String path) 
	{
		int pos  = path.lastIndexOf('/');
		return path.substring(0, pos + 1);
	}
	
	private String getFileName(String path)
	{
		int pos  = path.lastIndexOf('/');
		return path.substring(pos + 1);
	}

	public void setPostCommit(List<FSDeploymentRunnable> postCommit) {
		this.postCommit = postCommit;
	}

	public List<FSDeploymentRunnable> getPostCommit() {
		return postCommit;
	}

	public void setPrepare(List<FSDeploymentRunnable> prepare) {
		this.prepare = prepare;
	}

	public List<FSDeploymentRunnable> getPrepare() {
		return prepare;
	}

	public void setTransactionService(TransactionService trxService) {
		this.trxService = trxService;
	}

	public TransactionService getTransactionService() {
		return trxService;
	}

	/**
	 * The root path is the root for deployed files.   Files are placed below the root.
	 * @param rootPath
	 */
	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public String getRootPath() {
		return rootPath;
	}

	public int getCurrentVersion(String target, String storeName) {
		// Not implemented yet
		return -1;
	}

	public void setStoreNamePattern(String storeNamePattern) {
		this.storeNamePattern = storeNamePattern;
	}

	public String getStoreNamePattern() {
		return storeNamePattern;
	}

    public void setProxyUser(String proxyUser)
    {
        this.proxyUser = proxyUser;
    }

    public String getProxyUser()
    {
        return proxyUser;
    }
}
