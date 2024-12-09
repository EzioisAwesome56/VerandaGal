package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.Main;
import com.eziosoft.verandagal.database.objects.Artist;
import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.database.objects.ImagePack;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.objects.ArtistAPIResponse;
import com.eziosoft.verandagal.server.objects.ImageAPIResponse;
import com.eziosoft.verandagal.server.objects.PackAPIResponse;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;

public class APIHandlerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // check to see if the API is even enabled
        if (!VerandaServer.configFile.isEnableAPI()){
            VerandaServer.LOGGER.warn("Somebody tried to access the API despite it being disabled!");
            // exit this call
            AsyncContext cxt = req.startAsync();
            ServletOutputStream out = resp.getOutputStream();
            out.setWriteListener(new BasicTextWriter("API is disabled and cannot be used", cxt, out));
            return;
        }
        // otherwise, do things
        // get the action id
        int action_id;
        // java gets angry if we don't define this, despite the fact the value should never be used in the first place
        long objid = -1;
        try {
            action_id = Integer.parseInt(req.getParameter("aid"));
        } catch (Exception e){
            // whoever tried to use the api probably doesnt know how to use it
            // display docs
            displayAPIHelp(req, resp);
            return;
        }
        // in some cases, objid is not actually required, so we have to handle it seperately
        try {
            objid = Long.parseLong(req.getParameter("oid"));
        } catch (Exception e){
            // do we actually have to give a shit?
            switch (action_id){
                case 1, 3 -> {}
                default -> {
                    displayAPIHelp(req, resp);
                    return;
                }
            }
        }
        // check to see if the action id is valid or not
        switch (action_id){
            case 0 -> {
                // do image api request
                doImageAPIRequest(req, resp, objid);
            }
            case 1 -> {
                doPackListRequest(req, resp);
            }
            case 2 -> {
                doPackInformationRequest(req, resp, objid);
            }
            case 3 -> {
                doArtistListRequest(req, resp);
            }
            case 4 -> {
                doArtistInformationRequest(req, resp, objid);
            }
            default -> {
                // assume incorrect/malformed api call
                displayAPIHelp(req, resp);
                return;
            }
        }
    }

    /**
     * this function displays the page for API docs
     * always gets displayed if the api url is entered directly
     * or the servlet has deemed the request to be an invalid request
     * @param req request from parent function
     * @param resp response from caller
     */
    private static void displayAPIHelp(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        // set the content type
        resp.setContentType("text/html");
        // build the page
        String page = "";
        page += VerandaServer.template.getTemplate("header");
        page += VerandaServer.sidebarBuilder.getSideBar();
        // the actual docs page
        page += VerandaServer.template.getTemplate("api");
        // footer
        page += VerandaServer.template.getTemplate("footer");
        // send it to the client
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(page, cxt, out));
        return;
    }

    /**
     * this function handles requests for image information
     * returns basically the image object as json if it exists
     * @param req request from caller
     * @param resp response from caller
     * @param imageid imageid of which you want to obtain the information for
     */
    private static void doImageAPIRequest(HttpServletRequest req, HttpServletResponse resp, long imageid) throws IOException {
        // set content type
        resp.setContentType("application/json");
        // attempt to get the image from the database
        Image theimage = VerandaServer.maindb.LoadObject(Image.class, imageid);
        // check if its valid
        if (theimage == null) {
            // send an error
            // and then bail
            doError(req, resp);
            return;
        }
        // first, we have to build our object
        ImageAPIResponse apiresp = new ImageAPIResponse();
        apiresp.setImg(theimage);
        // attempt to load the image pack for this image
        ImagePack pack = VerandaServer.packinfo.getImagePack(theimage.getPackid());
        String addpath = "/img/";
        if (pack != null){
            addpath += pack.getFsdir() + "/";
        }
        // build the main url for the image
        apiresp.addURL(addpath + theimage.getFilename());
        // also append the thumbnail url
        apiresp.addURL("/thumb/?id=" + theimage.getId());
        // then we can send this object to the user
        String json = Main.gson_pretty.toJson(apiresp);
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(json, cxt, out));
        return;
    }

    /**
     * return an error in json format to whatever
     * tried to request the API
     * @param req http request
     * @param resp http response
     */
    private static void doError(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        // very simple function
        // i just didnt want to copy paste the same code like 4 times
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter("{\"error\": \"valid request, invalid object requested\"}", cxt, out));
        return;
    }

    /**
     * this handles returning a list of all imagepacks present on the server
     * @param req http request
     * @param resp http response
     */
    private static void doPackListRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        // set return type to be json
        resp.setContentType("application/json");
        // first we want to get a list of all packs
        long numpacks = VerandaServer.maindb.getCountOfRecords(ImagePack.class);
        HashMap<Long, String> packlist = new HashMap<>();
        for (long x = 0; x < numpacks; x++){
            // attempt to load the pack
            ImagePack pack = VerandaServer.packinfo.getImagePack(x + 1);
            // set the name to default
            String name = "error loading pack info";
            // check to see if it actually exists
            if (pack != null){
                name = pack.getName();
            }
            // then, add that to the hashmap
            packlist.put(x + 1, name);
        }
        // once thats done, return that as json
        String json = Main.gson_pretty.toJson(packlist);
        // async send it
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(json, cxt, out));
    }

    /**
     * gives the user information about the requested pack
     * also gives a list of image IDs that are part of said pack
     * @param req http request
     * @param resp http response
     * @param packid pack id of which you want to query
     */
    private static void doPackInformationRequest(HttpServletRequest req, HttpServletResponse resp, long packid) throws IOException{
        // set the content type to json
        resp.setContentType("application/json");
        // attempt to get the pack id
        ImagePack pack = VerandaServer.packinfo.getImagePack(packid);
        // is it null?
        if (pack == null){
            // bail out because the pack was not found
            doError(req, resp);
            return;
        }
        // make a new object
        PackAPIResponse apiresp = new PackAPIResponse();
        // set our pack into it
        apiresp.setPack(pack);
        // get all the images in the pack
        Long[] images = VerandaServer.maindb.getAllImagesInPack(packid);
        // check if null
        if (images == null){
            // add -1 to the list
            VerandaServer.LOGGER.warn("Requested image pack {} returned null for list of image ids", packid);
            apiresp.addimage(-1l);
        } else {
            // otherwise, add every image id in the pack
            for (long id : images){
                apiresp.addimage(id);
            }
        }
        // convert to json
        String json = Main.gson_pretty.toJson(apiresp);
        // send it down the pipe
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(json, cxt, out));
    }

    /**
     * this endpoint returns a list of every artist known to the server
     * @param req http servlet request
     * @param resp http servlet response
     */
    private static void doArtistListRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        // set content type to json
        resp.setContentType("application/json");
        // get total count of artists from the db
        long num_artists = VerandaServer.maindb.getCountOfRecords(Artist.class);
        // error check
        if (num_artists == -1){
            VerandaServer.LOGGER.warn("Number of artists returned -1!");
            doError(req, resp);
            return;
        }
        // create hashmap of names and IDs
        HashMap<Long, String> apiresp = new HashMap<>();
        // input all of the artists into this hashmap
        for (long x = 0; x < num_artists; x++){
            // load the artist
            Artist art = VerandaServer.maindb.LoadObject(Artist.class, x + 1);
            // check if null
            if (art == null){
                apiresp.put(x + 1, "Invalid artist");
            } else {
                // put the actual name
                apiresp.put(x + 1, art.getName());
            }
        }
        // then, convert the hashmap to json and send it
        String json = Main.gson_pretty.toJson(apiresp);
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(json, cxt, out));
        return;
    }

    /**
     * this function handles requests to the artist information endpoint
     * @param req http servlet request
     * @param resp http servlet response
     * @param artistid the artist id of which you are querying
     */
    private static void doArtistInformationRequest(HttpServletRequest req, HttpServletResponse resp, long artistid) throws IOException{
        // set content type to json
        resp.setContentType("application/json");
        // attempt to load the artist
        Artist art = VerandaServer.maindb.LoadObject(Artist.class, artistid);
        // check if null
        if (art == null){
            VerandaServer.LOGGER.debug("Artist ID {} returned null", artistid);
            doError(req, resp);
            return;
        }
        // otherwise, create object to store all information in
        ArtistAPIResponse apiresp = new ArtistAPIResponse();
        // set the artist
        apiresp.setArtist(art);
        // attempt to get all images by artist
        Long[] ownedImages = VerandaServer.maindb.getAllImagesByArtist(artistid);
        // error handling
        if (ownedImages == null){
            VerandaServer.LOGGER.debug("List of images for artist id {} returned null", artistid);
            apiresp.addImage(-1l);
        } else {
            for (long iid : ownedImages){
                apiresp.addImage(iid);
            }
        }
        // once done, turn it into a string
        String json = Main.gson_pretty.toJson(apiresp);
        // and then send it
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(json, cxt, out));
    }
}
