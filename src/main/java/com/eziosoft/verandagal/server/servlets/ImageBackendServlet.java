package com.eziosoft.verandagal.server.servlets;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import com.eziosoft.verandagal.server.utils.FileAsyncWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class ImageBackendServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get the url of the image, or try to anyway
        String[] raw;
        try {
            raw = req.getPathInfo().split("/");
        } catch (Exception e){
            handleInvalidRequest(req, resp);
            return;
        }
        // remove the 0th item from the list
        // also convert to list
        ArrayList<String> rawlist = new ArrayList<String>();
        Collections.addAll(rawlist, raw);
        rawlist.removeFirst();
        // make a path based on the thing
        String basepath = VerandaServer.configFile.getImageDir();
        for (String s : rawlist){
            basepath = FilenameUtils.concat(basepath, s);
        }
        // now to see if the file exists
        VerandaServer.LOGGER.debug("Debug path info: {}", basepath);
        File ourimage = new File(basepath);
        if (!ourimage.exists()){
            handleInvalidRequest(req, resp);
            return;
        }
        // now we have to write the image, sort of
        // first we need to set the mime type
        String type = FilenameUtils.getExtension(ourimage.getAbsolutePath());
        switch (type){
            case "jpg":
            case "jpeg":
                resp.setContentType("image/jpeg");
                break;
            case "png":
                resp.setContentType("image/png");
                break;
            default:
                VerandaServer.LOGGER.warn("Unknown mime type, sending octet-stream and hoping for the best");
                // per spec on MDN web docs, we should be setting the content type to this
                resp.setContentType("application/octet-stream");
        }
        // send our image data
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new FileAsyncWriter(ourimage, cxt, out));
    }

    private static void handleInvalidRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        resp.setContentType("text/plain");
        resp.setStatus(404);
        // send the result down the tube
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter("Request failed", cxt, out));
    }
}
