package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.database.objects.Image;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GalleryBrowseServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // steal some code from another servlet because i am a lazy piece of shit
        // set the mime type right away for simplicity
        resp.setContentType("text/html");
        // get the id
        long id;
        try {
            id = Long.parseLong(req.getParameter("pid"));
        } catch (Exception e){
            // whatever we got is probably garbage, so explode
            //VerandaServer.LOGGER.error("Error while trying to get gallery id", e);
            ServerUtils.handleInvalidRequest(req, resp, "invalidpack");
            return;
        }
        // then, attempt to load the pack and see if it exists
        ImagePack pack = VerandaServer.maindb.LoadObject(ImagePack.class, id);
        if (pack == null){
            VerandaServer.LOGGER.error("Error while trying to load pack");
            ServerUtils.handleInvalidRequest(req, resp, "invalidpack");
        }
        // start building the page
        StringBuilder b = new StringBuilder();
        b.append(VerandaServer.template.getTemplate("header"));
        b.append(VerandaServer.sidebarBuilder.getSideBar());
        // get the gallery template
        String gallery = VerandaServer.template.getTemplate("gallery");
        // replace the strings
        gallery = gallery.replace("${PACKNAME}", pack.getName());
        // find out how many images we have in the DB
        Long[] imgids = VerandaServer.maindb.getAllImagesInPack(pack.getId());
        // NEW FEATURE: we need to filter the gallery view too
        HashMap<Integer, Object> output = ServerUtils.buildThumbnailGallery(req, imgids, VerandaServer.configFile.getItemsPerRow());
        // update the gallery string with our content
        gallery = gallery.replace("${GALCONTENT}", (String) output.get(0));
        int filter_count = (Integer) output.get(1);
        // also handle the other block for filter stats
        if (filter_count > 0){
            gallery = gallery.replace("${FILT_COUNT}", Integer.toString(filter_count) + " items where filtered");
        } else {
            gallery = gallery.replace("${FILT_COUNT}", "");
        }
        b.append(gallery);
        // add the footer
        b.append(VerandaServer.template.getTemplate("footer"));
        // send the finished page down the pipes
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(b.toString(), cxt, out));
    }
}
