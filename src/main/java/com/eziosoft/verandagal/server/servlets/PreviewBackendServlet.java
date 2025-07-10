package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import com.eziosoft.verandagal.server.utils.FileAsyncWriter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;

public class PreviewBackendServlet extends HttpServlet {

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
        // now that we have the id, check to see if a preview exists for that id
        File ourpreview = new File(VerandaServer.configFile.getImagePreviewDir(), id + ".webp");
        if (!ourpreview.exists()){
            // the request failed. try again later
            VerandaServer.LOGGER.error("Preview does not exist for image id {}", id);
            handleInvalidRequest(req, resp);
            return;
        }
        // otherwise, set return type
        resp.setContentType("image/webp");
        // then, send the actual image data
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new FileAsyncWriter(ourpreview, cxt, out));
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
