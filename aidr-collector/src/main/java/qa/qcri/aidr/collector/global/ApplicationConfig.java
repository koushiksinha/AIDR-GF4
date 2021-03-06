/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.qcri.aidr.collector.global;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

/**
 *
 * @author Imran
 */
//@javax.ws.rs.ApplicationPath("webresources")
@ApplicationPath("/webresources")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        System.out.println("In ApplicationConfig: registering REST APIs");
    	Set<Class<?>> resources = new java.util.HashSet<Class<?>>();
        // following code can be used to customize Jersey 1.x JSON provider:
    	//resources.add(MOXyJsonProvider.class);	// default JSON provider in gf 4
    	resources.add(JacksonFeature.class);		// gf 3 way modified
    	/*
    	try {
            Class jacksonProvider = Class.forName("org.codehaus.jackson.jaxrs.JacksonJsonProvider");
            resources.add(jacksonProvider);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        */
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
    	resources.add(qa.qcri.aidr.collector.api.CollectorManageResource.class);
    	System.out.println("Added resource CollectionManageResource");
        resources.add(qa.qcri.aidr.collector.api.TwitterCollectorAPI.class);
        System.out.println("Added resource TwitterCollectorAPI");
        
    }
    
}
