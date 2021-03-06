package org.jbpm;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.test.JbpmJUnitTestCase;
import org.junit.Test;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.task.api.TaskService;
import org.kie.internal.task.api.model.Status;
import org.kie.internal.task.api.model.TaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample file to test a process.
 */
public class ProcessPersistenceHumanTaskOnLaneTest extends JbpmJUnitTestCase {

    private Logger testLogger = LoggerFactory.getLogger(ProcessPersistenceHumanTaskOnLaneTest.class);

    public ProcessPersistenceHumanTaskOnLaneTest() {
        super(true);
        setPersistence(true);
    }

    @Test 
    public void testProcess() throws Exception {
        StatefulKnowledgeSession ksession = createKnowledgeSession("HumanTaskOnLane.bpmn2");
        TaskService taskService = getTaskService(ksession);

        ProcessInstance processInstance = ksession.startProcess("UserTask");

        assertProcessInstanceActive(processInstance.getId(), ksession);
        

        // simulating a system restart
        ksession = restoreSession(ksession, true);
        taskService = getTaskService(ksession);

        // let john execute Task 1
        String taskUser = "john";
        String locale = "en-UK";
        List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner(taskUser, locale);
        assertEquals(1, list.size());
        
        TaskSummary task = list.get(0);
        taskService.claim(task.getId(), taskUser);
        taskService.start(task.getId(), taskUser);
        taskService.complete(task.getId(), taskUser, null);



        // simulating a system restart
        ksession = restoreSession(ksession, true);
        taskService = getTaskService(ksession);
        List<Status> reservedOnly = new ArrayList<Status>();
        reservedOnly.add(Status.Reserved);

        
        list = taskService.getTasksAssignedAsPotentialOwnerByStatus(taskUser, reservedOnly, locale);
        assertEquals(1, list.size());
        
        task = list.get(0);
        taskService.start(task.getId(), taskUser);
        taskService.complete(task.getId(), taskUser, null);


        assertProcessInstanceCompleted(processInstance.getId(), ksession);
    }


}
