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
public class ProjectPhaseFailureDetector extends AbstractEventSpy {

    @Requirement
    private Logger logger;

    static {
        System.out.println("Maven phase detector extension loaded.");
    }

    @Override
    public void onEvent(Object event) throws Exception {
        ExecutionEvent executionEvent = null;
        if (event instanceof ExecutionEvent &&
                (executionEvent = (ExecutionEvent) event).getType() == ExecutionEvent.Type.ProjectFailed) {
            final MavenProject project = executionEvent.getSession().getCurrentProject();
            final String targetDirectory = project.getModel().getBuild().getDirectory();
            if (!new File(targetDirectory, "surefire-reports").exists() &&
                    !new File(targetDirectory, "failsafe-reports").exists()) {
                final Exception exception = executionEvent.getException();
                final File fakeSureFireReports = new File(targetDirectory, "surefire-reports");
                fakeSureFireReports.mkdirs();
                createJunitXml(project.getArtifactId(), ExceptionUtils.getRootCause(exception).getClass().getName(),
                        exception.getMessage(), fakeSureFireReports);
            }
        }
    }

    private void createJunitXml(String artifactId, String errorType, String message, File folder) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("testsuite");
            doc.appendChild(rootElement);
            Element testcase = doc.createElement("testcase");
            testcase.setAttribute("classname", getClass().toString());
            testcase.setAttribute("name", "ProjectPhaseFailed-" + artifactId);
            Element failure = doc.createElement("failure");
            failure.setAttribute("type", errorType);
            failure.appendChild(doc.createTextNode(message));
            testcase.appendChild(failure);
            rootElement.appendChild(testcase);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(
                    new File(folder, String.format("%s-%s.xml", getClass(), artifactId)));
            transformer.transform(source, result);
        } catch (Exception exception) {
            logger.error("Failed to create XML report.", exception);
        }
    }

}
