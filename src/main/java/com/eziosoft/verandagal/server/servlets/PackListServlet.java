package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.database.objects.Artist;
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

public class PackListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* why yes, i did reuse
        most of this code from the artist list servlet, how could you tell?
         */
        // set the content type right away
        resp.setContentType("text/html");
        // try to get a count of every artist
        long total_artists = VerandaServer.maindb.getCountOfRecords(ImagePack.class);
        if (total_artists < 1){
            // how did you cause this?
            VerandaServer.LOGGER.error("No packs where returned in the query, or an error occured");
            VerandaServer.LOGGER.error("Returned size is {}", total_artists);
            VerandaServer.LOGGER.error("If it was -1, you may have serious issues! Otherwise, make sure you have packs installed!");
            ServerUtils.handleInvalidRequest(req, resp, "generic_error");
            return;
        }
        // otherwise, start building the page
        StringBuilder page = new StringBuilder();
        page.append(VerandaServer.template.getTemplate("header").replace("${PAGENAME}", "Pack listing"));
        page.append(VerandaServer.sidebarBuilder.getSideBar());
        // load the content now
        String content = VerandaServer.template.getTemplate("packlist");
        // then, build the list
        StringBuilder list = new StringBuilder();
        for (long x = 0;  x < total_artists; x++){
            // attempt to load artist
            String thename = "invalid image pack";
            ImagePack temp = VerandaServer.maindb.LoadObject(ImagePack.class, x + 1);
            // if valid, set the name of the artist
            if (temp != null){
                thename = temp.getName();
            }
            // now, we build
            list.append("<li>");
            // make the link
            list.append("<b>").append(thename).append("</b>: ").append("<a href=\"/pack/?pid=").append(x + 1).append("\">Browse</a> | ")
                    .append("<a href=\"/packinfo/?pid=").append(x + 1).append("\">Pack Info</a>");
            // end the list element
            list.append("</li>");
        }
        // then, update the page
        content = content.replace("${LIST}", list.toString());
        // finish the page
        page.append(content);
        page.append(VerandaServer.template.getTemplate("footer"));
        // send it down the pipe
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page.toString(), cxt, out));
    }
}
