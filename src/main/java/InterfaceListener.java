import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.util.List;

public class InterfaceListener implements IListener<MessageReceivedEvent> { // The event type in IListener<> can be any class which extends Event
  
  
  @Override
  public void handle(MessageReceivedEvent event) { // This is called when the ReadyEvent is dispatched
    
    // get message content
    String message = event.getMessage().getContent().toLowerCase(); // Gets the message from the event object NOTE: This is not the content of the message, but the object itself
    
    IChannel textChannel = event.getMessage().getChannel();
    
    List<IVoiceChannel> voiceChannels = event.getMessage().getAuthor().getConnectedVoiceChannels();
    
    if (voiceChannels.size() <= 0) {
      Main.writeMessage(event.getMessage().getChannel(),
          "You have to be in a voice channel to do this.");
      return;
    }
    
    IVoiceChannel voiceChannel = voiceChannels.get(0);
    
    if (message.equals("!trump")) {
      
      // Abort if busy
      if (Main.isBusy) {
        
        Main.writeMessage(textChannel,
            "I am busy already, try again in a moment! :)");
        return;
        
      }
      
      Main.playTrump(voiceChannel, textChannel);
      
    }
  }
  
  
}