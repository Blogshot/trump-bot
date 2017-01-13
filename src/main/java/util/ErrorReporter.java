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
    this.bugChannel = guild.getChannelByID("269364663349805057");
  }

  public void report(Exception e) {
    report(exceptionToString(e));
  }
  
  public void report(String message) {

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
