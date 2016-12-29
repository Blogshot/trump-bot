package util;

import com.google.gson.JsonArray;

import java.util.ArrayList;

public class SupportList extends ArrayList<SupportRequest> {

  public JsonArray serialize() {

    JsonArray array = new JsonArray();

    for (SupportRequest request : this) {
      System.out.println("Serializing " + request.ticket_id);
      array.add(request.serialize());
    }

    System.out.println("Serializing " + array.toString() + array.size());

    return array;
  }

  public boolean add(SupportRequest request) {

    // first make sure every ticket-ID is unique
    unifyTicketIDs(request);

    // add to list
    super.add(request);

    return true;
  }

  private void unifyTicketIDs(SupportRequest newRequest) {

    for (SupportRequest existing : this) {

      if (newRequest.ticket_id == existing.ticket_id) {

        // if ID was found, change it and search again until unique
        newRequest.changeTicketID();
        unifyTicketIDs(newRequest);

        break;
      }
    }
  }

  public void removeID(int ticket_id) {

    for (SupportRequest request : this) {
      if (request.ticket_id == ticket_id) {

        this.remove(request);
        break;
      }
    }
  }

  public String createReport() {

    if (this.size() == 0) {
      return "Currently no tickets open.";
    }

    StringBuilder builder = new StringBuilder();

    builder.append("Currently open tickets:\n");

    for (SupportRequest request : this) {

      builder.append("```\n");
      builder.append(request.ticket_id).append(":\n");
      builder.append(request.message).append("\n");
      builder.append("```");
    }

    builder.append(
        "\n\nYou can reply to any of this tickets by typing: `!trump -admin --reply-ticket <ticket-id> <your text here>`");

    return builder.toString();
  }

  public SupportRequest getFromID(int ticket_id) {

    for (SupportRequest request : this) {
      if (request.ticket_id == ticket_id) {
        return request;
      }
    }

    return null;
  }
}
