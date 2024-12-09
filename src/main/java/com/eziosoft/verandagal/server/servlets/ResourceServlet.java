package com.eziosoft.verandagal.server.servlets;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;

import java.io.IOException;

public class ResourceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // code lightly adapted from previous DankAP code
        String[] raw;
        try {
            raw = req.getPathInfo().split("/");
        } catch (Exception e){
            AsyncContext cxt = req.startAsync();
            ServletOutputStream out = resp.getOutputStream();
            out.setWriteListener(new BasicTextWriter("failed", cxt, out));
            return;
        }
        if (raw.length < 1){
            AsyncContext cxt = req.startAsync();
            ServletOutputStream out = resp.getOutputStream();
            out.setWriteListener(new BasicTextWriter("failed", cxt, out));
            return;
        }
        // get the object we want
        String object = raw[1];
        // check to see if it is a supported file type
        String[] fileparts = object.split("\\.");
        if (!(fileparts[1].equals("js") || fileparts[1].equals("css"))){
            // fail
            AsyncContext cxt = req.startAsync();
            ServletOutputStream out = resp.getOutputStream();
            out.setWriteListener(new BasicTextWriter("failed", cxt, out));
            return;
        }
        String content;
        try {
            content = VerandaServer.template.getResource(object);
        } catch (IOException e) {
            AsyncContext cxt = req.startAsync();
            ServletOutputStream out = resp.getOutputStream();
            out.setWriteListener(new BasicTextWriter("failed", cxt, out));
            return;
        }
        // if we have our content, then we need to set what return type it is
        // TODO: make this suck less, if required
        if (fileparts[1].equals("css")){
            resp.setContentType("text/css");
        } else if (fileparts[1].equals("js")){
            resp.setContentType("text/javascript");
        }
        // send our content
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(content, cxt, out));
    }
}
