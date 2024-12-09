package com.eziosoft.verandagal.server.utils;

import com.eziosoft.verandagal.Main;
import com.eziosoft.verandagal.server.objects.SessionObject;
import jakarta.servlet.http.HttpSession;

public class SessionUtils {

    private static final String attr_name = "cont";

    public static SessionObject getSessionDetails(HttpSession session){
        // attempt to load the details from our session
        String content = (String) session.getAttribute(attr_name);
        // check if null
        if (content == null){
            // make a new session
            SessionObject pain = new SessionObject();
            // set the defaults
            pain.setDefaults();
            // store it
            session.setAttribute(attr_name, Main.gson_pretty.toJson(pain));
            // then, return it
            return pain;
        }
        // otherwise, do stuff with it
        // get the stored json text
        String json = (String) session.getAttribute(attr_name);
        // return the object
        return Main.gson_pretty.fromJson(json, SessionObject.class);
    }

    /**
     * update the stored session object for whatever user is currently visiting the webpage
     * @param session session to update
     * @param obj content you wish to updaste the session with
     */
    public static void updateSessionDetails(HttpSession session, SessionObject obj){
        // get the session details as a string
        String cont = Main.gson_pretty.toJson(obj);
        // write to session
        session.setAttribute(attr_name, cont);
        // and we're done.
    }
}
