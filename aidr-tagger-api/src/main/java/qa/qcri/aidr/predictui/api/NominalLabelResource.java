/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.qcri.aidr.predictui.api;

import java.util.List;
import qa.qcri.aidr.predictui.util.ResponseWrapper;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import qa.qcri.aidr.predictui.dto.NominalLabelDTO;
import qa.qcri.aidr.predictui.entities.NominalLabel;
import qa.qcri.aidr.predictui.facade.NominalLabelResourceFacade;

/**
 * REST Web Service
 *
 * @author Imran
 */
@Path("/label")
@Stateless
public class NominalLabelResource {

    @Context
    private UriInfo context;
    @EJB
    private NominalLabelResourceFacade labelLocal;

    public NominalLabelResource() {
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/all")
    public Response getAllNominalLabels() {
        List<NominalLabel> labelList = labelLocal.getAllNominalLabel();
        ResponseWrapper response = new ResponseWrapper();
        response.setNominalLabels(labelList);
        return Response.ok(response).build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response getNominalLabelByID(@PathParam("id") int labelID) {
        NominalLabel label = labelLocal.getNominalLabelByID(labelID);
        if (label == null)
            return Response.noContent().build();
        
        return Response.ok(label).build();
    }

    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addLabel(NominalLabelDTO label){
     
        NominalLabel newLabel =  labelLocal.addNominalLabel(label);
        return Response.ok(newLabel).build();
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editLabel(NominalLabelDTO label){
        NominalLabel newLabel =  labelLocal.editNominalLabel(label);
        return Response.ok(newLabel).build();
    }
    
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLabel(@PathParam("id") int labelID){
        try{
        labelLocal.deleteNominalLabel(labelID);
        }catch(RuntimeException e){
            return Response.ok("No label found with the given ID").build();
        }
        return Response.ok("Label deleted").build();
    }
    
}
