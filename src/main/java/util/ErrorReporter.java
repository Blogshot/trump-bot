package util;

import main.Main;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.util.MessageBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorReporter {

  private final IDiscordClient client;

  public ErrorReporter(IDiscordClient client) {
    this.client = client;
  }

  public void report(Exception e) {
    send(exceptionToString(e));
  }

  public void report(String message) {
    send(message);
  }

  private void send(String message) {

    try {
      IPrivateChannel privateChannel =
          client.getOrCreatePMChannel(client.getUserByID(Main.getInstance().adminID));

      new MessageBuilder(client).withChannel(privateChannel).withContent(message).build();

    } catch (Exception ignored) {
    }
  }

  private String exceptionToString(Exception e) {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }
}
