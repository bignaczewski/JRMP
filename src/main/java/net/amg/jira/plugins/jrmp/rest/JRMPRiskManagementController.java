/*Copyright 2015 AMG.net

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package net.amg.jira.plugins.jrmp.rest;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.gson.Gson;
import net.amg.jira.plugins.jrmp.rest.model.ErrorCollection;
import net.amg.jira.plugins.jrmp.rest.model.GadgetFieldEnum;
import net.amg.jira.plugins.jrmp.rest.model.MatrixRequest;
import net.amg.jira.plugins.jrmp.services.MatrixGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Controller used for validations and other useful things
 *
 * @author Adam Król
 */
@Path("/controller")
@Controller
public class JRMPRiskManagementController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private I18nResolver i18nResolver;
    @Autowired
    private SearchService searchService;
    @Autowired
    private JiraAuthenticationContext authenticationContext;
    @Autowired
    private MatrixGenerator matrixGenerator;
    //4spring dep.injection
    public JRMPRiskManagementController(){}


    @Path("/validate")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response doValidation(@Context HttpServletRequest request) {

        logger.info("Validation: Method start");
        String filter = request.getParameter(GadgetFieldEnum.FILTER.toString());
        String relativeDate = request.getParameter(GadgetFieldEnum.DATE.toString());
        String title = request.getParameter(GadgetFieldEnum.TITLE.toString());
        String refreshRate = request.getParameter(GadgetFieldEnum.REFRESH.toString());
        String template = request.getParameter(GadgetFieldEnum.TEMPLATE.toString());

        MatrixRequest matrixRequest = new MatrixRequest();
        matrixRequest.setTitle(title);
        matrixRequest.setDate(relativeDate);
        matrixRequest.setTemplate(template);
        matrixRequest.setFilter(filter);
        matrixRequest.setRefreshRate(refreshRate);

        Gson gson = new Gson();

        ErrorCollection errorCollection = matrixRequest.doValidation(i18nResolver,authenticationContext,searchService);
        errorCollection.setParameters(request.getParameterMap());

        if(errorCollection.hasAnyErrors()) {
            logger.warn("Validation: Wrong parameters passed to Validator. Returning BAD_REQUEST");
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(errorCollection)).build();
        }
        logger.info("Validation: Everything Went okay, returning OK.");
        return Response.ok().build();
    }

    @Path("/matrix")
    @POST
    @Produces({MediaType.TEXT_HTML})
    @Consumes("application/json")
    public Response getMatrix(MatrixRequest matrixRequest) {

        logger.debug("getMatrix: Method start");

        ErrorCollection errorCollection = matrixRequest.doValidation(i18nResolver,authenticationContext,searchService);
        if(!errorCollection.hasAnyErrors()){

            try {
                return Response.ok(matrixGenerator.generateMatrix(matrixRequest.getProjectOrFilter(),
                        matrixRequest.getTitle(),
                        matrixRequest.getTemplate(),
                        matrixRequest.getDateModel()),
                        MediaType.TEXT_HTML).build();
            } catch(Exception e){
                logger.error("getMatrix: The Matrix couldn't be generated because of: " + e.getMessage(),e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }else{
            logger.warn("getMatrix: Wrong parameters passed in matrixRequest. Returning BAD_REQUEST");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }


    }

    @GET
    @Path("{args : (.*)?}")
    public Response emptyGETResponse()
    {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("{args : (.*)?}")
    public Response emptyPOSTResponse()
    {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("{args : (.*)?}")
    public Response emptyPUTResponse()
    {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @DELETE
    @Path("{args : (.*)?}")
    public Response emptyDELETEResponse()
    {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

}
