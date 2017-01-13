package util;

import main.Main;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.MessageBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorReporter {

  private final IDiscordClient client;

  public ErrorReporter(IDiscordClient client) {
    this.client = client;
  }

  public void report(Exception e) {
    report(exceptionToString(e));
  }
  
  public void report(String message) {

    try {
      new MessageBuilder(client)
          .withChannel(Main.getInstance().bugChannel)
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
