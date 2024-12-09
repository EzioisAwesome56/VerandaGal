package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.database.objects.Artist;
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

public class ArtistListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // set the content type right away
        resp.setContentType("text/html");
        // try to get a count of every artist
        long total_artists = VerandaServer.maindb.getCountOfRecords(Artist.class);
        if (total_artists < 1){
            // how did you cause this?
            VerandaServer.LOGGER.error("No artists where returned in the query, or an error occured");
            VerandaServer.LOGGER.error("Returned size is {}", total_artists);
            VerandaServer.LOGGER.error("If it was -1, you may have serious issues! Otherwise, make sure you imported the artists file!");
            ServerUtils.handleInvalidRequest(req, resp, "generic_error");
            return;
        }
        // otherwise, start building the page
        StringBuilder page = new StringBuilder();
        page.append(VerandaServer.template.getTemplate("header"));
        page.append(VerandaServer.sidebarBuilder.getSideBar());
        // load the content now
        String content = VerandaServer.template.getTemplate("artistlist");
        // then, build the list
        StringBuilder list = new StringBuilder();
        for (long x = 0;  x < total_artists; x++){
            // attempt to load artist
            String thename = "invalid artist";
            Artist temp = VerandaServer.maindb.LoadObject(Artist.class, x + 1);
            // if valid, set the name of the artist
            if (temp != null){
                thename = temp.getName();
            }
            // now, we build
            list.append("<li>");
            // make the link
            list.append("<a href=\"").append("/artist/?id=").append(x + 1).append("\">").append(thename).append("</a>");
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
