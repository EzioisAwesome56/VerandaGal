package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.objects.SessionObject;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import com.eziosoft.verandagal.server.utils.SessionUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

public class SettingsPageServlet  extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get session
        HttpSession session = req.getSession();
        SessionObject obj = SessionUtils.getSessionDetails(session);
        // setup a bunch of booleans
        boolean normal = false, spicy = false, extraspicy = false, ai = false;
        for (Map.Entry<String, String[]> thing : req.getParameterMap().entrySet()){
            switch (thing.getKey()) {
                case "normal" -> {
                    normal = true;
                    continue;
                }
                case "spicy" -> {
                    spicy = true;
                    continue;
                }
                case "extraspicy" -> {
                    extraspicy = true;
                    continue;
                }
                case "ai" -> {
                    ai = true;
                    continue;
                }
            }
        }
        // then, once we have everything, update the session
        obj.setShow_ai(ai);
        obj.setShow_extra_spicy(extraspicy);
        obj.setShow_normal(normal);
        obj.setShow_spicy(spicy);
        // write updated session
        SessionUtils.updateSessionDetails(session, obj);
        // get the page
        String page = build_page(obj);
        // replace the thing
        // TODO: handle errors
        page = page.replace("${STATUS}", "Settings saved to session");
        // send it down the wire
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page, cxt, out));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionObject seshobj = SessionUtils.getSessionDetails(req.getSession());
        // quickly slap together the actual page
        String page = build_page(seshobj);
        // we dont need to show the result text so get rid of it
        page = page.replace("${STATUS}", "");
        // shit to send it down the tube
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page, cxt, out));
    }

    /**
     * this gets called twice so
     * instead of copy pasting code, we shall just do this instead
     * @param seshobj session object to handle
     * @return built page
     */
    private static String build_page(SessionObject seshobj) throws IOException{
        StringBuilder b = new StringBuilder();
        b.append(VerandaServer.template.getTemplate("header"));
        b.append(VerandaServer.sidebarBuilder.getSideBar());
        String settings_page = VerandaServer.template.getTemplate("settings");
        // set the checkboxes to the values currently stored in the user session
        settings_page = settings_page.replace("${NORM}", seshobj.isShow_normal() ? "checked" : "");
        settings_page = settings_page.replace("${SPICE}", seshobj.isShow_spicy() ? "checked" : "");
        settings_page = settings_page.replace("${ESPICE}", seshobj.isShow_extra_spicy() ? "checked" : "");
        settings_page = settings_page.replace("${AI}", seshobj.isShow_ai() ? "checked" : "");
        b.append(settings_page);
        b.append(VerandaServer.template.getTemplate("footer"));
        return b.toString();
    }
}
