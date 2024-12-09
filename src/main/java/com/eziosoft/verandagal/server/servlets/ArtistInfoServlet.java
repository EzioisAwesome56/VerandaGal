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
import java.util.HashMap;

public class ArtistInfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // set the return type
        resp.setContentType("text/html");
        // get the id of the artist
        long id;
        try {
            id = Long.parseLong(req.getParameter("id"));
        } catch (Exception e){
            // whatever we got is probably garbage, so explode
            ServerUtils.handleInvalidRequest(req, resp, "invalidartist");
            return;
        }
        // attempt to load the artist
        Artist artist = VerandaServer.maindb.LoadObject(Artist.class, id);
        // error checking
        if (artist == null){
            ServerUtils.handleInvalidRequest(req, resp, "invalidartist");
            return;
        }
        // then, do stuff
        StringBuilder page = new StringBuilder();
        // get the basics
        page.append(VerandaServer.template.getTemplate("header"));
        page.append(VerandaServer.sidebarBuilder.getSideBar());
        // load the template on its own
        String arttemp = VerandaServer.template.getTemplate("artist");
        // then, fill in the information we need
        arttemp = arttemp.replace("${ARTNAME}", artist.getName());
        arttemp = arttemp.replace("${ARTID}", Long.toString(artist.getId()));
        // we now have to build the url list for this artist
        StringBuilder urls = new StringBuilder();
        for (String s : artist.getUrls()){
            // TODO: find/add a way to display text here besides just "link"
            urls.append("<li><a href=\"").append(s).append("\">Link</a></li>");
        }
        // add this to the page
        arttemp = arttemp.replace("${ARTURLS}", urls.toString());
        arttemp = arttemp.replace("${ARTNOTES}", artist.getNotes());
        // pain in the ass time: display all images owned by the artist in a gallery on their page
        // get all the ids owned by them
        Long[] imgids = VerandaServer.maindb.getAllImagesByArtist(artist.getId());
        // get the built table
        HashMap<Integer, Object> output = ServerUtils.buildThumbnailGallery(req, imgids, VerandaServer.configFile.getItemsPerRow());
        // then, put the built mini-gallery into the page
        arttemp = arttemp.replace("${GALCONTENT}", (String) output.get(0));
        // some bonus code for handling filtered items
        int filter_count = (Integer) output.get(1);
        // also handle the other block for filter stats
        if (filter_count > 0){
            arttemp = arttemp.replace("${FILT_COUNT}", Integer.toString(filter_count) + " items where filtered");
        } else {
            arttemp = arttemp.replace("${FILT_COUNT}", "");
        }
        // TODO: deal with pagination
        // once done, add it to the string builder
        page.append(arttemp);
        // finish the page up
        page.append(VerandaServer.template.getTemplate("footer"));
        // send it
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page.toString(), cxt, out));
    }
}
