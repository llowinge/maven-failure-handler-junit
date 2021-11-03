package org.jboss.fuse.maven;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

@Component(role = EventSpy.class)
public class FailureHandlerJunit extends AbstractEventSpy {

    @Requirement
    private Logger logger;

    @Override
    public void init(Context context) throws Exception {
        logger.info("Maven failure handler junit extension loaded.");
        super.init(context);
    }

    @Override
    public void onEvent(Object event) throws Exception {
        ExecutionEvent executionEvent = null;
        if (event instanceof ExecutionEvent &&
                (executionEvent = (ExecutionEvent) event).getType() == ExecutionEvent.Type.ProjectFailed) {
            final MavenProject project = executionEvent.getSession().getCurrentProject();
            final String targetDirectory = project.getModel().getBuild().getDirectory();
            final File surefireReportsFile = new File(targetDirectory, "surefire-reports");
            final File failsafeReportsFile = new File(targetDirectory, "failsafe-reports");
            if (!surefireReportsFile.exists() && !failsafeReportsFile.exists()) {
                final Exception exception = executionEvent.getException();
                surefireReportsFile.mkdirs();
                createJunitXml(project.getGroupId(), project.getArtifactId(),
                        ExceptionUtils.getRootCause(exception).getClass().getName(),
                        exception.getMessage(), ExceptionUtils.getStackTrace(exception), surefireReportsFile);
            }
        }
    }

    private void createJunitXml(String groupId, String artifactId, String errorType, String errorMessage,
            String errorStacktrace, File folder) {
        try {
            final String fullName = groupId + ".modules." + artifactId;
            final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            final Document doc = docBuilder.newDocument();
            final Element rootElement = doc.createElement("testsuite");
            rootElement.setAttribute("name", fullName);
            rootElement.setAttribute("tests", "1");
            rootElement.setAttribute("errors", "1");
            rootElement.setAttribute("skipped", "0");
            rootElement.setAttribute("failures", "0");
            doc.appendChild(rootElement);
            final Element testcase = doc.createElement("testcase");
            testcase.setAttribute("classname", fullName);
            testcase.setAttribute("name", "failedMavenPhase");
            final Element error = doc.createElement("error");
            error.setAttribute("message", errorMessage);
            error.setAttribute("type", errorType);
            error.appendChild(doc.createTextNode(errorStacktrace));
            testcase.appendChild(error);
            rootElement.appendChild(testcase);

            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            final DOMSource source = new DOMSource(doc);
            final StreamResult result = new StreamResult(
                    new File(folder, String.format("TEST-%s.xml", fullName)));
            transformer.transform(source, result);
        } catch (Exception exception) {
            logger.error("Failed to create XML report.", exception);
        }
    }

}
