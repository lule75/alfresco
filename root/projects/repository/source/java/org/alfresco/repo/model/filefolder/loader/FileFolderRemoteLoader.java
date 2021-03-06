/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.repo.model.filefolder.loader;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * A class that communicates with the remote FileFolder interface to perform
 * loading of the system using the standard models.
 * <p>
 * TODO: The transport interface must be hidden behind and implementation
 *       of the standard FileFolderService interface.
 * 
 * @author Derek Hulley
 * @since 2.1
 */
public class FileFolderRemoteLoader
{
    private static final String STORE_PROTOCOL = "FileFolderRemoteLoader";
    
    private Properties properties;
    private String username;
    private String password;
    
    private LoaderSession session;
    private AbstractLoaderThread[] threads;
    
    public FileFolderRemoteLoader(Properties properties, String username, String password)
    {
        this.properties = properties;
        this.username = username;
        this.password = password;
    }
    
    public synchronized void initialize() throws Exception
    {
        if (session != null || threads != null)
        {
            throw new AlfrescoRuntimeException("Application already initialized");
        }
        session = FileFolderRemoteLoader.makeSession(username, password, properties);
        threads = FileFolderRemoteLoader.makeThreads(session, properties);
        
        // Log the initial summaries
        String summary = session.getSummary();
        session.logVerbose(summary, true);
        session.logSummary(summary);
        session.logError(summary);
        
        // Header the outputs
        session.logVerbose(LoaderSession.getLineEnding(), true);
        session.logVerbose(COLUMNS_VERBOSE, true);
        session.logSummary(LoaderSession.getLineEnding());
        session.logSummary(COLUMNS_SUMMARY);
    }
    
    private static final String COLUMNS_VERBOSE =
        String.format("%40s\t%15s\t%15s\t%15s\t%15s\t%15s",
                "NAME", "COUNT", "TIME", "AVERAGE TIME", "PER SECOND", "DESCRIPTION");
    private static final String COLUMNS_SUMMARY =
        String.format("%40s\t%15s\t%15s\t%15s\t%15s\t%15s",
                "NAME", "COUNT", "TOTAL TIME", "AVERAGE TIME", "PER SECOND", "DESCRIPTION");
    
    public synchronized void start()
    {
        if (session == null || threads == null)
        {
            throw new AlfrescoRuntimeException("Application not initialized");
        }
        
        // Fire up the threads
        for (Thread thread : threads)
        {
            thread.start();
        }
    }
    
    public synchronized void stop()
    {
        // Stop the threads
        for (AbstractLoaderThread thread : threads)
        {
            thread.setStop();
        }
        // Now join each thread to make sure they all finish their current operation
        for (AbstractLoaderThread thread : threads)
        {
            // Notify any waits
            synchronized(thread)
            {
                thread.notifyAll();
            }
            try
            {
                thread.join();
            }
            catch (InterruptedException e) {}
        }
        // Log each thread's summary
        for (AbstractLoaderThread thread : threads)
        {
            String summary = thread.getSummary();
            session.logSummary(summary);
        }
    }
    
    public void dumpThreadSummaries()
    {
        System.out.println("");
        System.out.println(COLUMNS_SUMMARY);
        // Dump each thread's summary
        for (AbstractLoaderThread thread : threads)
        {
            String summary = thread.getSummary();
            System.out.println(summary);
        }
    }

    public static final String PROP_SESSION_NAME = "session.name";
    public static final String PROP_SESSION_SOURCE_DIR = "session.sourceDir";
    public static final String PROP_SESSION_STORE_IDENTIFIERS = "session.storeIdentifiers";
    public static final String PROP_SESSION_RMI_URLS = "session.rmiUrls";
    public static final String PROP_SESSION_FOLDER_PROFILE = "session.folderProfile";
    
    /**
     * Factory method to construct a session using the given properties.
     */
    private static LoaderSession makeSession(String username, String password, Properties properties) throws Exception
    {
        // Name
        String name = properties.getProperty(PROP_SESSION_NAME);
        FileFolderRemoteLoader.checkProperty(PROP_SESSION_STORE_IDENTIFIERS, name);
        
        // Source files
        String sourceDirStr = properties.getProperty(PROP_SESSION_SOURCE_DIR);
        File sourceDir = new File(sourceDirStr);
        
        // Stores
        String storeIdentifiersStr = properties.getProperty(PROP_SESSION_STORE_IDENTIFIERS);
        FileFolderRemoteLoader.checkProperty(PROP_SESSION_STORE_IDENTIFIERS, storeIdentifiersStr);
        StringTokenizer tokenizer = new StringTokenizer(storeIdentifiersStr, ",");
        Set<StoreRef> storeRefs = new HashSet<StoreRef>();
        while (tokenizer.hasMoreTokens())
        {
            String storeIdentifier = tokenizer.nextToken().trim();
            storeRefs.add(new StoreRef(STORE_PROTOCOL, storeIdentifier));
        }
        FileFolderRemoteLoader.checkProperty(PROP_SESSION_STORE_IDENTIFIERS, storeRefs);
        
        // RMI URLs
        String rmiUrlsStr = properties.getProperty(PROP_SESSION_RMI_URLS);
        FileFolderRemoteLoader.checkProperty(PROP_SESSION_RMI_URLS, rmiUrlsStr);
        tokenizer = new StringTokenizer(rmiUrlsStr, ",");
        Set<String> rmiUrls = new HashSet<String>();
        while (tokenizer.hasMoreTokens())
        {
            String rmiUrl = tokenizer.nextToken().trim();
            rmiUrls.add(rmiUrl);
        }
        FileFolderRemoteLoader.checkProperty(PROP_SESSION_STORE_IDENTIFIERS, rmiUrls);
        
        // RMI URLs
        String folderProfilesStr = properties.getProperty(PROP_SESSION_FOLDER_PROFILE);
        FileFolderRemoteLoader.checkProperty(PROP_SESSION_FOLDER_PROFILE, folderProfilesStr);
        tokenizer = new StringTokenizer(folderProfilesStr, ",");
        ArrayList<Integer> folderProfilesList = new ArrayList<Integer>(5);
        while (tokenizer.hasMoreTokens())
        {
            String folderProfileStr = tokenizer.nextToken().trim();
            Integer folderProfile = Integer.valueOf(folderProfileStr);
            folderProfilesList.add(folderProfile);
        }
        FileFolderRemoteLoader.checkProperty(PROP_SESSION_FOLDER_PROFILE, folderProfilesList);
        int[] folderProfiles = new int[folderProfilesList.size()];
        for (int i = 0; i < folderProfiles.length; i++)
        {
            folderProfiles[i] = folderProfilesList.get(i);
        }
        if (folderProfiles.length == 0 || folderProfiles[0] != 1)
        {
            throw new LoaderClientException(
                    "'" + PROP_SESSION_FOLDER_PROFILE + "' must always start with '1', " +
                    "which represents the root of the hierarchy, and have at least one other value.  " +
                    "E.g. '1, 3'");
        }
        
        // Construct
        LoaderSession session = new LoaderSession(
                username,
                password,
                name,
                rmiUrls,
                storeRefs,
                sourceDir,
                folderProfiles);
        
        // Initialize the session
        session.initialize();
        
        // Done
        return session;
    }
    
    /**
     * Factory method to construct the worker threads.
     */
    private static AbstractLoaderThread[] makeThreads(LoaderSession session, Properties properties) throws Exception
    {
        ArrayList<AbstractLoaderThread> threads = new ArrayList<AbstractLoaderThread>(3);
        // Iterate over the properties and pick out the thread descriptors
        for (Object o : properties.keySet())
        {
            String propertyName = (String)o;
            if (!propertyName.startsWith("test.load."))
            {
                continue;
            }
            String name = propertyName;
            
            // Get the type of the test
            int lastIndex = propertyName.indexOf(".", 10);
            if (lastIndex < 0)
            {
                throw new LoaderClientException(
                        "Invalid test loader property.  " +
                        "It should be of the form 'test.load.upload.xyz=...': " + propertyName);
            }
            String type = propertyName.substring(10, lastIndex);
            
            // Get the values
            String valuesStr = properties.getProperty(propertyName);
            FileFolderRemoteLoader.checkProperty(propertyName, valuesStr);
            // Parse it into the well-known values
            String[] strValues = new String[] {"1", "0", "0", "1", "false", "1"};
            int index = 0;
            StringTokenizer tokenizer = new StringTokenizer(valuesStr, ",");
            while (tokenizer.hasMoreTokens())
            {
                String value = tokenizer.nextToken().trim();
                if (value.length() > 0)
                {
                    strValues[index] = value;
                }
                index++;
                if (index >= strValues.length)
                {
                    break;
                }
            }
            long testCount = 1L;
            long testPeriod = 0L;
            long testTotal = 0L;
            long testDepth = 1L;
            boolean testVerbose = false;
            long filesPerIteration = 1;
            try
            {
                testCount = Long.parseLong(strValues[0]);
                testPeriod = Long.parseLong(strValues[1]);
                testTotal = Long.parseLong(strValues[2]);
                testDepth = Long.parseLong(strValues[3]);
                testVerbose = Boolean.parseBoolean(strValues[4]);
                filesPerIteration = Long.parseLong(strValues[5]);
            }
            catch (Throwable e)
            {
                throw new LoaderClientException(
                        "Unable to parse the loader configuration for '" + name + "'. " + LoaderSession.getLineEnding() +
                        "The correct format is [threadCount], [period(ms)], [total], [folder depth], [verbose]<, [filesPerIteration]>");
            }
            
            // Construct
            for (int i = 0; i < testCount; i++)
            {
                AbstractLoaderThread thread = null;
                if (type.equals("upload"))
                {
                    thread = new LoaderUploadThread(session, name, testPeriod, testTotal, testDepth, testVerbose, filesPerIteration);
                }
                else if (type.equals("totals"))
                {
                    thread = new LoaderTotalsThread(session, name, testPeriod, testTotal, testDepth, testVerbose);
                }
                else if (type.equals("listFolders"))
                {
                    thread = new LoaderListFoldersThread(session, name, testPeriod, testTotal, testDepth, testVerbose);

                }
                else if(type.equals("delete"))
                {
                    thread = new LoaderDeleteThread(session, name, testPeriod, testTotal, testDepth, testVerbose, filesPerIteration);
                }
                else if(type.equals("update"))
                {
                    thread = new LoaderUpdateThread(session, name, testPeriod, testTotal, testDepth, testVerbose, filesPerIteration);

                }
                else if(type.equals("coci"))
                {
                     thread = new LoaderCOCIThread(session, name, testPeriod, testTotal, testDepth, testVerbose, filesPerIteration);
                }
                else
                {
                    throw new LoaderClientException("Unknown test type: " + name);
                }
                threads.add(thread);
            }
        }
        // Done
        AbstractLoaderThread[] ret = new AbstractLoaderThread[threads.size()];
        return threads.toArray(ret);
    }
    
    /**
     * Checks for null values and empty collections.
     */
    private static void checkProperty(String propertyName, Object propertyValue) throws LoaderClientException
    {
        if (propertyValue == null)
        {
            throw new LoaderClientException("'" + propertyName + "' must be provided.");
        }
        else if (propertyValue instanceof Collection)
        {
            Collection propertyCollection = (Collection) propertyValue;
            int size = propertyCollection.size();
            if (size == 0)
            {
                throw new LoaderClientException("'" + propertyName + "' does not have any values.");
            }
        }
    }
    
    public static void main(String ... args)
    {
        Map<String, String> argMap = ripArgs(args);
        String username = argMap.get("username");
        String password = argMap.get("password");
        String config = argMap.get("config");
        if (username == null || password == null || config == null)
        {
            printUsage();
            System.exit(1);
        }
        try
        {
            File propertiesFile = new File(config);
            if (!propertiesFile.exists())
            {
                System.err.println("Unable to find config file: " + config);
            }
            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesFile));

            FileFolderRemoteLoader app = new FileFolderRemoteLoader(properties, username, password);
            
            // Initialize
            app.initialize();
            
            // Run
            app.start();
            
            // Now lower this threads priority
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            
            // Wait for a quit signal
            System.out.println("Running test " + app.session.getName() + ".");
            System.out.println("   Enter 'q' to quit.");
            System.out.println("   Enter 's' to dump a thread summary.");
            while (true)
            {
                int keyPress = System.in.read();
                if (keyPress == 'Q' || keyPress == 'q')
                {
                    break;
                }
                else if (keyPress == 'S' || keyPress == 's')
                {
                    app.dumpThreadSummaries();
                }
                else if (System.in.available() > 0)
                {
                    // Don't wait, just process
                    continue;
                }
                // No more keypresses so just wait
                Thread.yield();
            }
            // Finish off
            app.stop();
            System.out.println("The test is complete.");
        }
        catch (LoaderClientException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        catch (Throwable e)
        {
            System.err.println("A failure prevented proper execution.");
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
    
    private static Map<String, String> ripArgs(String ... args)
    {
        Map<String, String> argsMap = new HashMap<String, String>(5);
        for (String arg : args)
        {
            int index = arg.indexOf('=');
            if (!arg.startsWith("--") || index < 0 || index == arg.length() - 1)
            {
                // Ignore it
                continue;
            }
            String name = arg.substring(2, index);
            String value = arg.substring(index + 1, arg.length());
            argsMap.put(name, value);
        }
        return argsMap;
    }
    
    private static void printUsage()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n")
          .append("Usage\n")
          .append("   java -jar ...  --username=<username> --password=<password> --config=<config file> \n");
        System.out.println(sb.toString());
    }
}
