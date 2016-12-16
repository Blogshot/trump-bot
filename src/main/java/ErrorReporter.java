import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.util.MessageBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorReporter {
  
  private IDiscordClient client;
  private Exception e;

  public ErrorReporter(IDiscordClient client, Exception e) {
    this.client = client;
    this.e = e;
  }
  
  public void report() {
    
    try {
      IPrivateChannel privateChannel = client.getOrCreatePMChannel(client.getUserByID("197995146187505665"));
      
      new MessageBuilder(client)
          .withChannel(privateChannel)
          .withContent(exceptionToString(e))
          .build();
      
    } catch (Exception ignored) {    }
  
  
  }
  
  
  private String exceptionToString(Exception e) {
    
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
    
  }
  
}
