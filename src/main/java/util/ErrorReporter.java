package util;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.MessageBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorReporter {

  private final IDiscordClient client;
  private IChannel bugChannel;
  
  public ErrorReporter(IDiscordClient client) {
    
    this.client = client;
  
    // get bug-channel
    IGuild guild = client.getGuildByID("269206577418993664");
    
    // TODO remove this after https://github.com/austinv11/Discord4J/issues/195 is fixed
    /*
    if bot isn't ready yet, set bugChannel to null. Temporary workaround.
    */
    if (guild != null) {
      this.bugChannel = guild.getChannelByID("269364663349805057");
    }
  }

  public void report(Exception e) {
    report(exceptionToString(e));
  }
  
  public void report(String message) {
  
    // TODO remove this after https://github.com/austinv11/Discord4J/issues/195 is fixed
    /*
      ignore if bugChannel == null
       */
    if (bugChannel == null) {
      return;
    }
  
    try {
      new MessageBuilder(client)
          .withChannel(bugChannel)
          .withContent(message)
          .build();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String exceptionToString(Exception e) {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }
}
