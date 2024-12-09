package com.eziosoft.verandagal.server.utils;

import com.eziosoft.verandagal.database.objects.ImagePack;
import com.eziosoft.verandagal.server.VerandaServer;

import java.io.IOException;

public class SidebarBuilder {

    // because you cannot import packs at runtime,
    // we can cache the sidebar here
    private String sidebar = "";

    public String getSideBar() throws IOException {
        if (this.sidebar.isEmpty()){
            VerandaServer.LOGGER.debug("Now building the sidebar");
            this.sidebar = VerandaServer.template.getTemplate("sidebar");
            long numpacks = VerandaServer.maindb.getCountOfRecords(ImagePack.class);
            VerandaServer.LOGGER.debug("Total number of packs in db: {}", numpacks);
            // check to make sure it did not return -1
            if (numpacks == -1){
                VerandaServer.LOGGER.error("Apparently, there are NO packs in the DB!");
                return "Error building sidebar";
            }
            // otherwise, we can then start building the sidebar links
            StringBuilder b = new StringBuilder();
            // loop thru each pack
            for (int x = 0; x < numpacks; x++){
                // load the pack
                // FIX 12-9-2024: i had a thunk that this should be using the pack cacher, so now it does
                ImagePack pack = VerandaServer.packinfo.getImagePack(x + 1);
                // error checking because i am very paranoid
                if (pack == null){
                    VerandaServer.LOGGER.error("ERROR: Somehow, we have obtained a pack that does not exist!");
                    VerandaServer.LOGGER.error("Pack ID we tried to load: {}", x + 1);
                    return "Failed to load pack";
                }
                // otherwise, we can keep going!
                b.append("<li>");
                // start the link portion of this
                b.append("<a href=\"/pack/?pid=");
                b.append(x + 1);
                b.append("\">");
                // put the name of the pack in the sidebar too
                b.append(pack.getName());
                // dont forget to close the link element!
                b.append("</a>");
                b.append("</li>");
            }
            // once we're done, do string replacement magic
            this.sidebar = this.sidebar.replace("${PACKLIST}", b.toString());
        }
        return sidebar;
    }
}
