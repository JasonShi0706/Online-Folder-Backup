
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import filesync.*;

public class SyncClientThread implements Runnable {
	public boolean terminateLoop = false;
	private SynchronisedFile fromFile; // this would be on the Client
	private Socket socket;
	SyncClientThread(SynchronisedFile ff, Socket ss){
		fromFile=ff;
		socket = ss;
	}
	
	public void set(boolean t){
		terminateLoop = t;
	}
	
	synchronized public void run(){
		try {

			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());					
		
			Instruction inst;
			while((inst=fromFile.NextInstruction())!=null){
				String msg=inst.ToJSON();
				out.writeUTF(msg);
				out.flush();		
				// Blocks until server responds
				String response = null;
				response = in.readUTF();
					//Instruction receivedInst = instFact.FromJSON(response);			
				if (response.equals("BlockUnavailable")){
						Instruction upgraded = new NewBlockInstruction((CopyBlockInstruction)inst);
						String msg2 = upgraded.ToJSON();
						out.writeUTF(msg2);
						out.flush();
				if(inst.Type().equals("EndUpdate") && terminateLoop){
					break;
				}
				}					
			} // get next instruction loop forever
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
