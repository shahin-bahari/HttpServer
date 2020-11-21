package com.shahin.httpServer.logger;

import com.shahin.httpServer.HttpServer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {

    private static Logger logger = Logger.getLogger(HttpServer.SERVER_NAME);

    public static void log(Level level,String tag,String msg){
        logger.log(level,tag + " :\n" + msg);
    }
}
