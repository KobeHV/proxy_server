package proxy_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ���ͻ��˷��͹���������ת��������ķ������ˣ��������������ص�����ת�����ͻ���
 *
 */
public class ProxyTask implements Runnable {
	private Socket socketIn;
	private Socket socketOut;
	
	private long totalUpload=0l;//�ܼ����б�����
	private long totalDownload=0l;//�ܼ����б�����
 
	public ProxyTask(Socket socket) {
		this.socketIn = socket;
	}
	
	private static final SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	/** �����ӵ�����ķ����� */
	private static final String AUTHORED = "HTTP/1.1 200 Connection established\r\n\r\n";
	/** �������½ʧ��(��Ӧ����ʱ���漰��½����) */
	//private static final String UNAUTHORED="HTTP/1.1 407 Unauthorized\r\n\r\n";
	/** �ڲ����� */
	private static final String SERVERERROR = "HTTP/1.1 500 Connection FAILED\r\n\r\n";
	
	@Override
	public void run() {
		
		StringBuilder builder=new StringBuilder();
		try {
			builder.append("\r\n").append("Request Time  ��" + sdf.format(new Date()));
			
			InputStream isIn = socketIn.getInputStream();
			OutputStream osIn = socketIn.getOutputStream();
			//�ӿͻ����������ж�ȡͷ����������������Ͷ˿�
			HttpHeader header = HttpHeader.readHeader(isIn);
			
			//���������־��Ϣ
			builder.append("\r\n").append("From    Host  ��" + socketIn.getInetAddress());//�����׽������ӵĵ�ַ
			builder.append("\r\n").append("From    Port  ��" + socketIn.getLocalPort());//���ش��׽��ְ󶨵��ı��ض˿�
			builder.append("\r\n").append("Proxy   Method��" + header.getMethod());
			builder.append("\r\n").append("Request Host  ��" + header.getHost());
			builder.append("\r\n").append("Request Port  ��" + header.getPort());
			
			//���û�����������ַ�Ͷ˿ڣ��򷵻ش�����Ϣ
			if (header.getHost() == null || header.getPort() == null) {
				osIn.write(SERVERERROR.getBytes());
				osIn.flush();
				return ;
			}
			
			// ���������Ͷ˿�
			socketOut = new Socket(header.getHost(), Integer.parseInt(header.getPort()));
			socketOut.setKeepAlive(true);
			InputStream isOut = socketOut.getInputStream();
			OutputStream osOut = socketOut.getOutputStream();
			//�¿�һ���߳̽����ص�����ת�����ͻ���,���л�����⣬��û������ԭ��
			Thread ot = new DataSendThread(isOut, osIn);
			ot.start();
			if (header.getMethod().equals(HttpHeader.METHOD_CONNECT)) {
				// ������ͨ�źŷ��ظ�����ҳ��
				osIn.write(AUTHORED.getBytes());
				osIn.flush();
			}else{
				//http������Ҫ������ͷ��Ҳת����ȥ
				byte[] headerData=header.toString().getBytes();
				totalUpload+=headerData.length;
				osOut.write(headerData);
				osOut.flush();
			}
			//��ȡ�ͻ����������������ת����������
			readForwardDate(isIn, osOut);
			//�ȴ���ͻ���ת�����߳̽���
			ot.join();
		} catch (Exception e) {
			e.printStackTrace();
			if(!socketIn.isOutputShutdown()){
				//��������Է��ش���״̬�Ļ��������ڲ�����
				try {
					socketIn.getOutputStream().write(SERVERERROR.getBytes());
				} catch (IOException e1) {}
			}
		} finally {
			try {
				if (socketIn != null) {
					socketIn.close();
				}
			} catch (IOException e) {}
			if (socketOut != null) {
				try {
					socketOut.close();
				} catch (IOException e) {}
			}
			//��¼��������������������ʱ�䲢��ӡ
			builder.append("\r\n").append("Up    Bytes  ��" + totalUpload);
			builder.append("\r\n").append("Down  Bytes  ��" + totalDownload);
			builder.append("\r\n").append("Closed Time  ��" + sdf.format(new Date()));
			builder.append("\r\n");
			logRequestMsg(builder.toString());
		}	
	}
	
	/**
	 * ������߳̾�������־������
	 * @param msg
	 */
	private synchronized void logRequestMsg(String msg){
		System.out.println(msg);
	}
 
	/**
	 * ��ȡ�ͻ��˷��͹��������ݣ����͸���������
	 * 
	 * @param isIn
	 * @param osOut
	 */
	private void readForwardDate(InputStream isIn, OutputStream osOut) {
		byte[] buffer = new byte[4096];
		try {
			int len;
			while ((len = isIn.read(buffer)) != -1) {
				if (len > 0) {
					osOut.write(buffer, 0, len);
					osOut.flush();
				}
				totalUpload+=len;
				if (socketIn.isClosed() || socketOut.isClosed()) {
					break;
				}
			}
		} catch (Exception e) {
			try {
				socketOut.close();// ���Թر�Զ�̷��������ӣ��ж�ת���̵߳Ķ�����״̬
			} catch (IOException e1) {}
		}
	}
 
	/**
	 * ���������˷��ص�����ת�����ͻ���
	 * 
	 * @param isOut
	 * @param osIn
	 */
	class DataSendThread extends Thread {
		private InputStream isOut;
		private OutputStream osIn;
 
		DataSendThread(InputStream isOut, OutputStream osIn) {
			this.isOut = isOut;
			this.osIn = osIn;
		}
 
		@Override
		public void run() {
			byte[] buffer = new byte[4096];
			try {
				int len;
				while ((len = isOut.read(buffer)) != -1) {
					if (len > 0) {
						// logData(buffer, 0, len);
						osIn.write(buffer, 0, len);
						osIn.flush();
						totalDownload+=len;
					}
					if (socketIn.isOutputShutdown() || socketOut.isClosed()) {
						break;
					}
				}
			} catch (Exception e) {}
		}
	}
 
}
