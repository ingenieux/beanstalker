#set($symbol_pound='#')
        #set($symbol_dollar='$')
        #set($symbol_escape='\' )
        package ${package}.resources;

        import javax.ws.rs.*;

@Path("/health")
@Produces("application/json; charset=utf-8")
public class HealthCheckResource extends BaseResource {
    @GET
    @Path("/check")
    public String doCheck() throws Exception {
        return "ok";
    }
}
