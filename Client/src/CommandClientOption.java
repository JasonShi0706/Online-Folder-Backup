import org.kohsuke.args4j.Option;


public class CommandClientOption {
	@Option(name = "-f", required = true)  
	private String out;   
	
	@Option(name = "-h", required = true)  
	private String hostname;   
	
	@Option(name="-p",required=false)
	private int port=4444;
	
	public int getPort(){
		return port;
	}
	public String getPath(){
		return out;
	}
	public String getHostname(){
		return hostname;
	}
}
