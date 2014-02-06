/**
 * This code creates a long pooling connection to stream JSONP data 
 * from a REDIS DB to a client using a servlet. The connection is
 * kept alive until one of the conditions occur:
 * 		1. The streaming connection duration expires (subscription_duration parameter value)
 * 		2. The REDIS DB connection times out (REDIS_CALLBACK_TIMEOUT constant)
 * 		3. Connection loss, e.g., client closes the connection 

 * The code accepts i) channel name, ii) fully qualified channel name and, iii) wildcard '*' for
 * pattern based subscription.
 * 
 * @author Koushik Sinha
 * Last modified: 02/01/2014
 *
 * Dependencies:  servlets 3+, jedis-2.2.1, gson-2.2.4, commons-pool-1.6, slf4j-1.7.5
 * 	
 * Hints for testing:
 * 		1. You can increase the test duration by adjusting the SUBSCRIPTION_MAX_DURATION. 
 *  	2. Tune REDIS_CALLBACK_TIMEOUT, in case the rate of publication is very slow
 *  	3. Tune the number of threads in ExecutorService
 *
 * Deployment steps: 
 * 		1. [Required] Set redisHost and redisPort in code, as per your REDIS setup/location
 * 		2. [Optional] Tune time-out and other parameters, if necessary
 * 		3. [Required]Compile and package as WAR file
 * 		4. [Required] Deploy as WAR file in glassfish 3.1.2
 * 		5. [Optional] Setup ssh tunneling (e.g. command: ssh tunneling:: ssh -f -L 1978:localhost:6379 scd1.qcri.org -N)
 * 		6. Issue stream request from client
 *
 *
 * Invocations: 
 * ============
 * Channel Name based examples:
 *  1. http://localhost:8080/aidr-output/stream?crisisCode=clex_20131201&callback=print&rate=10  
 *  2. http://localhost:8080/aidr-output/stream?crisisCode=clex_20131201&duration=1h 
 *  3. http://localhost:8080/aidr-output/stream?crisisCode=clex_20131201&duration=1h&callback=print
 *  4. http://localhost:8080/aidr-output/stream?crisisCode=clex_20131201&duration=1h&rate=15
 *  5. http://localhost:8080/aidr-output/stream?crisisCode=clex_20131201&duration=1h&callback=print&rate=10
 *  
 * Wildcard based examples:
 *  1. http://localhost:8080/aidr-output/stream?crisisCode=*&callback=print&rate=10 
 *  2. http://localhost:8080/aidr-output/stream?crisisCode=*&duration=1h 
 *  3. http://localhost:8080/aidr-output/stream?crisisCode=*&duration=1h&callback=print
 *  4. http://localhost:8080/aidr-output/stream?crisisCode=*&duration=1h&rate=15
 *  5. http://localhost:8080/aidr-output/stream?crisisCode=*&duration=1h&callback=print&rate=10
 *  
 * Fully qualified channel name examples:
 *  1. http://localhost:8080/aidr-output/stream?crisisCode=aidr_predict.clex_20131201&callback=print&rate=10 
 *  2. http://localhost:8080/aidr-output/stream?crisisCode=aidr_predict.clex_20131201&duration=1h 
 *  3. http://localhost:8080/aidr-output/stream?crisisCode=aidr_predict.clex_20131201&duration=1h&callback=print
 *  4. http://localhost:8080/aidr-output/stream?crisisCode=aidr_predict.clex_20131201&duration=1h&rate=15
 *  5. http://localhost:8080/aidr-output/stream?crisisCode=aidr_predict.clex_20131201&duration=1h&callback=print&rate=10
 * 
 *  
 *  Parameter explanations:
 *  	1. crisisCode [mandatory]: the REDIS channel to which to subscribe
 *  	2. subscription_duration [optional]: time for which to subscribe (connection automatically closed after that). 
 *		   	The allowed suffixes are: s (for seconds), m (for minutes), h (for hours) and d (for days). The max subscription 
 *		   	duration is specified by the hard coded SUBSCRIPTION_MAX_DURATION value (default duration). 
 *  	3. callback [optional]: name of the callback function for JSONP data
 *  	4. rate [optional]: an upper bound on the rate at which to send messages to client, expressed as messages/min 
 *  	   	(a floating point number). If <= 0, then default rate is assumed.
 */

package qa.qcri.aidr.output.getdata;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import qa.qcri.aidr.output.utils.JsonDataFormatter;
import qa.qcri.aidr.output.utils.WriteResponse;

@Path("/stream")
public class RedisHTTPStreaming implements AsyncListener, ServletContextListener {

	// Time-out constants
	private static final int REDIS_CALLBACK_TIMEOUT = 5 * 60 * 1000;		// in ms
	private static final int SUBSCRIPTION_MAX_DURATION = 6 * 60 * 60 * 1000;			// in ms

	// Pertaining to JEDIS - establishing connection with a REDIS DB
	// Currently using ssh tunneling:: ssh -f -L 1978:localhost:6379 scd1.qcri.org -N
	// Channel(s) being used for testing:
	// 		a) aidr_predict.clex_20131201
	//		
	private static final String CHANNEL_PREFIX_CODE = "aidr_predict.";
	private boolean patternSubscriptionFlag;

	private String redisChannel = "*";										// channel to subscribe to		
	private static final String redisHost = "localhost";					// Current assumption: REDIS running on same m/c
	private static final int redisPort = 1978;					
	public static JedisPoolConfig poolConfig = null;
	public static JedisPool pool = null;
	public Jedis subscriberJedis = null;
	public RedisSubscriber aidrSubscriber = null;

	// Related to Async Thread management
	public static ExecutorService executorServicePool;
	public AsyncContext asyncContext;


	private boolean runFlag = true;
	private boolean error = false;
	private boolean timeout = false;
	private long subscriptionDuration = SUBSCRIPTION_MAX_DURATION;

	// rate control related 
	private static final int DEFAULT_SLEEP_TIME = 0;		// in msec
	private float messageRate = 0;							// default: <= 0 implies no rate control
	private int sleepTime = DEFAULT_SLEEP_TIME;

	// Share data structure between Jedis and Async threads
	public List<String> messageList = Collections.synchronizedList(new ArrayList<String>());
	private final boolean rejectNullFlag = false;

	// Debugging
	private static Logger logger = LoggerFactory.getLogger(RedisHTTPStreaming.class);

	/////////////////////////////////////////////////////////////////////////////
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// For now: set up a simple configuration that logs on the console
		//PropertyConfigurator.configure("log4j.properties");		// where to place the properties file?
		//BasicConfigurator.configure();							// initialize log4j logging
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");		// set logging level for slf4j
		logger.info("[init] In servlet init...");
		initJedisPool();
		executorServicePool = Executors.newFixedThreadPool(200);		// max number of threads
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			logger.debug("[destroy] Attempting stopSubscription...");
			stopSubscription(subscriberJedis);
			pool.destroy();
			logger.info("[destroy] stopSubscription success!");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("[destroy] Exception occurred attempting stopSubscription: " + e.toString());
			e.printStackTrace();
		}
		shutdownAndAwaitTermination(executorServicePool);
	}
	
	// Initialize JEDIS parameters and thread pool
	public void initJedisPool() {
		if (null == poolConfig) {
			poolConfig = new JedisPoolConfig();
			poolConfig.setMaxActive(100);
			poolConfig.setMaxIdle(50);
			poolConfig.setMinIdle(5);
			poolConfig.setTestWhileIdle(true);
			poolConfig.setTestOnBorrow(true);
			poolConfig.setTestOnReturn(true);
			poolConfig.numTestsPerEvictionRun = 10;
			poolConfig.timeBetweenEvictionRunsMillis = 60000;
			poolConfig.maxWait = 3000;
			poolConfig.whenExhaustedAction = org.apache.commons.pool.impl.GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW;
			logger.info("[connectToRedis] New Jedis poolConfig: " + poolConfig);
		} else {
			logger.info("[connectToRedis] Reusing existing Jedis poolConfig: " + poolConfig);
		}
		if (null == pool) {
			pool = new JedisPool(poolConfig, redisHost, redisPort, 10000);
			logger.info("[connectToRedis] New Jedis pool: " + pool);
		} else {
			logger.info("[connectToRedis] Reusing existing Jedis pool: " + pool);
		}
	}

	public boolean initRedisConnection() { 
		subscriberJedis = pool.getResource();
		if (subscriberJedis != null) {
			logger.info("[initRedisConnection] Obtained Jedis object from pool");
			return true;
		}
		return false;
	}

	// Stop subscription of this subscribed thread and return resources to the JEDIS thread pool
	private void stopSubscription(final Jedis jedis) {
		if (aidrSubscriber != null && aidrSubscriber.getSubscribedChannels() > 0) {
			logger.debug("[stopSubscription] subscribed channels count = " + aidrSubscriber.getSubscribedChannels());
			if (!patternSubscriptionFlag) { 
				aidrSubscriber.unsubscribe();				
			}
			else {
				aidrSubscriber.punsubscribe();
			}
			logger.info("[stopSubscription] unsubscribed " + aidrSubscriber + ", Subscription count = " + aidrSubscriber.getSubscribedChannels());
		}

		try {
			pool.returnResource(jedis);
			logger.info("[stopSubscription] Pool resource returned");
		} catch (JedisConnectionException e) {
			logger.error("[stopsubscription] JedisConnectionException occurred...");
			pool.returnBrokenResource(jedis);
		}
		logger.info("[stopSubscription] Subscription ended for Channel=" + redisChannel);
	}


	// Create a subscription to specified REDIS channel: spawn a new thread
	private void subscribeToChannel(final RedisSubscriber sub, final Jedis jedis, String channel) throws Exception {
		logger.info("[subscribeToChannel] sub = " + sub + ", jedis = " + jedis);
		redisChannel = channel;
		executorServicePool.submit(new Runnable() {
			public void run() {
				try {
					logger.info("[subscribeToChannel] patternSubscriptionFlag = " + patternSubscriptionFlag);
					if (!patternSubscriptionFlag) { 
						logger.info(sub + "@[subscribeToChannel] Attempting subscription for " + redisHost + ":" + redisPort + "/" + redisChannel);
						jedis.subscribe(sub, redisChannel);
						logger.info(sub + "@[subscribeToChannel] Out of subscription for Channel = " + redisChannel);
					} 
					else {
						logger.info(sub + "@[subscribeToChannel] Attempting pSubscription for " + redisHost + ":" + redisPort + "/" + redisChannel);
						jedis.psubscribe(sub, redisChannel);
						logger.info(sub + "@[subscribeToChannel] Out of pSubscription for Channel = " + redisChannel);
					}
				} catch (Exception e) {
					logger.error(sub + "@[subscribeToChannel] AIDR Predict Channel Subscribing failed");
					stopSubscription(jedis);
				} finally {
					try {
						logger.info(sub + "@[subscribeToChannel::finally] Attempting stopSubscription...");
						stopSubscription(jedis);
						logger.info(sub + "@[subscribeToChannel::finally] stopSubscription success!");
					} catch (Exception e) {
						logger.error(sub + "@[subscribeToChannel::finally] Exception occurred attempting stopSubscription: " + e.toString());
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}); 
	}

	private boolean isPattern(String channelName) {
		// We consider only the wildcards * and ?
		if (channelName.contains("*") || channelName.contains("?")) {
			patternSubscriptionFlag = true;
			return true;
		}
		else {
			patternSubscriptionFlag = false;
			return false;
		}
	}

	public String setFullyQualifiedChannelName(final String channelPrefixCode, final String channelCode) {
		if (isPattern(channelCode)) {
			patternSubscriptionFlag = true;
			return channelCode;			// already fully qualified name
		}
		//Otherwise concatenate to form the fully qualified channel name
		String channelName = channelPrefixCode.concat(channelCode);
		patternSubscriptionFlag = false;
		return channelName;
	}


	/*@POST
	@Path("/{crisisCode}")
	public void handlePost(HttpServletRequest request,
			HttpServletResponse response,
			@PathParam("crisisCode") String channelCode,
			@QueryParam("callbackName") String callbackName,
			@DefaultValue("-1") @QueryParam("rate") float rate,
			@DefaultValue("0") @QueryParam("duration") String duration) throws IOException, Exception {
		getAIDRStreamData(request, response, channelCode, callbackName, rate, duration);
	}*/

	@GET
	@Path("/channel/{crisisCode}")
	@Produces("application/json")
	public void getAIDRStreamData(@Context HttpServletRequest request,
			@Suspended final AsyncResponse response,
			@PathParam("crisisCode") String channelCode,
			@QueryParam("callbackName") final String callbackName,
			@DefaultValue("-1") @QueryParam("rate") float rate,
			@DefaultValue("0") @QueryParam("duration") String duration)
					throws IOException, ServletException {
		if (channelCode != null) {
			// TODO: Handle client refresh of webpage in same session				
			if (initRedisConnection()) {
				// Get callback function name, if any
				String channel = setFullyQualifiedChannelName(CHANNEL_PREFIX_CODE,channelCode);

				// Now spawn asynchronous response.getWriter() - if coming from a different session
				asyncContext = request.startAsync();
				logger.info("[doGet] asyncContext = " + asyncContext);
				aidrSubscriber = new RedisSubscriber();
				try {
					subscribeToChannel(aidrSubscriber, subscriberJedis, channel);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.error("[doGet] Fatal exception occurred attempting subscription: " + e.toString());
					e.printStackTrace();
					System.exit(1);
				}
				// Set timeout period for async thread
				asyncContext.setTimeout(subscriptionDuration);		// negative --> no timeout
				
				logger.info(this + "@[RedisSubscriber] Step 1: get subscription duration");
				if (duration != null) {
					subscriptionDuration = parseTime(duration);
				}
				logger.info(this + "@[RedisSubscriber] Step 2: get output rate, if any");

				if (rate > 0.0) {			// otherwise, use default rate
					messageRate = rate;	// specified as messages/min (NOTE: upper-bound)
				}
				sleepTime = Math.max(0, Math.round(60 * 1000 / messageRate));		// time to sleep between sends (in msecs)
				logger.info(this + "@[RedisSubscriber] Parameters received: crisisCode:" + channel
						+ ", subscription_duration = " + subscriptionDuration 
						+ ", callback = " + callbackName 
						+ ", rate = " + this.messageRate);
				
				// Listen for errors and timeouts			
				logger.info(this +"@[RedisSubscriber] Step 3: add asyc listener and set timeout period.");
				asyncContext.addListener(this);
				setRunFlag(true);		
				startAsyncThread(asyncContext, callbackName, response);
			}
		} 
		else {
			// No crisisCode provided...
			logger.info("[doGet] In parameter: crisisCode = null, stopSubscription=false");
			/*PrintWriter out = response.getWriter();
			try {
				response.setCharacterEncoding("UTF-8");
				response.setContentType("text/html");

				// Allocate a output writer to write the response message into the network socket
				out.println("<!DOCTYPE html>");
				out.println("<html>");
				out.println("<head><title>Redis HTTP Streaming App</title></head>");
				out.println("<body>");
				out.println("<h1>No CrisisCode Provided! </h1>");
				out.println("<h>Can not initiate REDIS channel subscription!</h>");
				out.println("</body></html>");
			} finally {
				out.flush();
				out.close();  // Always close the output writer
			}*/
		}
		logger.info("[doGet] Reached end-of-function...");
	}

	public void startAsyncThread(AsyncContext asyncCtx, String str, final AsyncResponse response) {
		final AsyncContext asyncContext = asyncCtx;
		final String callbackName = str;
		executorServicePool.submit(new Runnable() {
			
			@Override
			public void run() {
				logger.info(this + "@[run] started async thread execution..., time = " + new Date());
				// Time-out related local variables
				long startTime = new Date().getTime();			// start time of the thread execution
				long currentTime = new Date().getTime(); 
				long lastAccessedTime = currentTime; 
				
				logger.info("[run] asyncContext = " + asyncContext + ", runFlag = " + getRunFlag());
				HttpServletResponse resp = (HttpServletResponse) asyncContext.getResponse();
				logger.info("[run] response = " + resp);
				WriteResponse responseWriter = new WriteResponse(resp,true);
				logger.info("[run] responseWriter = " + responseWriter);
				responseWriter.initWriter("application/json");
				logger.info("[run] responseWriter initialized");
				/*PrintStream responseWriter = null;
				try {
					responseWriter = new PrintStream(response.getOutputStream(), true, "UTF-8");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/

				while (getRunFlag() && !isThreadTimeout(startTime)) {
					// Here we poll a non blocking resource for updates
					logger.info("[run] while loop..., messageList = " + messageList);
					if (messageList != null && !messageList.isEmpty()) {
						if (!error && !timeout) {
							// There are updates, send these to the waiting client
							List<String> latestMsg = null; 
							synchronized (messageList) {
								latestMsg = new ArrayList<String>();
								latestMsg.addAll(messageList);
							}
							JsonDataFormatter taggerOutput = new JsonDataFormatter(callbackName);	// Tagger specific JSONP output formatter
							StringBuilder jsonDataList = taggerOutput.createList(latestMsg, latestMsg.size(), rejectNullFlag);
							int sendCount = taggerOutput.getMessageCount();

							// Send the prepared jsonp data list to client
							logger.info("[doGet] Going to write data, count = " + sendCount);
							//responseWriter.println(jsonDataList.toString());
							//responseWriter.flush();
							responseWriter.writeJsonData(jsonDataList, sendCount);

							synchronized (messageList) {
								// Reset the messageList buffer and cleanup
								messageList.clear();	// remove the sent message from list
								latestMsg.clear();
								latestMsg = null;
								jsonDataList = null;
							}
							lastAccessedTime = new Date().getTime();		// time when message last received from REDIS

							// Now sleep for a short time before going for next message - easy to read on screen
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						else {
							logger.error(this + "@[run] Not sending response because task timed-out or error'ed. error={}, timeout={}, run={}", new Object[] { error, timeout, getRunFlag() });
							setRunFlag(false);
						}
					}
					else {
						// messageList is empty --> no message received 
						// from REDIS. Wait for some more time before giving up.
						currentTime = new Date().getTime();
						long elapsed = currentTime - lastAccessedTime;
						if (elapsed > REDIS_CALLBACK_TIMEOUT) {
							logger.error(this + "@[run::Timeout] Elapsed time = " + elapsed + ", exceeded REDIS timeout = " + REDIS_CALLBACK_TIMEOUT + "sec");
							setRunFlag(false);
						}	
						else {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					// check if the client is up - indirectly through whether the write succeeded or failed
					if (responseWriter.checkError()) {
						logger.info(this + "@[run] Client side error - possible client disconnect..." + new Date());
						setRunFlag(false);
					}
					String testString3 = "{\"filter_level\":\"medium\",\"text\":\"@wongt0n You in Japan now?\",\"contributors\":null,\"geo\":null,\"retweeted\":false,\"in_reply_to_screen_name\":\"wongt0n\",\"truncated\":false,\"entities\":{\"hashtags\":[],\"symbols\":[],\"urls\":[],\"user_mentions\":[{\"id\":123834194,\"indices\":[0,8],\"screen_name\":\"wongt0n\",\"id_str\":\"123834194\",\"name\":\"Lazy Wong~\"}]},\"lang\":\"en\",\"in_reply_to_status_id_str\":\"421190875260014592\",\"id\":421193072316149761,\"aidr\":{\"features\":[{\"words\":[\"\",\"2009\",\"edition\",\"_#mp3\",\"#mp3_#music\",\"408_the\",\"the_end\",\"peas_2009\",\"#mp3\",\"the\",\"edition_black\",\"end_japan\",\"eyed_peas\",\"#music_408\",\"408\",\"japan\",\"japan_edition\",\"black_eyed\",\"#music\",\"eyed\",\"end\",\"black\",\"peas\"],\"type\":\"wordvector\"}],\"crisis_code\":\"japan_chem_explosion\",\"nominal_labels\":[{\"label_name\":\"Not related to crisis\",\"source_id\":289,\"from_human\":false,\"attribute_description\":\"Informative messages enhancing situational awareness, v1.0\",\"label_code\":\"030_not_info\",\"confidence\":0.54,\"label_description\":\"Not related to the crisis\",\"attribute_code\":\"informative_v1\",\"attribute_name\":\"Informative v1.0\"}, {\"label_name\":\"Not related to crisis\",\"source_id\":289,\"from_human\":false,\"attribute_description\":\"Informative messages enhancing situational awareness, v1.0\",\"label_code\":\"030_not_info\",\"confidence\":0.84,\"label_description\":\"Not related to the crisis\",\"attribute_code\":\"informative_v1\",\"attribute_name\":\"Informative v1.0\"}, {\"label_name\":\"Not related to crisis\",\"source_id\":289,\"from_human\":false,\"attribute_description\":\"Informative messages enhancing situational awareness, v1.0\",\"label_code\":\"030_not_info\",\"confidence\":0.20,\"label_description\":\"related to the crisis\",\"attribute_code\":\"informative_v1\",\"attribute_name\":\"Informative v1.0\"}],\"doctype\":\"twitter\",\"crisis_name\":\"Explosion at Japan chemical factory\"},\"source\":\"web\",\"in_reply_to_user_id_str\":\"123834194\",\"favorited\":false,\"in_reply_to_status_id\":421190875260014592,\"retweet_count\":0,\"created_at\":\"Thu Jan 09 08:13:48 +0000 2014\",\"in_reply_to_user_id\":123834194,\"favorite_count\":0,\"id_str\":\"421193072316149761\",\"place\":null,\"user\":{\"location\":\"\",\"default_profile\":false,\"profile_background_tile\":true,\"statuses_count\":10045,\"lang\":\"en\",\"profile_link_color\":\"1212E3\",\"profile_banner_url\":\"https://pbs.twimg.com/profile_banners/457120810/1358321901\",\"id\":457120810,\"following\":null,\"protected\":false,\"favourites_count\":7,\"profile_text_color\":\"E60ED4\",\"contributors_enabled\":false,\"verified\":false,\"description\":\"An ordinary SONE who fall in love with Taeyeon+Jessica+Sunny+Tiffany+Hyoyeon+Yuri+Sooyoung+Yoona+Seohyun = SNSD. Spazzing and sharing is my vacation on twitter.\",\"name\":\"~~\uC18C\uC2DC\uB77C\uC11C \uD589\uBCF5\uD574\uC694~~\",\"profile_sidebar_border_color\":\"FFFFFF\",\"profile_background_color\":\"EDFAFA\",\"created_at\":\"Sat Jan 07 01:51:54 +0000 2012\",\"default_profile_image\":false,\"followers_count\":47,\"geo_enabled\":false,\"profile_image_url_https\":\"https://pbs.twimg.com/profile_images/378800000219857862/9606b10e2dd7d700111f4c5be7384f63_normal.jpeg\",\"profile_background_image_url\":\"http://a0.twimg.com/profile_background_images/889556219/7456374b70ecfea67145b0214f15a988.jpeg\",\"profile_background_image_url_https\":\"https://si0.twimg.com/profile_background_images/889556219/7456374b70ecfea67145b0214f15a988.jpeg\",\"follow_request_sent\":null,\"url\":null,\"utc_offset\":28800,\"time_zone\":\"Kuala Lumpur\",\"notifications\":null,\"profile_use_background_image\":true,\"friends_count\":127,\"profile_sidebar_fill_color\":\"E1D2F5\",\"screen_name\":\"blueagle90\",\"id_str\":\"457120810\",\"profile_image_url\":\"http://pbs.twimg.com/profile_images/378800000219857862/9606b10e2dd7d700111f4c5be7384f63_normal.jpeg\",\"listed_count\":0,\"is_translator\":false},\"coordinates\":null}";
					messageList.add(testString3);
				}	// end-while

				// clean-up and exit thread
				if (!error && !timeout) {
					if (messageList != null) {
						messageList.clear();
						messageList = null;
					}
					if (!responseWriter.checkError()) {
						responseWriter.close();
					}
					try {
						logger.debug(this + "@[run] All done. Attempting stopSubscription... time = " + new Date());
						stopSubscription(subscriberJedis);
						logger.debug(this + "@[run] All done. unSubscription success! ");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error(this + "@[run] Attempting clean-up. Exception occurred attempting stopSubscription: " + e.toString());
						e.printStackTrace();
					}
					logger.debug(this + "@[run] Attempting async complete...");
					// Double check just to ensure graceful exit - Not sure if required!
					if (!error && !timeout) {
						logger.debug(this + "@[run] Async complete...");
						response.resume(asyncContext);
						asyncContext.complete();
					}
				}
				logger.debug(this + "@[run] Forced Async complete...");
				response.resume(asyncContext);
				asyncContext.complete();
			}
		});
	}
	
	// cleanup all threads 
	void shutdownAndAwaitTermination(ExecutorService threadPool) {
		threadPool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
				threadPool.shutdownNow(); 			// Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!threadPool.awaitTermination(1, TimeUnit.SECONDS))
					logger.error("[shutdownAndAwaitTermination] Pool did not terminate");
				System.err.println("[shutdownAndAwaitTermination] Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			threadPool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}


	private long parseTime(String timeString) {
		long duration = 0;
		float value = Float.parseFloat(timeString.substring(0, timeString.length()-1));
		if (value > 0) {
			String suffix = timeString.substring(timeString.length() - 1, timeString.length());
			switch(suffix) {
			case "s":
				duration = Math.min(SUBSCRIPTION_MAX_DURATION, Math.round(value * 1000));
				break;
			case "m":
				duration = Math.min(SUBSCRIPTION_MAX_DURATION, Math.round(value * 1000 * 60));
				break;
			case "h":
				duration = Math.min(SUBSCRIPTION_MAX_DURATION, Math.round(value * 1000 * 60 * 60));
				break;
			case "d":
				duration = Math.min(SUBSCRIPTION_MAX_DURATION, Math.round(value * 1000 * 60 * 60 * 24));
				break;
			}
		}
		return duration;
	}


	///////////////////////////////////
	// Now to implement Async methods
	///////////////////////////////////
	public boolean isThreadTimeout(long startTime) {
		if ((subscriptionDuration > 0) && (new Date().getTime() - startTime) > subscriptionDuration) {
			logger.warn(this + "@[isThreadTimeout] Exceeded Thread timeout = " + subscriptionDuration + "msec");
			return true;
		}
		return false;
	}

	public void setRunFlag(final boolean val) {
		runFlag = val;
		logger.info(this + "@[setRunFlag] flag = " + runFlag);
	}

	public boolean getRunFlag() {
		return runFlag;
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		setRunFlag(false);
		error = true;
		logger.error(this + "@[onError] An error occured while executing task for client ");
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		setRunFlag(false);
		timeout = true;
		logger.warn(this + "@[onTimeout] Timed out while executing task for client");
		asyncContext.complete();
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {}


	//////////////////////////////////////////////////////////////////////////////////////////////////
	// The inner class that handles both Asynchronous Servlet Thread and Redis Threaded Subscription
	//////////////////////////////////////////////////////////////////////////////////////////////////
	private class RedisSubscriber extends JedisPubSub {
		// Redis/Jedis related
		private String channel = redisChannel;

		@SuppressWarnings("unused")
		public RedisSubscriber() {}

		@Override
		public void onMessage(String channel, String message) {
			final int DEFAULT_COUNT = 1;
			synchronized (messageList) {
				if (messageList.size() < DEFAULT_COUNT) messageList.add(message);
			}
			// Also log message for debugging purpose
			logger.debug("[onMessage] Received Redis message to be sent to client");
		}

		@Override
		public void onPMessage(String pattern, String channel, String message) {
			synchronized (messageList) {
				if (messageList.size() <  1) messageList.add(message);
			}
			// Also log message for debugging purpose
			logger.debug("[onPMessage] For pattern: " + pattern + "##channel = " + channel + ", Received Redis message: ");
		}

		@Override
		public void onPSubscribe(String pattern, int subscribedChannels) {
			logger.info("[onPSubscribe] Started pattern subscription...");
		}

		@Override
		public void onPUnsubscribe(String pattern, int subscribedChannels) {
			logger.info("[onPUnsubscribe] Unsubscribed from pattern subscription...");
		}

		@Override
		public void onSubscribe(String channel, int subscribedChannels) {
			logger.info("[onSubscribe] Started channel subscription...");
		}

		@Override
		public void onUnsubscribe(String channel, int subscribedChannels) {
			logger.info(this + "@[onUnsubscribe] Unusbscribed from channel " + channel);
		}
	}
}
