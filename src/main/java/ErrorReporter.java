import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.util.MessageBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

class ErrorReporter {

  private IDiscordClient client;

  ErrorReporter(IDiscordClient client) {
    this.client = client;
  }

  void report(Exception e) {
    send(exceptionToString(e));
  }

  void report(String message) {
    send(message);
  }

  private void send(String message) {

    try {
      IPrivateChannel privateChannel =
          client.getOrCreatePMChannel(client.getUserByID("197995146187505665"));

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
