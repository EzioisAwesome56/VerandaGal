package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.objects.ItemPage;
import com.eziosoft.verandagal.server.objects.SessionObject;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import com.eziosoft.verandagal.server.utils.ServerUtils;
import com.eziosoft.verandagal.server.utils.SessionUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;

public class ViewAllBrowseServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // steal some code from another servlet because i am a lazy piece of shit
        // set the mime type right away for simplicity
        resp.setContentType("text/html");
        // we also need to get the current page
        int pageno;
        try {
            pageno = Integer.parseInt(req.getParameter("p"));
        } catch (Exception e){
            // set to 0 for weird page numbers or just stuff that failed to parse
            // used to be 0 but we set it to 0 later so what is the point
            pageno = 0;
        }
        // start building the page
        StringBuilder b = new StringBuilder();
        b.append(VerandaServer.template.getTemplate("header").replace("${PAGENAME}", "All Images"));
        b.append(VerandaServer.sidebarBuilder.getSideBar());
        // get the gallery template
        String gallery = VerandaServer.template.getTemplate("gallery");
        // replace the strings
        // because this is the view all servlet, we can just set this to "All Images"
        gallery = gallery.replace("${PACKNAME}", "All Images");
        // we are going to force enable pagination if there are more then 4000 images in the database
        // so first we need to get the total number of images
        long total_images = VerandaServer.maindb.getCountOfRecords(Image.class);
        if (total_images == -1){
            VerandaServer.LOGGER.error("There are apparently no images at all...");
            ServerUtils.handleInvalidRequest(req, resp, "noimages");
            return;
        }
        // TODO: set as option in config file
        boolean force_pagination = total_images > 4000;
        // do new pagination, get the image ids from inside this class
        ItemPage page = new ItemPage(VerandaServer.maindb, req, force_pagination, pageno, total_images);
        // NEW FEATURE: we need to filter the gallery view too
        HashMap<Integer, Object> output = ServerUtils.buildThumbnailGallery(req, page.getPageContents(), VerandaServer.configFile.getItemsPerRow());
        // update the gallery string with our content
        gallery = gallery.replace("${GALCONTENT}", (String) output.get(0));
        int filter_count = (Integer) output.get(1);
        // also handle the other block for filter stats
        if (filter_count > 0){
            gallery = gallery.replace("${FILT_COUNT}", Integer.toString(filter_count) + " items where filtered");
        } else {
            gallery = gallery.replace("${FILT_COUNT}", "");
        }
        // update the count information with page information
        if (page.getTotal_pages() > 1){
            gallery = gallery.replace("${ITEMCOUNT}", "Showing " + Integer.toString(page.getPageContents().length) + " of " + Long.toString(total_images) + " items");
        } else {
            gallery = gallery.replace("${ITEMCOUNT}", "Showing " + Integer.toString(page.getPageContents().length) + " items");
        }
        // should we show the navigation?
        HttpSession sesseion = req.getSession();
        SessionObject sesh = SessionUtils.getSessionDetails(sesseion);
        if (sesh.isUse_pagination() || force_pagination){
            // load and build it
            String nav = VerandaServer.template.getTemplate("navigation");
            // do some dirty hacks to reuse old code for this task
            nav = nav.replace("${SIMPLENAV}", ServerUtils.buildNavigation(page, pageno, "/all/").replace("&", "?"));
            nav = nav.replace("${PAGES}", ServerUtils.buildPageCount(page, pageno));
            gallery = gallery.replace("${NAV}", nav);
        } else {
            gallery = gallery.replace("${NAV}", "");
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
