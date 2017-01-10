import org.kohsuke.args4j.Option;


public class CommandServerOption {
	@Option(name = "-f", required = true)  
	private String path;   
	
	@Option(name="-p",required=false)
	private int port=4444;
	
	public int getPort(){
		return port;
	}
	public String getPath(){
		return path;
	}
}
