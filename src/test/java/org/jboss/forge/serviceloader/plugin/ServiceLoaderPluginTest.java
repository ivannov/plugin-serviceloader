package org.jboss.forge.serviceloader.plugin;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Scanner;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.UnknownFileResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.util.OSUtils;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;

public class ServiceLoaderPluginTest extends AbstractShellTest {

    private Project project;
    private DirectoryResource servicesDirectory;
    private DirectoryResource sourceDirectory;
    
    @Before
    public void setup() throws Exception {
        project = initializeJavaProject();
        servicesDirectory = getServicesDirectory();
        sourceDirectory = getSourceDirectory();
    }

    @Deployment
    public static JavaArchive getDeployment() {
        return AbstractShellTest.getDeployment().addPackages(true, ServiceLoaderPlugin.class.getPackage());
    }

    @Test
    public void testNewClassCommand() throws Exception {
        createJavaClass("com.foo.BarImpl");
        getShell().execute("serviceloader new-class --interface com.foo.Bar --implementation com.foo.BarImpl");
        assertNotNull(servicesDirectory);
        assertTrue(servicesDirectory.isDirectory());
        UnknownFileResource interfaceFile = servicesDirectory.getChildOfType(UnknownFileResource.class,
                "com.foo.Bar");
        assertTrue(interfaceFile.exists());
        assertFalse(interfaceFile.isDirectory());
        String implementation = getImplementations(interfaceFile);
        assertEquals("com.foo.BarImpl", implementation);
    }
    
    @Test
    public void testNewClassCommandWithExistingServiceFile() throws Exception {
        createJavaClass("com.foo.BarImpl1");
        createJavaClass("com.foo.BarImpl2");
        getShell().execute("serviceloader new-class --interface com.foo.Bar --implementation com.foo.BarImpl1");
        getShell().execute("serviceloader new-class --interface com.foo.Bar --implementation com.foo.BarImpl2");
        UnknownFileResource interfaceFile = servicesDirectory.getChildOfType(UnknownFileResource.class, "com.foo.Bar");
        String implementation = getImplementations(interfaceFile);
        assertEquals("com.foo.BarImpl1" + OSUtils.getLineSeparator() + "com.foo.BarImpl2", implementation);        
    }

    @Test
    public void testNewClassCommandChecksImplementationExistence() throws Exception {
        createJavaClass("com.foo.BarImpl1");
        getShell().execute("serviceloader new-class --interface com.foo.Bar --implementation com.foo.BarImpl1");
        getShell().execute("serviceloader new-class --interface com.foo.Bar --implementation com.foo.BarImpl2");
        UnknownFileResource interfaceFile = servicesDirectory.getChildOfType(UnknownFileResource.class, "com.foo.Bar");
        String implementation = getImplementations(interfaceFile);
        assertEquals("com.foo.BarImpl1", implementation);        
    }
    
    @Test
    public void testNewClassCommandsDeclaresImplementationJustOnce() throws Exception {
        createJavaClass("com.foo.BarImpl");
        getShell().execute("serviceloader new-class --interface com.foo.Bar --implementation com.foo.BarImpl");
        getShell().execute("serviceloader new-class --interface com.foo.Bar --implementation com.foo.BarImpl");
        UnknownFileResource interfaceFile = servicesDirectory.getChildOfType(UnknownFileResource.class, "com.foo.Bar");
        String implementation = getImplementations(interfaceFile);
        assertEquals("com.foo.BarImpl", implementation);        
    }
    
    private DirectoryResource getSourceDirectory() {
        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
        return facet.getSourceFolder();
    }
    
    private DirectoryResource getServicesDirectory() {
        ResourceFacet facet = project.getFacet(ResourceFacet.class);
        DirectoryResource servicesDirectory = facet.getResourceFolder().getChildDirectory("META-INF/services");
        return servicesDirectory;
    }
    
    private void createJavaClass(String fullyQualifiedName) {
        sourceDirectory.getChildOfType(JavaResource.class, fullyQualifiedName.replace(".", "/") + ".java").createNewFile();
    }

    private String getImplementations(UnknownFileResource interfaceFile) throws IOException {
        Scanner scanner = new Scanner(interfaceFile.getUnderlyingResourceObject());        
        StringBuilder implementations = new StringBuilder();

        while (scanner.hasNextLine()) {
            implementations.append(scanner.nextLine());
            if (scanner.hasNextLine()) {
                implementations.append(OSUtils.getLineSeparator());
            }
        }
        scanner.close();

        return implementations.toString();
    }

}
