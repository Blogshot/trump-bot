import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.util.List;

public class ChatListener implements IListener<MessageReceivedEvent> { // The event type in IListener<> can be any class which extends Event
  
  
  @Override
  public void handle(MessageReceivedEvent event) { // This is called when the ReadyEvent is dispatched
    
    // get message content
    String message = event.getMessage().getContent().toLowerCase(); // Gets the message from the event object NOTE: This is not the content of the message, but the object itself
    
    IChannel textChannel = event.getMessage().getChannel();
        
    
    if (message.equals("!trump")) {
  
      List<IVoiceChannel> voiceChannels = event.getMessage().getAuthor().getConnectedVoiceChannels();
  
      if (voiceChannels.size() <= 0) {
        Main.writeMessage(event.getMessage().getChannel(),
            "Look, you have to be in a voice channel to do this. It's that easy!");
        return;
      }
  
      // Current voicechannel of author
      IVoiceChannel voiceChannel = voiceChannels.get(0);
  
      // Abort if busy
      if (Main.currentVoiceChannel != null) {
        
        Main.writeMessage(textChannel,
            "I am a very busy man, and currently I am needed somewhere else.");
        return;
      }
      
      Main.currentVoiceChannel = voiceChannel;
      Main.playTrump(voiceChannel, textChannel);
      
    }
  }
  
  
}