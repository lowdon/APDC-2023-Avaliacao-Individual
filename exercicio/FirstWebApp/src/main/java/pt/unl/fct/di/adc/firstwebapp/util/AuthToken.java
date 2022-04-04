package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

    public static final long EXPIRATION_TIME = 1000*60*60*2; //2h
    public String user_name;
    public long user_role;
    public String tokenID;
    public long creation_date;
    public long expiration_date;

    public AuthToken(String username, long user_role) {
        this.user_name = username;
        this.user_role = user_role;
        this.tokenID = UUID.randomUUID().toString();
        this.creation_date = System.currentTimeMillis();
        this.expiration_date = this.creation_date + AuthToken.EXPIRATION_TIME;
    }

    public AuthToken(String username, long user_role, String tokenID, long creation_date, long expiration_date) {
        this(username, user_role);
        this.tokenID = tokenID;
        this.creation_date = creation_date;
        this.expiration_date = expiration_date;
    }

}