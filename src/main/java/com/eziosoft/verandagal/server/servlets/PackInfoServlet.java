package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.database.objects.ImagePack;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import com.eziosoft.verandagal.server.utils.ServerUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class PackInfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /*
        this code was stolen and reused from the artist info page
        they do pretty much the same thing so it makes it easy
         */
        // set the return type
        resp.setContentType("text/html");
        // get the id of the artist
        long id;
        try {
            id = Long.parseLong(req.getParameter("pid"));
        } catch (Exception e){
            // whatever we got is probably garbage, so explode
            ServerUtils.handleInvalidRequest(req, resp, "invalidpack");
            return;
        }
        // attempt to load the artist
        ImagePack pack = VerandaServer.maindb.LoadObject(ImagePack.class, id);
        // error checking
        if (pack == null){
            ServerUtils.handleInvalidRequest(req, resp, "invalidpack");
            return;
        }
        // then, do stuff
        StringBuilder page = new StringBuilder();
        // get the basics
        page.append(VerandaServer.template.getTemplate("header"));
        page.append(VerandaServer.sidebarBuilder.getSideBar());
        // load the template on its own
        String packtemp = VerandaServer.template.getTemplate("pack");
        // then, fill in the information we need
        packtemp = packtemp.replace("${PACKNAME}", pack.getName());
        packtemp = packtemp.replace("${PACKID}", Long.toString(id));
        packtemp = packtemp.replace("${PACKNUM}", Long.toString(pack.getTotalImages()));
        packtemp = packtemp.replace("${PACKUPLOAD}", pack.getUploadDate());
        packtemp = packtemp.replace("${PACKNOTES}", pack.getDescription());
        packtemp = packtemp.replace("${URL}", "/pack/?pid=" + Long.toString(pack.getId()));
        // put it into the page string builder
        page.append(packtemp);
        // finish the page up
        page.append(VerandaServer.template.getTemplate("footer"));
        // send it
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page.toString(), cxt, out));
    }
}
