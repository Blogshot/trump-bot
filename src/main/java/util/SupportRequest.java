package util;

import com.google.gson.JsonObject;
import main.Main;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

public class SupportRequest {

  public int ticket_id = -1;
  public String user_id;
  public String message;

  public SupportRequest(String id, String message) {
    this.user_id = id;
    this.message = message;
    changeTicketID();
  }

  public SupportRequest(JsonObject obj) {
    this.ticket_id = obj.get("ticket_id").getAsInt();
    this.user_id = obj.get("user_id").getAsString();
    this.message = obj.get("message").getAsString();
  }

  JsonObject serialize() {
    JsonObject obj = new JsonObject();

    obj.addProperty("ticket_id", ticket_id);
    obj.addProperty("user_id", user_id);
    obj.addProperty("message", message);

    return obj;
  }

  public void reply(String message, IDiscordClient client, boolean admin) {
    for (IUser candidate : client.getUsers()) {

      if (candidate.getID().equals(user_id)) {

        try {
          IPrivateChannel privateChannel = candidate.getOrCreatePMChannel();

          // if the admin wrote the message
          if (admin) {
            message = "Admin wrote: " + message;

            this.message += "\n\n" + message;

            message +=
                "\n\nYou can reply to this message by typing: `!trump -ticket:"
                    + ticket_id
                    + " <your text here>`";
          } else {
            message = "User wrote: " + message;

            this.message += "\n\n" + message;

            message +=
                "\n\nYou can reply to this message by typing: `!trump -admin --reply-ticket "
                    + ticket_id
                    + " <your text here>`";
          }

          new MessageBuilder(client).withContent(message).withChannel(privateChannel).build();

          Main.getInstance().saveSupportRequests();

        } catch (DiscordException | RateLimitException | MissingPermissionsException e) {
          e.printStackTrace();
        }
      }
    }
  }

  void changeTicketID() {
    this.ticket_id++;
  }
}
