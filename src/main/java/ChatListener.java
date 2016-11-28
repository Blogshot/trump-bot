import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.util.ArrayList;
import java.util.List;

public class ChatListener implements IListener<MessageReceivedEvent> { // The event type in IListener<> can be any class which extends Event


    @Override
    public void handle(MessageReceivedEvent event) { // This is called when the ReadyEvent is dispatched

        // get message content
        String message = event.getMessage().getContent().toLowerCase();

        IChannel textChannel = event.getMessage().getChannel();
        IGuild guild = event.getMessage().getGuild();

        Main.Politician politician;

        if (message.startsWith("!trump") || message.startsWith("!merkel")) {

            boolean hasArguments = false;

            if (message.startsWith("!trump")) {
                politician = Main.Politician.trump;

                hasArguments = message.length() > "!trump".length();

            } else {
                politician = Main.Politician.merkel;

                hasArguments = message.length() > "!merkel".length();
            }

            // Abort if busy
            if (Main.currentVoiceChannel != null) {

                if (politician == Main.Politician.trump) {
                    Main.writeMessage(textChannel,
                            "I am a very busy man, and currently I am needed somewhere else.");
                } else {
                    Main.writeMessage(textChannel,
                            "I am a very busy women, and currently I am needed somewhere else.");
                }
                return;
            }

            // trim string
            message = message.trim();

            IVoiceChannel voiceChannel = null;

            // has parameters
            if (hasArguments) {

                for (String argument : getArguments(message, event)) {

                    // help-message
                    if (argument.equals("-help") || argument.equals("-h")) {

                        // print help and exit
                        printHelp(textChannel);
                        return;

                    } else if (argument.startsWith("-c:")) {

                        String value = argument.substring(argument.indexOf("-c:") + 3);
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
                            "Look, you have to be in a voicechannel (or specify one by adding '-c:<name of channel>' to do this.");
                    return;
                }

            }

            Main.currentVoiceChannel = voiceChannel;
            Main.playAudio(voiceChannel, textChannel, politician);
        }
    }

    private void printHelp(IChannel textChannel) {
        Main.writeMessage(textChannel,
                "Trump-Bot usage:\n" +
                        "!trump [options]\n" +
                        "!merkel [options]\n\n" +
                        "Options:\n\n" +
                        "\t'-help','-h'\t\tShow this message\n" +
                        "\t'-c:<channel>'\t\tSpecify voice channel to join"
        );

    }

    private String getVoiceChannelList(IGuild guild) {

        String list = "";

        for (IVoiceChannel channel : guild.getVoiceChannels()) {
            list += channel.getName() + "\n";
        }

        return list;

    }
  
  private ArrayList<String> getArguments(String message, MessageReceivedEvent event) {
            /*
          get argument-string
          !trump /c:test -f=china
          ->
          -c=test -f=china

         */
    
    ArrayList<String> args = new ArrayList<>();
    
    
    int mark = message.length();
    
    for (int i = message.length() - 1; i >= 0; i--) {
      
      try {
        
        // if there is " -"
        if (message.charAt(i) == '-' && message.charAt(i - 1) == ' ') {
          args.add(message.substring(i, mark));
          
          // set mark at space
          mark = i - 1;
        }
        
      } catch (IndexOutOfBoundsException e) {
        Main.writeMessage(event.getMessage().getChannel(),
            "Error while parsing arguments: " + e.getMessage() + "\n\n" + "i=" + i + "\nmark=" + mark);
      }
    }
    
    
    return args;
  }
}