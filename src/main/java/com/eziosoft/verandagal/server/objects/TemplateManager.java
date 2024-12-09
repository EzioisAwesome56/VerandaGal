package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.server.VerandaServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TemplateManager {

    private final HashMap<String, String> cached_content;

    public TemplateManager(){
        // setup the cacher
        this.cached_content = new HashMap<String, String>();
    }

    public String getResource(String name) throws IOException {
        // check if its cached
        if (this.cached_content.containsKey(name)){
            // used the cached content instead
            return this.cached_content.get(name);
        } else {
            // attempt to load the resource
            String resource = IOUtils.resourceToString("/resource/" + name, StandardCharsets.UTF_8);
            // if we have it, we can probably cache it
            this.cached_content.put(name, resource);
            // return what we have
            return resource;
        }
    }

    private static String silently_patch(String content){
        String patched = content;
        if (patched.contains("${SERVNAME}")){
            patched = patched.replaceAll("\\$\\{SERVNAME}", VerandaServer.configFile.getServiceName());
        }

        return patched;
    }

    public String getTemplate(String name) throws IOException {
        // check if it is cached
        if (this.cached_content.containsKey(name)){
            // returned cached content
            VerandaServer.LOGGER.debug("using cached content");
            return this.cached_content.get(name);
        } else {
            // attempt to read the file from resources
            String oof = IOUtils.resourceToString("/templates/" + name + ".jht", StandardCharsets.UTF_8);
            // we will see if we can silently autopatch whatever we got
            oof = silently_patch(oof);
            // cache it
            this.cached_content.put(name, oof);
            // return it
            return oof;
        }
    }
}
