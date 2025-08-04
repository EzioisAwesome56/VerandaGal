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
import java.util.List;
import java.util.Map;

public class SearchPageServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // check to make sure search is even enabled
        if (!VerandaServer.configFile.isEnable_search()){
            ServerUtils.handleInvalidRequest(req, resp, "nosearch");
            return;
        }
        resp.setContentType("text/html");
        // get session
        HttpSession session = req.getSession();
        SessionObject obj = SessionUtils.getSessionDetails(session);
        // setup the string to hold the search terms
        String searchterms = null;
        for (Map.Entry<String, String[]> thing : req.getParameterMap().entrySet()){
            switch (thing.getKey()) {
                case "searchterms" -> {
                    searchterms = thing.getValue()[0];
                    continue;
                }
                default -> {
                    VerandaServer.LOGGER.warn("Invalid post field was found in the submitted data to search page!");
                }
            }
        }
        // get the page
        // this is the search page, so we want the results page
        String page = build_page(true);
        // do some string replacing
        page = page.replace("${PACKNAME}", "Search Results");
        // next, run a search
        List<Image> found_images = VerandaServer.maindb.searchImages(searchterms);
        // list how many items where found
        page = page.replace("${ITEMCOUNT}", "Found " + found_images.size() + " images from your search");
        if (found_images.isEmpty()){
            // no search result where found, zamn
            page = page.replace("${GALCONTENT}", "Sorry, nothing was found");
            // fix: remove the filtercount placeholder if nothing was found
            page = page.replace("${FILT_COUNT}", "");
        } else {
            // setup the gallery view using an ItemPage object
            ItemPage itempage = new ItemPage(found_images);
            // copy-pasted code from the main gallery browser; build the page
            HashMap<Integer, Object> output = ServerUtils.buildThumbnailGallery(req, itempage.getPageContents(), VerandaServer.configFile.getItemsPerRow());
            // update the gallery string with our content
            page = page.replace("${GALCONTENT}", (String) output.get(0));
            int filter_count = (Integer) output.get(1);
            // also handle the other block for filter stats
            if (filter_count > 0){
                page = page.replace("${FILT_COUNT}", Integer.toString(filter_count) + " items where filtered");
            } else {
                page = page.replace("${FILT_COUNT}", "");
            }
        }
        // remove the nav text
        page = page.replace("${NAV}", "");
        // send it down the wire
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page, cxt, out));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // check to make sure search is even enabled
        if (!VerandaServer.configFile.isEnable_search()){
            ServerUtils.handleInvalidRequest(req, resp, "nosearch");
            return;
        }
        resp.setContentType("text/html");
        // quickly slap together the actual page
        // send false because this is the search input page
        String page = build_page(false);
        // shit to send it down the tube
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page, cxt, out));
    }

    /**
     * this gets called twice so
     * instead of copy pasting code, we shall just do this instead
     * @param result is this the results page or the main search page?
     * @return built page
     */
    private static String build_page(boolean result) throws IOException{
        StringBuilder b = new StringBuilder();
        String header = VerandaServer.template.getTemplate("header");
        // change the page title based on if we're in the search results or not
        if (result){
            header = header.replace("${PAGENAME}", "Search Results");
        } else {
            header = header.replace("${PAGENAME}", "Search");
        }
        b.append(header);
        b.append(VerandaServer.sidebarBuilder.getSideBar());
        String page_content;
        if (result){
            // send a blank gallery; we will do stuff with it later
            page_content = VerandaServer.template.getTemplate("gallery");
        } else {
            // send the original search page
            page_content = VerandaServer.template.getTemplate("search");
        }
        b.append(page_content);
        b.append(VerandaServer.template.getTemplate("footer"));
        return b.toString();
    }
}
