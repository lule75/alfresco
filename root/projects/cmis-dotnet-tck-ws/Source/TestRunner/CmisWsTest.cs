﻿/*
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
using System;
using System.Net;
using System.Net.Security;
using System.ServiceModel;
using System.Collections.Generic;
using System.Security.Cryptography.X509Certificates;
using System.Configuration;
using System.Reflection;
using WcfCmisWSTests.CmisServices;

///
/// author: Stas Sokolovsky
///
namespace WcfCmisWSTests
{
    public class CmisWsTest
    {
        static CmisLogger logger = CmisLogger.getInstance();

        static Dictionary<string, Object> tests = new Dictionary<string, Object>();

        static CmisWsTest()
        {
            tests.Add("RepositoryService", new RepositoryServiceClientTest());
            tests.Add("ObjectService", new ObjectServiceClientTest());
            tests.Add("RelationshipService", new RelationshipServiceClientTest());
            tests.Add("NavigationService", new NavigationServiceClientTest());
            tests.Add("MultiFilingService", new MultiFilingServiceClientTest());
            tests.Add("DiscoveryService", new DiscoveryServiceClientTest());
            tests.Add("VersioningService", new VersioningServiceClientTest());
            tests.Add("ACLService", new ACLServiceClientTest());
        }

        static void Main(string[] args)
        {
            configureLogger();

            writeHeader();

            string[] testingServices = getTestingServices();

            CmisTestRunner testRunner = new CmisTestRunner();

            foreach (string service in testingServices)
            {
                Object test;
                if (tests.TryGetValue(service, out test))
                {
                    testRunner.run(test, service);
                }
            }

            showStatistics(testRunner);
        }

        static void writeHeader()
        {
            logger.log("******************************************************************");
            logger.log("*                                                                *");
            logger.log("*                          CMIS TEST                             *");
            logger.log("*       Copyright (C) 2005-2009 Alfresco Software Limited.       *");
            logger.log("*                                                                *");
            logger.log("******************************************************************");
            logger.log();
            logger.log("Checking configuration...");
            logger.log();
            logger.log("RepositoryService: ");
            logger.log(CmisClientFactory.getInstance().getRepositoryServiceClient().Endpoint.Address.ToString());
            logger.log();
            logger.log("ObjectService: ");
            logger.log(CmisClientFactory.getInstance().getObjectServiceClient().Endpoint.Address.ToString());
            logger.log();
            logger.log("RelationshipService: ");
            logger.log(CmisClientFactory.getInstance().getRelationshipServiceClient().Endpoint.Address.ToString());
            logger.log();
            logger.log("DiscoveryService: ");
            logger.log(CmisClientFactory.getInstance().getDiscoveryServiceClient().Endpoint.Address.ToString());
            logger.log();
            logger.log("MultiMilingService: ");
            logger.log(CmisClientFactory.getInstance().getMultiFilingServiceClient().Endpoint.Address.ToString());
            logger.log();
            logger.log("NavigationService: ");
            logger.log(CmisClientFactory.getInstance().getNavigationServiceClient().Endpoint.Address.ToString());
            logger.log();
            logger.log("VersioningService: ");
            logger.log(CmisClientFactory.getInstance().getVersioningServiceClient().Endpoint.Address.ToString());
            logger.log();
            logger.log("Starting...");
            logger.log();
            logger.log();
        }

        static void showStatistics(CmisTestRunner testRunner)
        {
            logger.log();
            logger.log("Finished");
            logger.log("------------------------------------------------------");
            logger.log("Total passed:   " + testRunner.GeneralStatistics.PassedCount);
            logger.log("Total failed:   " + testRunner.GeneralStatistics.FailedCount);
            logger.log("Total skipped:  " + testRunner.GeneralStatistics.SkippedCount);
            logger.log("Total executed: " + testRunner.GeneralStatistics.TotalCount);
            logger.log("------------------------------------------------------");
            logger.log("Totally spent time: " + testRunner.GeneralStatistics.TotalSpentTime + " ms");
            logger.log();
            logger.log();
        }

        static void configureLogger()
        {
            string configLoggingLevel = ConfigurationSettings.AppSettings["logging.level"];
            if (configLoggingLevel != null)
            {
                CmisLogger.GeneralLoggingLevel = Convert.ToInt32(configLoggingLevel);
            }
            string configLoggingFilename = ConfigurationSettings.AppSettings["logging.filename"];
            if (configLoggingFilename != null)
            {
                CmisLogger.Filename = configLoggingFilename;
            }
        }

        static string[] getTestingServices()
        {
            string[] result;
            string testingServices = ConfigurationSettings.AppSettings["testing.services"];
            if (testingServices != null)
            {
                result = testingServices.Split(',');
                for (int i = 0; i < result.Length; i++)
                {
                    result[i] = result[i].Trim();
                }
            }
            else
            {
                List<string> services = new List<string>();
                foreach (string servicename in tests.Keys)
                {
                    services.Add(servicename);
                }
                result = services.ToArray();
            }
            return result;
        }

    }
}