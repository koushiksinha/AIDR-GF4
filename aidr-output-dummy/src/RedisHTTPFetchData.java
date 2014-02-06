package qa.qcri.aidr.output.fetch;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

import qa.qcri.aidr.output.fetch.RedisHTTPGetData.RedisSubscriber;

/**
 * Servlet implementation class RedisHTTPFetchData
 */
@WebServlet(description = "Supplies client with last N data elements from buffer", urlPatterns = { "/RedisHTTPFetchData" })
public class RedisHTTPFetchData extends HttpServlet {
	private static final long serialVersionUID = 1L;
    

	void initializeChannelBuffers() {
		// spawn a thread that will subscribe to aidr_predict.* and create new circular buffers
		// for every new channel it detects
		initRedisSubscription(redisHost, redisPort);

		// Get callback function name, if any
		String channel = "aidr_predict.*";

		aidrSubscriber = new RedisSubscriber(subscriberJedis, channel);
		System.out.println("[doGet] aidrSubscriber = " + aidrSubscriber);
		try {
			subscribeToChannel(aidrSubscriber, subscriberJedis, channel);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("[doGet] Fatal exception occurred attempting subscription: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
		executorServicePool.execute(aidrSubscriber);	// alternatively, use: asyncContext.start(aidrSubscriber);
	}
	
	
	private static class ThreadedBufferingClass implements Runnable {

		private static String receivedCommonMessage = null;		// will be modified in onPMessage() listener
		@Override
		public void run() {
			
			
			// a circular buffer per new channel 
			Buffer channelMessageBuffer =    BufferUtils.synchronizedBuffer(new CircularFifoBuffer(MAX_MESSAGES_COUNT));
		}  
	
	
	}
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RedisHTTPFetchData() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see Servlet#destroy()
	 */
	public void destroy() {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
