package com.ngnl.hackpen.myapplication.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import javax.inject.Named;

/**
 * An endpoint class we are exposing
 */
@Api(
        name = "myApi",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.myapplication.hackpen.ngnl.com",
                ownerName = "backend.myapplication.hackpen.ngnl.com",
                packagePath = ""
        )
)

/**
 * Created by Arthur on 9/9/17.
 */

public class AddDataChunk {

    @ApiMethod(name = "addDataChunk")
    public Chunk addDataChunk(
            @Named("time") long[] time,
            @Named("x") float[] x,
            @Named("y") float[] y,
            @Named("z") float[] z) {

        Chunk chunk = new Chunk(time, x, y, z);
        Chunk.Point currentPoint;
        for (int i  = 0; i < time.length; i++) {
            currentPoint = chunk.getPoint(i);
            Storage.addData(
                    currentPoint.timestamp,
                    currentPoint.x,
                    currentPoint.y,
                    currentPoint.z);
        }
        return chunk;
    }
}
