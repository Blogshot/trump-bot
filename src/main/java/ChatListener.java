import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.util.List;

public class ChatListener implements IListener<MessageReceivedEvent> { // The event type in IListener<> can be any class which extends Event
  
  
  @Override
  public void handle(MessageReceivedEvent event) { // This is called when the ReadyEvent is dispatched
  
    // get message content
    String message = event.getMessage().getContent().toLowerCase();
  
    IChannel textChannel = event.getMessage().getChannel();
    IGuild guild = event.getMessage().getGuild();
  
  
    if (message.startsWith("!trump")) {
            
      // Abort if busy
      if (Main.currentVoiceChannel != null) {
  
        Main.writeMessage(textChannel,
            "I am a very busy man, and currently I am needed somewhere else.");
        return;
      }
      
      // trim string
      message = message.trim();
      
      IVoiceChannel voiceChannel = null;
    
      // has parameters
      if (message.length() > "!trump".length()) {
  
        for (String argument : getArguments(message)) {
    
          // help-message
          if (argument.equals("-help") || argument.equals("-h")) {
            
            // print help and exit
            printHelp(textChannel);
            return;
            
          } else if (argument.startsWith("-c=")) {
  
            String value = argument.substring(argument.indexOf("-c=") + 3);
            boolean found = false;
  
            // iterate through available channels to find the specified channel
            for (IVoiceChannel candidate : guild.getVoiceChannels()) {
  
              // set voicechannel
              if (candidate.getName().toLowerCase().equals(value)) {
                voiceChannel = candidate;
                found = true;
              }
  
            }
  
            // if no channel was found
            if (!found) {
  
              // invalid channel, report and exit
              Main.writeMessage(textChannel,
                  "I could not find the voice-channel you specified. Select one of the following:\n" +
                      getVoiceChannelList(guild));
              return;
            }
          } else {
            // invalid argument, print help and exit
            printHelp(textChannel);
            return;
          }
    
        }
      } else {
        
        // no parameters, default behaviour
        List<IVoiceChannel> voiceChannels = event.getMessage().getAuthor().getConnectedVoiceChannels();
      
        // Current voicechannel of author
        if (voiceChannels.size() > 0) {
          voiceChannel = voiceChannels.get(0);
        }
              
        if (voiceChannel == null) {
          Main.writeMessage(event.getMessage().getChannel(),
              "Look, you have to be in a voicechannel (or specify one by adding '-c=<name of channel>' to do this.");
          return;
        }
        
      }
  
      Main.currentVoiceChannel = voiceChannel;
      Main.playTrump(voiceChannel, textChannel);
    }
  }
  
  private void printHelp(IChannel textChannel) {
    Main.writeMessage(textChannel,
        "Trump-Bot usage:\n" +
            "!trump [options]\n\n" +
            "Options:\n\n" +
            "\t'-help','-h'\tShow this message\n" +
            "\t'-c=<channel>'\tSpecify voice channel to join"
    );
    
  }
  
  private String getVoiceChannelList(IGuild guild) {
    
    String list = "";
    
    for (IVoiceChannel channel : guild.getVoiceChannels()) {
      list += channel.getName() + "\n";
    }
    
    return list;
    
  }
    
  private String[] getArguments(String message) {
            /*
          get argument-string
          !trump -c=test -f=china
          ->
          -c=test -f=china
        
         */
    String arguments = message.substring(message.indexOf(" ") + 1);
    
    return arguments.split(" ");
  }
}