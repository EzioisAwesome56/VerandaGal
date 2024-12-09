package com.eziosoft.verandagal.server.utils;

import com.eziosoft.verandagal.server.VerandaServer;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileAsyncWriter implements WriteListener {
    // shamelessly stolen from another project
    // may that project rest in peace
    private final InputStream towrite;
    private final AsyncContext context;
    private final ServletOutputStream out;

    public FileAsyncWriter(File file, AsyncContext ctx, ServletOutputStream out) throws IOException {
        // assign our variables
        this.context = ctx;
        this.out = out;
        // read our file to a byte array so we can send the bytes
        // down an async pipe later
        this.towrite = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
    }


    @Override
    public void onWritePossible() throws IOException {
        // lightly borrowed from https://webtide.com/servlet-3-1-async-io-and-jetty/
        byte[] buffer = new byte[2048];
        while (this.out.isReady()){
            int len = this.towrite.read(buffer);
            // check if we ran out of data
            if (len < 0){
                // we're done
                this.context.complete();
                // close streams
                this.out.close();
                this.towrite.close();
                // yeet
                return;
            }
            // write out the data
            this.out.write(buffer, 0, len);
        }

    }

    @Override
    public void onError(Throwable throwable) {
        VerandaServer.LOGGER.error("Error while doing async task", throwable);
        this.context.complete();
    }
}
