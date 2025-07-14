package com.eziosoft.verandagal.server.servlets;

import com.eziosoft.verandagal.client.utils.ImageUtils;
import com.eziosoft.verandagal.database.objects.Artist;
import com.eziosoft.verandagal.database.objects.ImagePack;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.server.utils.BasicTextWriter;
import com.eziosoft.verandagal.server.utils.ServerUtils;

import java.io.File;
import java.io.IOException;

public class ImageViewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // set the mime type right away for simplicity
        resp.setContentType("text/html; charset=UTF-8");
        // get the id
        long id;
        try {
            id = Long.parseLong(req.getParameter("id"));
        } catch (Exception e){
            // whatever we got is probably garbage, so explode
            ServerUtils.handleInvalidRequest(req, resp, "invalidimage");
            return;
        }
        // with our ID in hand, we can now attempt to load an image entry from the database
        // this code used to suck but now its slightly better! yay!
        Image ourimage = VerandaServer.maindb.LoadObject(Image.class, id);
        if (ourimage == null){
            ServerUtils.handleInvalidRequest(req, resp, "invalidimage");
            return;
        }
        // NEW FEATURE: don't display image based on cookie settings
        boolean gotfiltered = ServerUtils.checkForFiltering(req, ourimage);
        if (gotfiltered){
            // user settings say we should not display this image, bail out
            // but first, we should check and see if the override is enabled or not
            int override = 0;
            try {
                override = Integer.parseInt(req.getParameter("o"));
            } catch (Exception e){
                // do nothing, but this will explode if we dont put this here
                VerandaServer.LOGGER.debug("Failure parsing for override option");
            }
            // once we have it, check to see if it is 0
            // if it is, then we bail out, otherwise, we just continue anyway
            if (override == 0) {
                // we need to quickly build an error page for this
                String page = "";
                page += VerandaServer.template.getTemplate("header");
                page += VerandaServer.sidebarBuilder.getSideBar();
                // load the filtered template and then edit it
                String filtered = VerandaServer.template.getTemplate("filtered");
                filtered = filtered.replace("${RATING}", ServerUtils.getRatingText(ourimage.getRating()));
                filtered = filtered.replace("${ISAI}", ourimage.isAI() ? "Yes" : "No");
                // add it to our page
                page += filtered;
                page += VerandaServer.template.getTemplate("footer");
                // page done, send it down the wire
                AsyncContext cxt = req.startAsync();
                ServletOutputStream out = resp.getOutputStream();
                out.setWriteListener(new BasicTextWriter(page, cxt, out));
                // bail out of the rest of this
                return;
            }
        }
        // at this point i tihnk we have a valid image
        // lets try to build the page
        StringBuilder b = new StringBuilder();
        try {
            // this needs to be at the top to make sure we actually have the pack loaded n shit
            // attempt to read this information from the db
            String packname;
            ImagePack pack = VerandaServer.packinfo.getImagePack(ourimage.getPackid());
            if (pack == null){
                packname = "Invalid pack ID attached to this image";
            } else {
                packname = pack.getName();
            }
            // also, since packs have their own subfolders, we have to deal with this NOW instead of later
            // since we are now reading pack info from the DB
            // we can actually deal with this
            String addpath = "";
            if (pack != null){
                // set the variable to our pack folder name, with a /
                addpath = pack.getFsdir() + "/";
            }
            // ok back to normal code now yay
            b.append(VerandaServer.template.getTemplate("header"));
            b.append(VerandaServer.sidebarBuilder.getSideBar());
            // get the string of the image itself
            String imgview = VerandaServer.template.getTemplate("image");
            // now we have to replace all the shit
            if (!VerandaServer.configFile.isDontUsePreviews() || ImageUtils.checkIfFormatRequiresPreview(ourimage.getFilename())){
                // if true, show new preview
                imgview = imgview.replace("${PREVIEWURL}", "preview/?id=" + id);
            } else {
                // otherwise, just show the original image
                imgview = imgview.replace("${PREVIEWURL}", "img/" + addpath + ourimage.getFilename());
            }
            imgview = imgview.replace("${FILENAME}", FilenameUtils.getName(addpath + ourimage.getFilename()));
            File imagefile = ServerUtils.getImageFile(addpath + ourimage.getFilename());
            imgview = imgview.replace("${FILESIZE}", ServerUtils.getFileSizeMiB(imagefile));
            imgview = imgview.replace("${FILESIZE2}", ServerUtils.getFileSizeMB(imagefile));
            // we need to attempt to load the artist information
            Artist artist = VerandaServer.maindb.LoadObject(Artist.class, ourimage.getArtistid());
            if (artist == null){
                imgview = imgview.replace("${ARTIST}", "Invalid Artist ID attached to image");
            } else {
                imgview = imgview.replace("${ARTIST}", "<a href=\"/artist/?id=" + artist.getId() + "\">" + artist.getName() + "</a>");
            }
            // this information was loaded eariler, because things above needed it
            imgview = imgview.replace("${PACK}", packname);
            // these are stored with the image so we can just write them asap
            imgview = imgview.replace("${URL}", ServerUtils.buildURLifRequired(ourimage.getSourceurl()));
            imgview = imgview.replace("${RATING}", ServerUtils.getRatingText(ourimage.getRating()));
            // this was broken away from the built in rating system
            // so now we have to display it seperately
            imgview = imgview.replace("${ISAI}", Boolean.toString(ourimage.isAI()));
            imgview = imgview.replace("${DATE}", ourimage.getUploaddate());
            imgview = imgview.replace("${NOTES}", ourimage.getUploaderComments());
            // moved addpath to the top of the file
            imgview = imgview.replaceAll("\\$\\{IMGURL}", addpath + ourimage.getFilename());
            imgview = imgview.replace("${IMGRES}", ourimage.getImageres());
            // new feature: bottom navigation on image view page
            if (ourimage.getId() - 1 < 1){
                // just replace it with a non-link
                imgview = imgview.replace("${PREVLINK}", "Previous Image");
            } else {
                // replace with link to previous image
                imgview = imgview.replace("${PREVLINK}", "<a href=\"/image/?id=" + (ourimage.getId() - 1) + "\">Previous Image</a>");
            }
            // get how many images total we have
            long total = VerandaServer.maindb.getCountOfRecords(Image.class);
            if (ourimage.getId() + 1 <= total){
                // if true, populate url to next image
                imgview = imgview.replace("${NEXTLINK}", "<a href=\"/image/?id=" + (ourimage.getId() + 1) + "\">Next Image</a>");
            } else {
                // just put the text there
                imgview = imgview.replace("${NEXTLINK}", "Next Image");
            }
            // now we can slap that into the string builder
            b.append(imgview);
            b.append(VerandaServer.template.getTemplate("footer"));
        } catch (Exception e){
            // if something went wrong, just do a fake out
            ServerUtils.handleInvalidRequest(req, resp, "invalidimage");
            VerandaServer.LOGGER.error("Something went wrong during image display", e);
            return;
        }
        // otherwise, we're done building the page
        // so send it to the client
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(b.toString(), cxt, out));
    }
}
