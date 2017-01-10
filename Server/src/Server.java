
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import filesync.*;


public class Server {
	private static int port;
    private static String path;
    
	public static void main(String[] args) throws IOException {
		
		SynchronisedFile synfile = null;
		SynchronisedFile currentSynfile = null;
		SynchronisedFile toFile = null;
		String filename = "";
		String tempfilename = "";
		String deletefilename = "";
		String tempdeletefilename = "";
	    ArrayList<String> fileList=new ArrayList<String>();
	    ArrayList<SynchronisedFile> synFilelist=new ArrayList<SynchronisedFile>();
		String path = "/Users/yangguangshi/EcpliseWorkSpace/workspace/DSproject1/serverfile/";
		
		CommandServerOption serverOption=new CommandServerOption();
    	CmdLineParser parser = new CmdLineParser(serverOption);
    	try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	port = serverOption.getPort();
    	path = serverOption.getPath();
    	
		try (ServerSocket server = new ServerSocket(path)) {
				Socket socket = server.accept();
			    InstructionFactory instFact=new InstructionFactory();

				// Get the in/out streams of the sockets
				DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());			
				
				// In infinite loop - keeps reading and sending to client
				while(true) {

					// Blocks until client sends a message
					String message = in.readUTF();
					JsonObject jsOb = new Gson().fromJson(message, JsonObject.class);
					String instructionType = "";
					instructionType = jsOb.get("Type").toString();
					if(instructionType.substring(1, instructionType.length()-1).equals("DeleteFile")){
						tempdeletefilename = jsOb.get("FileName").toString();
						deletefilename = tempdeletefilename.substring(1, tempdeletefilename.length()-1);
				    	File deletefile = new File(path+deletefilename);

				    	if(deletefile.exists()){
		    		
				    	   deletefile.delete();
				    	}
				    	
				    	if(fileList.contains(deletefilename)){
				    		synFilelist.remove(fileList.indexOf(deletefilename));
				    		synFilelist.trimToSize();
				    		fileList.remove(deletefilename);
				    		fileList.trimToSize();
				    	}
				    	continue;
					}
					
					else{
					    if (instructionType.substring(1, instructionType.length()-1).equals("StartUpdate")){
					    	tempfilename = jsOb.get("FileName").toString();
					    	filename = tempfilename.substring(1, tempfilename.length()-1);
					    }
						
					    if(!fileList.contains(filename)){
					    	File file = new File(path+filename);
					    	file.createNewFile();
					    	fileList.add(filename);
					    	synfile =new SynchronisedFile(path+filename);
					    	synFilelist.add(synfile);
					    }					    
					    
						Instruction receivedInst = instFact.FromJSON(message);
						int i = 1;
						try {
							currentSynfile = synFilelist.get(fileList.indexOf(filename));
							currentSynfile.ProcessInstruction(receivedInst);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(-1); // just die at the first sign of trouble
						} catch (BlockUnavailableException e) {
							// The server does not have the bytes referred to by the block hash.
							try {
								
								out.writeUTF(String.format("BlockUnavailable"));
								out.flush();
								String msg2 = in.readUTF();
								Instruction receivedInst2 = instFact.FromJSON(msg2);
								currentSynfile.ProcessInstruction(receivedInst2);
								i = 2;
							} catch (IOException e1) {
								e1.printStackTrace();
								System.exit(-1);
							} catch (BlockUnavailableException e1) {
								assert(false); // a NewBlockInstruction can never throw this exception
							}
						}
						if(i == 1){
						out.writeUTF(String.format("Successful"));
						out.flush();
						}
					}

				}
			
		}
	}

}
