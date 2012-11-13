package org.jboss.forge.serviceloader.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javax.inject.Inject;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.UnknownFileResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.util.OSUtils;

/**
 * Plugin for creating a new ServiceLoader service registration in META-INF/services, based on an interface and class
 * implementation
 * 
 * @author <a href="mailto:ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
@Alias("serviceloader")
@RequiresProject
public class ServiceLoaderPlugin implements Plugin {

    private static final String SERVICES_DIRECTORY = "META-INF/services";

    @Inject
    private Project project;

    @Command("new-class")
    public void newClassCommand(@Option(name = "interface", shortName = "i") String interfaceName,
            @Option(name = "implementation", shortName = "m", type = PromptType.JAVA_CLASS) String implName, PipeOut out) {

        ResourceFacet resourcesFacet = project.getFacet(ResourceFacet.class);
        JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);

        try {
            JavaResource implementationFile = javaSourceFacet.getJavaResource(implName);
            if (!implementationFile.exists()) {
                out.println("The implementation class " + implName
                        + " does not exist in the current project. You can declare only existing implementations of "
                        + interfaceName);
                return;
            }
        } catch (FileNotFoundException fnfe) {
            out.println("The implementation class " + implName
                    + " does not exist in the current project. You can declare only existing implementations of "
                    + interfaceName);
            return;
        }

        DirectoryResource servicesDirectory = getServicesDirectory(resourcesFacet);
        UnknownFileResource implementationsFile = getImplementationsFile(interfaceName, servicesDirectory);

        try {
            String implementations = getImplementations(implementationsFile, implName) + implName;
            implementationsFile.setContents(implementations);
        } catch (IOException ioe) {
            out.println("Could not update " + implementationsFile + ". Reason: " + ioe.getMessage());
        } catch (ImplementationExistsException iee) {
            out.println("The implementation " + implName + " is already declared in " + implementationsFile + ".");
        }
    }

    private DirectoryResource getServicesDirectory(ResourceFacet resourcesFacet) {
        DirectoryResource servicesDirectory = resourcesFacet.getResourceFolder().getChildDirectory(SERVICES_DIRECTORY);
        if (!servicesDirectory.exists()) {
            servicesDirectory.mkdirs();
        }
        return servicesDirectory;
    }

    private UnknownFileResource getImplementationsFile(String interfaceName, DirectoryResource servicesDirectory) {
        UnknownFileResource interfaceFile = servicesDirectory.getChildOfType(UnknownFileResource.class, interfaceName);
        if (!interfaceFile.exists()) {
            interfaceFile.createNewFile();
        }
        return interfaceFile;
    }

    private String getImplementations(UnknownFileResource interfaceFile, String implToAdd) throws IOException {
        Scanner scanner = new Scanner(interfaceFile.getUnderlyingResourceObject());
        StringBuilder implementations = new StringBuilder();

        while (scanner.hasNextLine()) {
            String currentImpl = scanner.nextLine();
            if (implToAdd.equals(currentImpl)) {
                scanner.close();
                throw new ImplementationExistsException();
            }
            implementations.append(currentImpl + OSUtils.getLineSeparator());
        }
        scanner.close();

        return implementations.toString();
    }

    private static final class ImplementationExistsException extends RuntimeException {
        private static final long serialVersionUID = 3496863553252610689L;
    }
}
