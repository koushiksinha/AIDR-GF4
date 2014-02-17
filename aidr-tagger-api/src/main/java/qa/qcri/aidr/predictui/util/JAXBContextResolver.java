package qa.qcri.aidr.predictui.util;

//import com.sun.jersey.api.json.JSONConfiguration;
//import com.sun.jersey.api.json.JSONJAXBContext;

import qa.qcri.aidr.predictui.entities.NominalAttribute;

import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;
import javax.ws.rs.ext.Provider;

/*
//   gf 3 way - modified attempt for gf4
@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

    private JAXBContext context;

    private final Class[] types = {
            ResponseWrapper.class,
            NominalAttribute.class
    };

    public JAXBContextResolver() throws Exception {
        this.context = new JSONJAXBContext(JSONConfiguration.natural().build(), types);
    }

    @Override
    public JAXBContext getContext(final Class objectType) {
        for (Class type : types) {
            if (type == objectType) {
                return context;
            }
        }
        return null;
    }

}
*/


@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

    private JAXBContext context;

    private final Class[] types = {
            ResponseWrapper.class,
            NominalAttribute.class
    };

    public JAXBContextResolver() throws Exception {
        //this.context = new JAXBContext(JSONConfiguration.natural().build(), types);
    	this.context = JAXBContext.newInstance(ResponseWrapper.class, NominalAttribute.class);
    }

    @Override
    public JAXBContext getContext(final Class objectType) {
        for (Class type : types) {
            if (type == objectType) {
                return context;
            }
        }
        return null;
    }

}
