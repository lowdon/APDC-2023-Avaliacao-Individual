package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.PwdChangeData;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    public static final String USER_TOKEN = "user_token";
    public static final String NO_ACCESS = "NO ACCESS";
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private final Gson g = new Gson();

    // Instantiates a client
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    // Com Kind mas identificador gerado automaticatemente
    KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    //Map<String, AuthToken> validTokens = new HashMap<>();

    KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    public LoginResource() {
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doRegistration(LoginData data) {
        LOG.fine("Registration attempt by user: " + data.user_name);

        if (!data.isValid())
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing parameters.").build();

        Transaction txn = datastore.newTransaction();

        try {

            Key userKey = userKeyFactory.newKey(data.getUser_name());
            Entity user = txn.get(userKey);


            if (user != null) {
                txn.rollback();
                return Response.status(Response.Status.BAD_REQUEST).entity("User already exists.").build();
            }


            Entity.Builder pre = Entity.newBuilder(userKey)
                    .set("user_name", data.user_name)
                    .set("user_pwd", data.user_pwd)
                    .set("user_email", data.user_email)
                    .set("user_creation_time", Timestamp.now())
                    .set("user_lastmodified_time", Timestamp.now());

            try{ // USER ROLE IS OPTIONAL, GOOD FOR DEBUG
                pre.set("user_role", data.user_role);
            } catch(NullPointerException e){
                LOG.warning("USER ROLE NOT DEFINED, SETTING TO 0");
                pre.set("user_role", 0);
            }

            user = pre.build();

            txn.add(user);
            LOG.warning("USER REGISTERED: " + data.user_name);
            txn.commit();
            return Response.ok("User " + data.user_name + " registered.").build();

        } finally {
            if( txn.isActive() )
                txn.rollback();
        }
    }


    @GET
    @Path("/getUser/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("username") String username) {

        try{
            Entity userEnt = datastore.get(userKeyFactory.newKey(username));
            return Response.ok(userEnt).build();
        } catch (Exception e){
            LOG.warning("NO USER");
            return Response.status(Response.Status.BAD_REQUEST).entity("NO SUCH USER").build();
        }

    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
        LOG.fine("Login attempt by user: " + data.user_name);

        Key userKey = userKeyFactory.newKey(data.getUser_name());

        Entity user = datastore.get(userKey);


        if( user != null ) {
            String hashedPWD = DigestUtils.sha512Hex(user.getString("user_pwd"));
            if(hashedPWD.equals(DigestUtils.sha512Hex(data.user_pwd))){
                AuthToken token = new AuthToken(data.user_name, data.user_role);
                LOG.info("USER " + data.user_name + " LOGIN SUCCESSFUL");

                Transaction txn = datastore.newTransaction();

                try{

                    Key tokenKey = tokenKeyFactory.newKey(token.tokenID);
                    Entity tokenEnt = txn.get(tokenKey);

                    if (tokenEnt != null) {
                        txn.rollback();
                        return Response.status(Response.Status.BAD_REQUEST).entity("Token already exists.").build();
                    }

                    tokenEnt = Entity.newBuilder(tokenKey)
                            .set("user_name", token.user_name)
                            .set("user_role", token.user_role)
                            .set("tokenId", token.tokenID)
                            .set("creation_date", token.creation_date)
                            .set("expiration_date", token.expiration_date)
                            .build();

                    txn.add(tokenEnt);
                    LOG.warning("TOKEN REGISTERED: " + token.tokenID);
                    txn.commit();
                    //validTokens.put(token.tokenID, token);
                    return Response.ok(g.toJson(token)).build();

                } finally {
                    if( txn.isActive() )
                        txn.rollback();
                }
            }
            LOG.warning("WRONG PASSWORD");
        }

        LOG.warning("FAILED LOGIN ATTEMPT USER: " + data.user_name);
        return Response.status(Response.Status.FORBIDDEN).entity("WRONG PASSWORD").build();

    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogout(@Context HttpHeaders headers) {
        String tokenId = headers.getHeaderString(USER_TOKEN);
        Key tokenKey = tokenKeyFactory.newKey(tokenId);

        LOG.fine("Logout attempt by token: " + tokenId);

        if(this.datastore.get(tokenKey) != null){
            this.datastore.delete(tokenKey);
            LOG.info("USER LOGOUT SUCCESSFUL");
            return Response.ok("LOGOUT SUCCESSFULL TOKEN: " + headers.getHeaderString(USER_TOKEN)).build();
        }

        LOG.warning("FAILED LOGOUT ATTEMPT NO SUCH TOKEN: " + tokenId);
        return Response.status(Response.Status.FORBIDDEN)
                .entity("FAILED LOGOUT ATTEMPT NO SUCH TOKEN: " + tokenId).build();

    }



    @DELETE
    @Path("/deleteUser/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("username") String nameToBeRemoved, @Context HttpHeaders headers){
        String authTokenString = headers.getHeaderString(USER_TOKEN);

        //CHECK IF IS LOGGED IN AKA AUTH VALID
        Entity authorToken = this.datastore.get(tokenKeyFactory.newKey(authTokenString));//validTokens.get(authTokenString);
        if(authorToken != null){
            Entity author = this.datastore.get(userKeyFactory.newKey(authorToken.getString("user_name")));
            Key keyToBeRemoved = this.userKeyFactory.newKey(nameToBeRemoved);
            Entity toBeRemoved = this.datastore.get(keyToBeRemoved);

            if( (author.getLong("user_role")) >= (toBeRemoved.getLong("user_role")) ){
                //this.users.remove(nameToBeRemoved);
                this.datastore.delete(keyToBeRemoved);
                return Response.ok("Deleted successfully userID: " + nameToBeRemoved).build();
            }
            else{
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Current logged in user does not have enough role privileges.").build();
            }

        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Delete user failed.").build();
    }

    @GET
    @Path("/getTokenInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTokenInfo(@Context HttpHeaders headers){

        String authTokenString = headers.getHeaderString(USER_TOKEN);
        LOG.info("GET TOKEN INFO REQUESTED BY TOKEN: " + authTokenString);

        //CHECK IF IS LOGGED IN AKA AUTH VALID
        Entity authorToken = this.datastore.get(tokenKeyFactory.newKey(authTokenString));
        if(authorToken != null) {
            AuthToken token = new AuthToken(
                    authorToken.getString("user_name"),
                    authorToken.getLong("user_role"),
                    authorToken.getString("tokenId"),
                    authorToken.getLong("creation_date"),
                    authorToken.getLong("expiration_date")
                    );
            return Response.ok(g.toJson(token)).build();
        }

        LOG.info("NO TOKEN INFO");
        return Response.status(Response.Status.BAD_REQUEST).entity("NO TOKEN INFO, NO SUCH TOKEN").build();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(@Context HttpHeaders headers){

        String authTokenString = headers.getHeaderString(USER_TOKEN);
        LOG.info("LIST USERS REQUESTED BY TOKEN: " + authTokenString);

        //CHECK IF IS LOGGED IN AKA AUTH VALID
        Entity authorToken = this.datastore.get(tokenKeyFactory.newKey(authTokenString));//validTokens.get(authTokenString);
        if(authorToken != null) {
            Entity author = this.datastore.get(userKeyFactory.newKey(authorToken.getString("user_name")));
            Query<Entity> q = Query.newEntityQueryBuilder().setKind("User")
                        .setFilter(
                                StructuredQuery.PropertyFilter.le(
                                        "user_role",
                                        author.getLong("user_role"))
                        )
                        .build();

            QueryResults<Entity> results = datastore.run(q);

            StringBuilder resultsToJSON = new StringBuilder();

            boolean isAuthorNormalUser = author.getLong("user_role") == 0;

            while(results.hasNext()){
                Entity currentEntry = results.next();
                if(isAuthorNormalUser &&
                        currentEntry.getString("state").equals("active") &&
                        !currentEntry.getBoolean("isPrivate") ){ // REMOVES ACCESS TO CERTAIN ENTRIES IN RESPONSE

                    currentEntry = Entity.newBuilder(currentEntry.getKey())
                            .set("user_name", currentEntry.getString("user_name")) // DO NOT ALLOW TO CHANGE USERNAME
                            .set("user_pwd", NO_ACCESS)
                            .set("user_email", currentEntry.getString("user_email"))
                            .set("user_creation_time", currentEntry.getTimestamp("user_creation_time"))
                            .set("user_lastmodified_time", currentEntry.getTimestamp("user_lastmodified_time"))
                            .set("user_role", NO_ACCESS)
                            .set("isPrivate", NO_ACCESS)
                            .set("phoneNumber", NO_ACCESS)
                            .set("cellPhoneNumber", NO_ACCESS)
                            .set("address", NO_ACCESS)
                            .set("nif", NO_ACCESS)
                            .set("state", currentEntry.getString("state"))
                            .build();// UPDATE ENTRY
                }
                resultsToJSON.append(g.toJson(currentEntry)).append("\n");
            }

            return Response.ok(resultsToJSON.toString()).build();

        }

        LOG.info("LIST USERS SUCCESSFUL");
        return Response.status(Response.Status.BAD_REQUEST).entity("NO USERS AVAILABLE").build();
    }

    @PUT
    @Path("/updateUser/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(LoginData data,
            @PathParam("username") String userToBeChanged, @Context HttpHeaders headers){
        String authTokenString = headers.getHeaderString(USER_TOKEN);

        //CHECK IF IS LOGGED IN AKA AUTH VALID
        Entity authorToken = this.datastore.get(tokenKeyFactory.newKey(authTokenString));//validTokens.get(authTokenString);
        if(authorToken != null){
            Entity author = this.datastore.get(userKeyFactory.newKey(authorToken.getString("user_name")));
            Key keyToBeChanged = this.userKeyFactory.newKey(userToBeChanged);
            Entity toBeChanged = this.datastore.get(keyToBeChanged); // USER MUST EXIST IN ORDER TO BE UPDATED

            if( toBeChanged != null &&
                    (author.getLong("user_role")) > (toBeChanged.getLong("user_role")) ){
                //this.users.remove(nameToBeRemoved);
                Entity.Builder updatedUser = Entity.newBuilder(keyToBeChanged)
                        .set("user_name", toBeChanged.getString("user_name")) // DO NOT ALLOW TO CHANGE USERNAME
                        .set("user_pwd", data.user_pwd)
                        .set("user_email", data.user_email)
                        .set("user_creation_time", toBeChanged.getTimestamp("user_creation_time"))
                        .set("user_lastmodified_time", Timestamp.now())
                        .set("user_role", toBeChanged.getLong("user_role"))
                        .set("isPrivate", data.isPrivate)
                        .set("phoneNumber", data.phoneNumber)
                        .set("cellPhoneNumber", data.cellPhoneNumber)
                        .set("address", data.address)
                        .set("nif", data.nif)
                        .set("state", data.state);
                        //.build();
                if(author.getLong("user_role") == 3){// IF IS SUPERUSER, LET CHANGE IN ROLE; ELSE NO CHANGE
                    updatedUser.set("user_role", data.user_role);
                }
                this.datastore.put(updatedUser.build());
                return Response.ok("Updated successfully userID: " + userToBeChanged).build();
            }
            else{
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Current logged in user does not have enough role privileges.").build();
            }

        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Update user failed.").build();
    }

    @PUT
    @Path("/activateUser/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateUser(@PathParam("username") String userToBeChanged, @Context HttpHeaders headers){
        String authTokenString = headers.getHeaderString(USER_TOKEN);

        //CHECK IF IS LOGGED IN AKA AUTH VALID
        Entity authorToken = this.datastore.get(tokenKeyFactory.newKey(authTokenString));//validTokens.get(authTokenString);
        if(authorToken != null){
            Entity author = this.datastore.get(userKeyFactory.newKey(authorToken.getString("user_name")));
            Key keyToBeChanged = this.userKeyFactory.newKey(userToBeChanged);
            Entity toBeChanged = this.datastore.get(keyToBeChanged); // USER MUST EXIST IN ORDER TO BE UPDATED

            if( toBeChanged != null &&
                    (author.getLong("user_role")) >= (toBeChanged.getLong("user_role")) ){
                //this.users.remove(nameToBeRemoved);
                Entity updatedUser = Entity.newBuilder(keyToBeChanged)
                        .set("user_name", toBeChanged.getString("user_name")) // DO NOT ALLOW TO CHANGE USERNAME
                        .set("user_pwd", toBeChanged.getString("user_pwd"))
                        .set("user_email", toBeChanged.getString("user_email"))
                        .set("user_creation_time", toBeChanged.getTimestamp("user_creation_time"))
                        .set("user_lastmodified_time", toBeChanged.getTimestamp("user_lastmodified_time"))
                        .set("user_role", toBeChanged.getLong("user_role"))
                        .set("isPrivate", toBeChanged.getBoolean("isPrivate"))
                        .set("phoneNumber", toBeChanged.getString("phoneNumber"))
                        .set("cellPhoneNumber", toBeChanged.getString("cellPhoneNumber"))
                        .set("address", toBeChanged.getString("address"))
                        .set("nif", toBeChanged.getString("nif"))

                        // SET TO ACTIVE IF INACTIVE AND VICE VERSA
                        .set("state", toBeChanged.getString("state").equals("inactive") ? "active" : "inactive")
                        .build();

                this.datastore.put(updatedUser);
                return Response.ok("Activated/Deactivated successfully userID: " + userToBeChanged +
                        "\n New state: " + updatedUser.getString("state")).build();
            }
            else{
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Current logged in user does not have enough role privileges.").build();
            }

        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Update user failed.").build();
    }

    @PUT
    @Path("/changePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(PwdChangeData data, @Context HttpHeaders headers){
        String authTokenString = headers.getHeaderString(USER_TOKEN);

        //CHECK IF IS LOGGED IN AKA AUTH VALID
        Entity authorToken = this.datastore.get(tokenKeyFactory.newKey(authTokenString));//validTokens.get(authTokenString);
        if(authorToken != null){
            Entity author = this.datastore.get(userKeyFactory.newKey(authorToken.getString("user_name")));

            if( author != null ){
                if(author.getString("user_pwd").equals(data.old_pwd) &&
                data.new_pwd.equals(data.new_pwd_conf) ) {
                    //this.users.remove(nameToBeRemoved);
                    Entity updatedUser = Entity.newBuilder(author.getKey())
                            .set("user_name", author.getString("user_name")) // DO NOT ALLOW TO CHANGE USERNAME
                            .set("user_pwd", data.new_pwd)
                            .set("user_email", author.getString("user_email"))
                            .set("user_creation_time", author.getTimestamp("user_creation_time"))
                            .set("user_lastmodified_time", author.getTimestamp("user_lastmodified_time"))
                            .set("user_role", author.getLong("user_role"))
                            .set("isPrivate", author.getBoolean("isPrivate"))
                            .set("phoneNumber", author.getString("phoneNumber"))
                            .set("cellPhoneNumber", author.getString("cellPhoneNumber"))
                            .set("address", author.getString("address"))
                            .set("nif", author.getString("nif"))
                            .set("state", author.getString("state"))
                            .build();

                    this.datastore.put(updatedUser);
                    return Response.ok("Changed password successfully userID: " + author.getString("user_name") +
                            "\n New password: " + updatedUser.getString("user_pwd")).build();
                }
            }

        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Change user password failed.").build();
    }

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(){
        return Response.ok("It's working!").build();
    }
}
