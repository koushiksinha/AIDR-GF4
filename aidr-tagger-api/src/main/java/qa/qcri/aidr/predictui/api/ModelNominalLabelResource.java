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
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import qa.qcri.aidr.predictui.dto.ModelNominalLabelDTO;
import qa.qcri.aidr.predictui.entities.ModelNominalLabel;
import qa.qcri.aidr.predictui.facade.ModelNominalLabelFacade;

/**
 * REST Web Service
 *
 * @author Imran
 */
@Path("/modelNominalLabel")
@Stateless
public class ModelNominalLabelResource {

    @Context
    private UriInfo context;
    @EJB
    private ModelNominalLabelFacade modelNominalLabel;

    public ModelNominalLabelResource() {
    }
    
    @GET
    @Produces("application/json")
    @Path("/all")
    public Response getAllModelNominalLabels() {
        List<ModelNominalLabel> modelNominalLabelList = modelNominalLabel.getAllModelNominalLabels();
        ResponseWrapper response = new ResponseWrapper();
        response.setMessage("SUCCESS");
        response.setModelNominalLabels(modelNominalLabelList);
        return Response.ok(response).build();
    }
    
    @GET
    @Produces("application/json")
    @Path("/{modelID}")
    public Response getAllModelNominalLabelsByModelID(@PathParam("modelID") int modelID) {
        List<ModelNominalLabelDTO> modelNominalLabelList = modelNominalLabel.getAllModelNominalLabelsByModelID(modelID);
        ResponseWrapper response = new ResponseWrapper();
        if (modelNominalLabelList != null){
        response.setModelNominalLabelsDTO(modelNominalLabelList);
        return Response.ok(response).build();
        }
        response.setMessage("no labels found for the given model-id.");
        return Response.ok(response).build();
    }
}
