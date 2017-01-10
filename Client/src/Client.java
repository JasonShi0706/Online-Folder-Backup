
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static java.nio.file.StandardWatchEventKinds.*;
import filesync.*;


public class Client {
	
	static SynchronisedFile fromFile=null;		
    static ArrayList<String> fileList=new ArrayList<String>();
    static ArrayList<SyncClientThread> syncClientList=new ArrayList<SyncClientThread>();
    static ArrayList<SynchronisedFile> synFilelist=new ArrayList<SynchronisedFile>();
    static ArrayList<Thread> threadList=new ArrayList<Thread>();
	private static String hostName;
	private static String path;
	private static int port;

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		
		CommandClientOption clientOption = new CommandClientOption();
		CmdLineParser parser = new CmdLineParser(clientOption);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		port = clientOption.getPort();
		path = clientOption.getPath();
		hostName = clientOption.getHostname();
		
		
		
		// Connect to local socket on port 4444
//		String path = "/Users/yangguangshi/EcpliseWorkSpace/workspace/DSproject1/client/";
		Path dir = Paths.get(path);
		WatchDir monitor = new WatchDir(dir, false);
		WatchService watcher = FileSystems.getDefault().newWatchService();
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

		// create a thread for watcher
		Runnable runnable = new Runnable() {
			public void run() {
				monitor.processEvents();
			}
		};
		Thread t = new Thread(runnable);
		t.setDaemon(true);
		t.start();
		
		try(Socket socket = new Socket(hostName, port)){    
			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());	
			File f = new File(path);
			File s[] = f.listFiles();
			for(int i=0;i<s.length;i++) {
				fileList.add(s[i].getName());
				fromFile = new SynchronisedFile(path+s[i].getName());
				synFilelist.add(fromFile);			
				SyncClientThread t1 = new SyncClientThread(fromFile, socket);
				syncClientList.add(t1);
				Thread stt = new Thread(t1);	
				stt.setName(fromFile.getFilename());				
				stt.setDaemon(true);
				stt.start();
				threadList.add(stt);				
				fromFile.CheckFileState();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			
			while(true){
				if(monitor.eventname.equals("")){    						     
				}
				else{
					String[] eventArray = monitor.eventname.split(",");
					String[] fileArray = monitor.filename.split(","); 
					
					monitor.eventname = "";
					monitor.filename = "";
					
					for(int i = 0; i< eventArray.length; i++){
						runAction(i, eventArray,fileArray, socket, out);									
					}					
				}						
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(-1);
				}								
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	@SuppressWarnings("deprecation")
	synchronized public static void runAction(int index, String[] eventArray, String[] fileArray, Socket socket, DataOutputStream out ) throws IOException, InterruptedException{
		int i = index;
        File tempFile =new File( fileArray[i].trim());     
		
		if(eventArray[i].equals("ENTRY_MODIFY")){
			try {        
				for(int j = 0;j < synFilelist.size(); j ++){
	                if(!tempFile.getName().equals(".DS_Store")){
						if(synFilelist.get(j).getFilename().equals(fileArray[i]) && fileList.contains(tempFile.getName())){
							synFilelist.get(j).CheckFileState();
						}
	                }
				}			
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		if(eventArray[i].equals("ENTRY_CREATE")){
			try {
				fromFile = new SynchronisedFile(fileArray[i]);
				synFilelist.add(fromFile);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			fileList.add(tempFile.getName());
			
			
			SyncClientThread t = new SyncClientThread(fromFile, socket);
			syncClientList.add(t);
			Thread stt = new Thread(t);	
						
			stt.setName(fromFile.getFilename());
			stt.setDaemon(true);
			stt.start();
			threadList.add(stt);
			fromFile.CheckFileState();
		}
		
		if(eventArray[i].equals("ENTRY_DELETE")){

			
			JSONObject obj=new JSONObject();
			obj.put("Type", "DeleteFile");
			obj.put("FileName", tempFile.getName());
			String msg = obj.toJSONString();
			out.writeUTF(msg);
			
			Thread th = threadList.get(fileList.indexOf(tempFile.getName()));
				
			syncClientList.get(fileList.indexOf(tempFile.getName())).set(true);
			threadList.remove(fileList.indexOf(tempFile.getName()));
			threadList.trimToSize();
			synFilelist.remove(fileList.indexOf(tempFile.getName()));
    		synFilelist.trimToSize();
    		syncClientList.remove(fileList.indexOf(tempFile.getName()));
    		syncClientList.trimToSize();
			fileList.remove(tempFile.getName());
    		fileList.trimToSize();					
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
		
	synchronized public static void check(SynchronisedFile fromFile) throws IOException, InterruptedException{
		fromFile.CheckFileState();
	}

}
