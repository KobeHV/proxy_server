package proxy_server;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketProxy {
	
	static final int listenPort=10240;
 
	public static void main(String[] args) throws Exception {
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		@SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(listenPort);
		final ExecutorService tpe=Executors.newCachedThreadPool();
		System.out.println("Proxy Server Start At "+sdf.format(new Date()));
		System.out.println("listening port:"+listenPort+"����");
		System.out.println();
		System.out.println();
	
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				socket.setKeepAlive(true);
				//���������б��ȴ�����
				tpe.execute(new ProxyTask(socket));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}

