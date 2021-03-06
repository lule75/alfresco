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
package org.alfresco.repo.workflow.jbpm;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.workflow.BPMEngineRegistry;
import org.alfresco.repo.workflow.TaskComponent;
import org.alfresco.repo.workflow.WorkflowComponent;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.WorkflowPackageComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowDeployment;
import org.alfresco.service.cmr.workflow.WorkflowException;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseSpringTest;
import org.springframework.core.io.ClassPathResource;


/**
 * JBPM Engine Tests
 * 
 * @author davidc
 */
public class JBPMEngineTest extends BaseSpringTest
{
    AuthenticationComponent authenticationComponent;
    NodeService nodeService;
    WorkflowComponent workflowComponent;
    TaskComponent taskComponent;
    WorkflowPackageComponent packageComponent;
    WorkflowDefinition testWorkflowDef;
    NodeRef testNodeRef;
        
    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        // run as system
        authenticationComponent = (AuthenticationComponent)applicationContext.getBean("authenticationComponent");
        authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        BPMEngineRegistry registry = (BPMEngineRegistry)applicationContext.getBean("bpm_engineRegistry");
        workflowComponent = registry.getWorkflowComponent("jbpm");
        taskComponent = registry.getTaskComponent("jbpm");
        packageComponent = (WorkflowPackageComponent)applicationContext.getBean("workflowPackageImpl"); 
        
        // deploy test process messages
        I18NUtil.registerResourceBundle("jbpmresources/test-messages");
        
        // deploy test process definition
        ClassPathResource processDef = new ClassPathResource("jbpmresources/test_processdefinition.xml");
        assertFalse(workflowComponent.isDefinitionDeployed(processDef.getInputStream(), MimetypeMap.MIMETYPE_XML));
        WorkflowDeployment deployment = workflowComponent.deployDefinition(processDef.getInputStream(), MimetypeMap.MIMETYPE_XML); 
        testWorkflowDef = deployment.definition; 
        assertNotNull(testWorkflowDef);
        assertEquals("jbpm$test", testWorkflowDef.name);
        assertEquals("1", testWorkflowDef.version);
        assertTrue(workflowComponent.isDefinitionDeployed(processDef.getInputStream(), MimetypeMap.MIMETYPE_XML));

        // get valid node ref
        nodeService = (NodeService)applicationContext.getBean(ServiceRegistry.NODE_SERVICE.getLocalName());
        testNodeRef = nodeService.getRootNode(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
        nodeService.setProperty(testNodeRef, ContentModel.PROP_CREATED, new Date());
    }

    
    @Override
    protected void onTearDownInTransaction()
    {
        authenticationComponent.clearCurrentSecurityContext();
    }
    
    
    public void testGetWorkflowDefinitions()
    {
        List<WorkflowDefinition> workflowDefs = workflowComponent.getDefinitions();
        assertNotNull(workflowDefs);
        assertTrue(workflowDefs.size() > 0);
    }
    
    
    public void testDeployWorkflow() throws Exception
    {
        ClassPathResource processDef = new ClassPathResource("jbpmresources/test_processdefinition.xml");
        WorkflowDeployment deployment = workflowComponent.deployDefinition(processDef.getInputStream(), MimetypeMap.MIMETYPE_XML); 
        testWorkflowDef = deployment.definition; 
        assertNotNull(testWorkflowDef);
        assertEquals("jbpm$test", testWorkflowDef.name);
        assertEquals("2", testWorkflowDef.version);
    }
    
    
    public void testStartWorkflow()
    {
        try
        {
            @SuppressWarnings("unused") WorkflowPath path = workflowComponent.startWorkflow("norfolknchance", null);
            fail("Failed to catch invalid definition id");
        }
        catch(WorkflowException e)
        {
        }

        // TODO: Determine why process definition is loaded, even though it doesn't exist
//        try
//        {
//            @SuppressWarnings("unused") WorkflowPosition pos = workflowComponent.startProcess("1000", null);
//            fail("Failed to catch workflow definition id that does not exist");
//        }
//        catch(WorkflowException e)
//        {
//        }

        WorkflowDefinition workflowDef = getTestDefinition();
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, null);
        assertNotNull(path);
        assertTrue(path.id.endsWith("-@"));
        assertNotNull(path.node);
        assertNotNull(path.instance);
        assertEquals(workflowDef.id, path.instance.definition.id);
    }

    
    public void testGetWorkflowById()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, null);
        assertNotNull(path);
        assertTrue(path.id.endsWith("-@"));
        assertNotNull(path.node);
        assertNotNull(path.instance);
        assertEquals(workflowDef.id, path.instance.definition.id);
        WorkflowInstance instance = workflowComponent.getWorkflowById(path.instance.id);
        assertNotNull(instance);
        assertEquals(path.instance.id, instance.id);
        
        workflowComponent.cancelWorkflow(instance.id);
    	WorkflowInstance result = workflowComponent.getWorkflowById(instance.id);
    	assertNull("The workflow isntance should be null!", result);
    }
    
    
    public void testStartWorkflowParameters()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.PROP_TASK_ID, 3);  // protected - shouldn't be written
        params.put(WorkflowModel.PROP_DUE_DATE, new Date());  // task instance field
        params.put(WorkflowModel.PROP_PRIORITY, 1);  // task instance field
        params.put(WorkflowModel.PROP_PERCENT_COMPLETE, 10);  // context variable
        params.put(QName.createQName("", "Message"), "Hello World");  // context variable outside of task definition
        params.put(QName.createQName("", "Array"), new String[] { "one", "two" });  // context variable outside of task definition
        params.put(QName.createQName("", "NodeRef"), new NodeRef("workspace://1/1001"));  // context variable outside of task definition
        params.put(ContentModel.PROP_OWNER, AuthenticationUtil.getAdminUserName());  // task assignment
        
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, params);
        assertNotNull(path);
        assertTrue(path.id.endsWith("-@"));
        assertNotNull(path.node);
        assertNotNull(path.instance);
        assertEquals(workflowDef.id, path.instance.definition.id);
        List<WorkflowTask> tasks1 = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks1);
        assertEquals(1, tasks1.size());

        WorkflowTask task = tasks1.get(0);
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_TASK_ID));
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_DUE_DATE));
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_PRIORITY));
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_PERCENT_COMPLETE));
        assertTrue(task.properties.containsKey(ContentModel.PROP_OWNER));
        
        NodeRef initiator = path.instance.initiator;
        String initiatorUsername = (String)nodeService.getProperty(initiator, ContentModel.PROP_USERNAME);
        assertEquals(AuthenticationUtil.getAdminUserName(), initiatorUsername);
    }

    
    public void testUpdateTask()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.PROP_TASK_ID, 3);  // protected - shouldn't be written
        params.put(WorkflowModel.PROP_DUE_DATE, new Date());  // task instance field
        params.put(WorkflowModel.PROP_PRIORITY, 1);  // task instance field
        params.put(WorkflowModel.PROP_PERCENT_COMPLETE, 10);  // context variable
        params.put(QName.createQName("", "Message"), "Hello World");  // context variable outside of task definition
        params.put(QName.createQName("", "Array"), new String[] { "one", "two" });  // context variable outside of task definition
        params.put(QName.createQName("", "NodeRef"), new NodeRef("workspace://1/1001"));  // context variable outside of task definition
        params.put(ContentModel.PROP_OWNER, AuthenticationUtil.getAdminUserName());  // task assignment
        
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, params);
        assertNotNull(path);
        assertTrue(path.id.endsWith("-@"));
        assertNotNull(path.node);
        assertNotNull(path.instance);
        assertEquals(workflowDef.id, path.instance.definition.id);
        List<WorkflowTask> tasks1 = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks1);
        assertEquals(1, tasks1.size());

        WorkflowTask task = tasks1.get(0);
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_TASK_ID));
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_DUE_DATE));
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_PRIORITY));
        assertTrue(task.properties.containsKey(WorkflowModel.PROP_PERCENT_COMPLETE));
        assertTrue(task.properties.containsKey(ContentModel.PROP_OWNER));

        // update with null parameters
        try
        {
            WorkflowTask taskU1 = taskComponent.updateTask(task.id, null, null, null);
            assertNotNull(taskU1);
        }
        catch(Throwable e)
        {
            fail("Task update failed with null parameters");
        }
        
        // update property value
        Map<QName, Serializable> updateProperties2 = new HashMap<QName, Serializable>();
        updateProperties2.put(WorkflowModel.PROP_PERCENT_COMPLETE, 100);
        WorkflowTask taskU2 = taskComponent.updateTask(task.id, updateProperties2, null, null);
        assertEquals(100, taskU2.properties.get(WorkflowModel.PROP_PERCENT_COMPLETE));

        // add to assocation
        QName assocName = QName.createQName("", "TestAssoc");
        List<NodeRef> toAdd = new ArrayList<NodeRef>();
        toAdd.add(new NodeRef("workspace://1/1001"));
        toAdd.add(new NodeRef("workspace://1/1002"));
        toAdd.add(new NodeRef("workspace://1/1003"));
        Map<QName, List<NodeRef>> addAssocs = new HashMap<QName, List<NodeRef>>();
        addAssocs.put(assocName, toAdd);
        WorkflowTask taskU3 = taskComponent.updateTask(task.id, null, addAssocs, null);
        assertNotNull(taskU3.properties.get(assocName));
        assertEquals(3, ((List<?>)taskU3.properties.get(assocName)).size());
        
        // add to assocation again
        List<NodeRef> toAddAgain = new ArrayList<NodeRef>();
        toAddAgain.add(new NodeRef("workspace://1/1004"));
        toAddAgain.add(new NodeRef("workspace://1/1005"));
        Map<QName, List<NodeRef>> addAssocsAgain = new HashMap<QName, List<NodeRef>>();
        addAssocsAgain.put(assocName, toAddAgain);
        WorkflowTask taskU4 = taskComponent.updateTask(task.id, null, addAssocsAgain, null);
        assertNotNull(taskU4.properties.get(assocName));
        assertEquals(5, ((List<?>)taskU4.properties.get(assocName)).size());
        
        // remove assocation
        List<NodeRef> toRemove = new ArrayList<NodeRef>();
        toRemove.add(new NodeRef("workspace://1/1002"));
        toRemove.add(new NodeRef("workspace://1/1003"));
        Map<QName, List<NodeRef>> removeAssocs = new HashMap<QName, List<NodeRef>>();
        removeAssocs.put(assocName, toRemove);
        WorkflowTask taskU5 = taskComponent.updateTask(task.id, null, null, removeAssocs);
        assertNotNull(taskU5.properties.get(assocName));
        assertEquals(3, ((List<?>)taskU5.properties.get(assocName)).size());
    }
    
    
    public void testGetWorkflowInstances()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        workflowComponent.startWorkflow(workflowDef.id, null);
        workflowComponent.startWorkflow(workflowDef.id, null);
        List<WorkflowInstance> instances = workflowComponent.getActiveWorkflows(workflowDef.id);
        assertNotNull(instances);
        assertEquals(2, instances.size());
        for (WorkflowInstance instance : instances)
        {
            assertEquals(workflowDef.id, instance.definition.id);
        }
    }
    
    
    public void testGetPositions()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        workflowComponent.startWorkflow(workflowDef.id, null);
        List<WorkflowInstance> instances = workflowComponent.getActiveWorkflows(workflowDef.id);
        assertNotNull(instances);
        assertEquals(1, instances.size());
        List<WorkflowPath> paths = workflowComponent.getWorkflowPaths(instances.get(0).id);
        assertNotNull(paths);
        assertEquals(1, paths.size());
        assertEquals(instances.get(0).id, paths.get(0).instance.id);
        assertTrue(paths.get(0).id.endsWith("-@"));
    }

    
    public void testCancelWorkflowInstance()
    {
    	WorkflowDefinition workflowDef = getTestDefinition();
        workflowComponent.startWorkflow(workflowDef.id, null);
        List<WorkflowInstance> instances1 = workflowComponent.getActiveWorkflows(workflowDef.id);
        assertNotNull(instances1);
        assertEquals(1, instances1.size());
        List<WorkflowTask> tasks = taskComponent.getAssignedTasks(AuthenticationUtil.getAdminUserName(), WorkflowTaskState.IN_PROGRESS);
        assertNotNull(tasks);
        assertTrue(tasks.size() > 0);
        WorkflowInstance cancelledInstance = workflowComponent.cancelWorkflow(instances1.get(0).id);
        assertNotNull(cancelledInstance);
        assertFalse(cancelledInstance.active);
        List<WorkflowInstance> instances2 = workflowComponent.getActiveWorkflows(workflowDef.id);
        assertNotNull(instances2);
        assertEquals(0, instances2.size());
        List<WorkflowTask> tasks1 = taskComponent.getAssignedTasks(AuthenticationUtil.getAdminUserName(), WorkflowTaskState.IN_PROGRESS);
        assertNotNull(tasks1);
        tasks1 = filterTasksByWorkflowInstance(tasks1, cancelledInstance.id);
        assertEquals(0, tasks1.size());
    }
    
     
    public void testSignal()
    {
        Map<QName, Serializable> parameters = new HashMap<QName, Serializable>();
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "testNode"), testNodeRef);
        WorkflowDefinition workflowDef = getTestDefinition();
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, parameters);
        assertNotNull(path);
        WorkflowPath updatedPath = workflowComponent.signal(path.id, path.node.transitions[1].id);
        assertNotNull(updatedPath);
    }
    

    public void testGetAssignedTasks()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        Map<QName, Serializable> parameters = new HashMap<QName, Serializable>();
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "reviewer"), AuthenticationUtil.getAdminUserName());
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "testNode"), testNodeRef);
        parameters.put(QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package"), packageComponent.createPackage(null));
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, parameters);
        assertNotNull(path);
        List<WorkflowTask> tasks = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        WorkflowTask updatedTask = taskComponent.endTask(tasks.get(0).id, path.node.transitions[0].id);
        assertNotNull(updatedTask);
        List<WorkflowTask> completedTasks = taskComponent.getAssignedTasks(AuthenticationUtil.getAdminUserName(), WorkflowTaskState.COMPLETED);
        assertNotNull(completedTasks);
        completedTasks = filterTasksByWorkflowInstance(completedTasks, path.instance.id);
        assertEquals(1, completedTasks.size());
        List<WorkflowTask> assignedTasks = taskComponent.getAssignedTasks(AuthenticationUtil.getAdminUserName(), WorkflowTaskState.IN_PROGRESS);
        assertNotNull(assignedTasks);
        assignedTasks = filterTasksByWorkflowInstance(assignedTasks, path.instance.id);
        assertEquals(1, assignedTasks.size());
        assertEquals("review", assignedTasks.get(0).name);
    }

    
    public void xtestMultiAssign()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        List<String> bpm_assignees = new ArrayList<String>();
        bpm_assignees.add(AuthenticationUtil.getAdminUserName());
        bpm_assignees.add("bob");
        bpm_assignees.add("fred");
        Map<QName, Serializable> parameters = new HashMap<QName, Serializable>();
        parameters.put(QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "assignees"), (Serializable)bpm_assignees);
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "testNode"), testNodeRef);
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, parameters);
        assertNotNull(path);
        List<WorkflowTask> tasks = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        WorkflowTask updatedTask = taskComponent.endTask(tasks.get(0).id, "multi");
        assertNotNull(updatedTask);
    }

    
    public void testEndTask()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        Map<QName, Serializable> parameters = new HashMap<QName, Serializable>();
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "reviewer"), AuthenticationUtil.getAdminUserName());
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "testNode"), testNodeRef);
        parameters.put(QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package"), packageComponent.createPackage(null));
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, parameters);
        assertNotNull(path);
        List<WorkflowTask> tasks1 = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks1);
        assertEquals(1, tasks1.size());
        assertEquals(WorkflowTaskState.IN_PROGRESS, tasks1.get(0).state);
        WorkflowTask updatedTask = taskComponent.endTask(tasks1.get(0).id, null);
        assertNotNull(updatedTask);
        assertEquals(WorkflowTaskState.COMPLETED, updatedTask.state);
        List<WorkflowTask> completedTasks = taskComponent.getAssignedTasks(AuthenticationUtil.getAdminUserName(), WorkflowTaskState.COMPLETED);
        assertNotNull(completedTasks);
        completedTasks = filterTasksByWorkflowInstance(completedTasks, path.instance.id);
        assertEquals(1, completedTasks.size());
        assertEquals(WorkflowTaskState.COMPLETED, completedTasks.get(0).state);
    }
    
    
    public void testGetTask()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        Map<QName, Serializable> parameters = new HashMap<QName, Serializable>();
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "reviewer"), AuthenticationUtil.getAdminUserName());
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "testNode"), testNodeRef);
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, parameters);
        assertNotNull(path);
        assertNotNull(path);
        List<WorkflowTask> tasks1 = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks1);
        assertEquals(1, tasks1.size());
        WorkflowTask getTask = taskComponent.getTaskById(tasks1.get(0).id);
        assertNotNull(getTask);
        assertEquals(getTask.id, tasks1.get(0).id);
    }

    
    public void testNodeRef()
    {
        WorkflowDefinition workflowDef = getTestDefinition();
        Map<QName, Serializable> parameters = new HashMap<QName, Serializable>();
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "reviewer"), AuthenticationUtil.getAdminUserName());
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "testNode"), testNodeRef);
        parameters.put(QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package"), packageComponent.createPackage(null));
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, parameters);
        assertNotNull(path);
        List<WorkflowTask> tasks1 = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks1);
        assertEquals(1, tasks1.size());
        assertEquals(WorkflowTaskState.IN_PROGRESS, tasks1.get(0).state);
        WorkflowTask updatedTask = taskComponent.endTask(tasks1.get(0).id, null);
        assertNotNull(updatedTask);
    }        

    
    public void testScript()
        throws IOException
    {
        // deploy test script definition
        ClassPathResource processDef = new ClassPathResource("jbpmresources/test_script.xml");
        assertFalse(workflowComponent.isDefinitionDeployed(processDef.getInputStream(), MimetypeMap.MIMETYPE_XML));
        WorkflowDeployment deployment = workflowComponent.deployDefinition(processDef.getInputStream(), MimetypeMap.MIMETYPE_XML); 
        assertNotNull(deployment);
        
        WorkflowDefinition workflowDef = deployment.definition;
        Map<QName, Serializable> parameters = new HashMap<QName, Serializable>();
        parameters.put(QName.createQName(NamespaceService.DEFAULT_URI, "testNode"), testNodeRef);
        parameters.put(QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package"), packageComponent.createPackage(null));
        WorkflowPath path = workflowComponent.startWorkflow(workflowDef.id, parameters);
        assertNotNull(path);
        List<WorkflowTask> tasks1 = workflowComponent.getTasksForWorkflowPath(path.id);
        assertNotNull(tasks1);
        assertEquals(1, tasks1.size());
        assertEquals(WorkflowTaskState.IN_PROGRESS, tasks1.get(0).state);
        WorkflowTask updatedTask = taskComponent.endTask(tasks1.get(0).id, null);
        assertNotNull(updatedTask);
    }        
    
    
    /**
     * Locate the Test Workflow Definition
     * 
     * @return  workflow definition
     */
    private WorkflowDefinition getTestDefinition()
    {
        return testWorkflowDef;
    }
    

    /**
     * Filter task list by workflow instance
     * 
     * @param tasks
     * @param processInstanceId
     * @return
     */
    private List<WorkflowTask> filterTasksByWorkflowInstance(List<WorkflowTask> tasks, String workflowInstanceId)
    {
        List<WorkflowTask> filteredTasks = new ArrayList<WorkflowTask>();
        for (WorkflowTask task : tasks)
        {
            if (task.path.instance.id.equals(workflowInstanceId))
            {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }
    
}
