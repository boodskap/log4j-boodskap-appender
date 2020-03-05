package io.boodskap.iot.ext.log4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
 
public class Main {
	
	private static final Logger logger = LogManager.getLogger(Main.class);
    
    public static void main(String[] args) throws InterruptedException {
        logger.trace("Trace Message");
        logger.debug("Debug Message");
        logger.info("Info Message");
        logger.warn("Warn Message");
        logger.error("Error Message");
        logger.fatal("Fatal Message");
        //Thread.sleep(5000);
    }
}