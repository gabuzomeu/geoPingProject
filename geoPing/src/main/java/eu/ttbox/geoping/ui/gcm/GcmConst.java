package eu.ttbox.geoping.ui.gcm;


public class GcmConst {

    /**
     * Set Project ID of your Google APIs Console Project.
     */
    public static final String PROJECT_ID = "geoping-cloud";

    /**
     * Set Project Number of your Google APIs Console Project.
     */
    public static final String PROJECT_NUMBER = "357142520111";

    /**
     * Set your Web Client ID for authentication at backend.
     */
    public static final String WEB_CLIENT_ID = "357142520111-et6rh3j3dfa2in2eii860l05vf94keu8.apps.googleusercontent.com";

    /**
     * Set default user authentication enabled or disabled.
     */
    public static final boolean IS_AUTH_ENABLED = true;

    /**
     * Auth audience for authentication on backend.
     */
    public static final String AUTH_AUDIENCE = "server:client_id:" + WEB_CLIENT_ID;

    /**
     * Endpoint root URL
     */
    public static final String ENDPOINT_ROOT_URL = "https://" + PROJECT_ID + ".appspot.com/_ah/api/";


}
