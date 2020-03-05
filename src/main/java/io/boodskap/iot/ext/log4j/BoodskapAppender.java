package io.boodskap.iot.ext.log4j;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.json.JSONObject;

import com.mashape.unirest.http.Unirest;

/**
 * An Appender that delivers events to Boodskap IoT Platform
 */

@Plugin(name = "Boodskap", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class BoodskapAppender extends AbstractAppender implements Runnable {
	
	protected final int queueSize;

    /**
     * Boodskap Base API Path
     */
    protected final String apiBasePath;

    /**
     * Boodskap Domain Key
     */
    protected final String domainKey;

    /**
     * Boodskap API Key
     */
    protected final String apiKey;

    /**
     * Application unique ID
     */
    protected final String appId;
    

    protected final boolean sync;
    private final LinkedBlockingQueue<String> outQ;
    private final ExecutorService exec;

    public BoodskapAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties, int queueSize, boolean sync, String apiBasePath, String domainKey, String apiKey, String appId) {
		
    	super(name, filter, layout, ignoreExceptions, properties);
		
		this.queueSize = queueSize;
		this.apiBasePath = apiBasePath;
		this.domainKey = domainKey;
		this.apiKey = apiKey;
		this.appId = appId;
		this.sync = sync;
		
		if(!sync) {
			
			outQ = new LinkedBlockingQueue<>(queueSize);
			
			exec = Executors.newSingleThreadExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread runner = new Thread(r);
					runner.setDaemon(true);
					return runner;
				}
			});
			
			exec.submit(this);
		}else {
			outQ = null;
			exec = null;
			sendPattern();
		}
		
	}

	public static abstract class AbstractBuilder<B extends AbstractBuilder<B>> extends AbstractAppender.Builder<B> {

        @PluginBuilderAttribute
        private int queueSize;

        @PluginBuilderAttribute
        private String apiBasePath;

        @PluginBuilderAttribute
        private String domainKey;
        
        @PluginBuilderAttribute
        private String apiKey;
        
        @PluginBuilderAttribute
        private String appId;

        @PluginBuilderAttribute
        private boolean sync = false;

		public int getQueueSize() {
			return queueSize;
		}

		public void setQueueSize(int queueSize) {
			this.queueSize = queueSize;
		}

		public String getApiBasePath() {
			return apiBasePath;
		}

		public void setApiBasePath(String apiBasePath) {
			this.apiBasePath = apiBasePath;
		}

		public String getDomainKey() {
			return domainKey;
		}

		public void setDomainKey(String domainKey) {
			this.domainKey = domainKey;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public boolean isSync() {
			return sync;
		}

		public void setSync(boolean sync) {
			this.sync = sync;
		}

		public AbstractBuilder<?>  withSync(boolean sync) {
			this.sync = sync;
			return this;
		}

		public AbstractBuilder<?>  withQueueSize(int queueSize) {
			this.queueSize = queueSize;
			return this;
		}

		public AbstractBuilder<?> withApiBasePath(String apiBasePath) {
			this.apiBasePath = apiBasePath;
			return this;
		}

		public AbstractBuilder<?> withDomainKey(String domainKey) {
			this.domainKey = domainKey;
			return this;
		}

		public AbstractBuilder<?> withApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public AbstractBuilder<?> withAppId(String appId) {
			this.appId = appId;
			return this;
		}
    }

    /**
     * Builds a BoodskapAppender.
     */
    public static class Builder extends AbstractBuilder<Builder> implements org.apache.logging.log4j.core.util.Builder<BoodskapAppender> {
    	
        @Override
        public BoodskapAppender build() {
        	return new BoodskapAppender(getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray(), getQueueSize(), isSync(), getApiBasePath(), getDomainKey(), getApiKey(), getAppId());
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
    	
    	if(null != exec && (!exec.isShutdown() && !exec.isTerminated())) {
    		exec.shutdownNow();
    	}
        
    	setStopped();
        
        return true;
    }

	@Override
	public void append(LogEvent event) {
		try {
			
			JSONObject log = new JSONObject();
			log.put("appid", appId);
			log.put("created_time", event.getTimeMillis());
			log.put("log_data", event.getMessage().getFormattedMessage());
			log.put("severity", event.getLevel().toString());
			log.put("thread", event.getThreadName());
			log.put("threadp", event.getThreadPriority());
			log.put("threadid", event.getThreadId());
			
			if(null != event.getSource()) {
				StackTraceElement src = event.getSource();
				log.put("clazz", src.getClassName());
				log.put("file", src.getFileName());
				log.put("line", src.getLineNumber());
				log.put("method", src.getMethodName());
			}
			
			if(null != event.getThrown()) {
				log.put("exception", ExceptionUtils.getStackTrace(event.getThrown()));
			}
			
			if(sync) {
				send(log.toString(), "log4j");
			}else {
				outQ.offer(log.toString());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		
		try {
			
			sendPattern();
			
			while(true) {
				String out = outQ.take();
				send(out, "log4j");
			}
			
		}catch(InterruptedException iex) {
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	private void sendPattern() {
		
		JSONObject json = new JSONObject();
		json.put("appid", appId);
		json.put("pattern", ((PatternLayout) getLayout()).getConversionPattern());
		
		send(json.toString(), "log4j-pattern");
				
	}

	private void send(String out, String rule) {
		try {
			@SuppressWarnings("unused")
			String res = Unirest.post(String.format("%s/mservice/push/bin/data/{dkey}/{akey}/-/-/-/{type}", apiBasePath))
			.routeParam("dkey", domainKey)
			.routeParam("akey", apiKey)
			.routeParam("type", rule)
			.body(out)
			.asString().getBody()
			;			
			//System.err.println(res);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
