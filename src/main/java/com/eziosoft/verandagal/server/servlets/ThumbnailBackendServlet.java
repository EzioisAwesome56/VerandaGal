package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.database.objects.Thumbnail;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import com.eziosoft.verandagal.server.utils.ByteAsyncWriter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ThumbnailBackendServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // attempt to get the ID of the requested thumbnail
        long id;
        try {
            id = Long.parseLong(req.getParameter("id"));
        } catch (Exception e){
            // whatever we got is probably garbage, so explode
            handleInvalidRequest(req, resp);
            return;
        }
        // now that we have the id, attempt to load thumbnail from db
        Thumbnail thumb = VerandaServer.thumbnails.LoadThumbnail(id);
        if (thumb == null){
            // doesnt exist or something i guess
            VerandaServer.LOGGER.warn("Thumbnail with id {} does not exist", id);
            handleInvalidRequest(req, resp);
            return;
        }
        // otherwise, set return type
        resp.setContentType("image/jpeg");
        // then, send the actual image data
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new ByteAsyncWriter(thumb.getImagedata(), cxt, out));
    }

    // just to appease web browsers trying to access this page
    private static void handleInvalidRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        // set the correct content type
        resp.setContentType("text/plain");
        // send the result down the tube
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter("Request Failed", cxt, out));
    }
}
