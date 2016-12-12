import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ChatListener implements IListener<MessageReceivedEvent> {
  
  @Override
  public void handle(MessageReceivedEvent event) {
    
    // get message content
    String message = event.getMessage().getContent().toLowerCase();
    
    Main.Politician politician = null;
    
    // set politician
    for (Main.Politician value : Main.Politician.values()) {
      if (message.startsWith("!" + value.name())) {
        politician = value;
      }
    }
    
    if (politician != null) {
      
      // trim string
      message = message.trim();
      
      boolean hasArguments = message.length() > ("!" + politician.name()).length();
      
      /*
          INITIALISATION
          
          we have to initialise
          - text-channel to respond with feedback if needed
          - sound-file to play (requires politician)
          - voice-channel to connect to
       */
      
      // init text channel
      IChannel textChannel = event.getMessage().getChannel();
      
      // Abort if bot is busy
      IVoiceChannel usedChannel = Main.getInstance().isBusyInGuild(event.getMessage().getGuild());
      if (usedChannel != null) {
        Main.getInstance().writeMessage(textChannel,
            "I am currently needed in Channel '" + usedChannel.getName() + "'.");
        return;
      }
      
      // init sound with random
      File soundFile = getRandomAudio(politician);
      
      // init voice channel with author's
      IVoiceChannel voiceChannel = null;
      List<IVoiceChannel> voiceChannels = event.getMessage().getAuthor().getConnectedVoiceChannels();
      
      if (voiceChannels.size() > 0) {
        voiceChannel = voiceChannels.get(0);
      }
      
      
      /*
          HANDLE PARAMETERS
       */
      
      // has parameters
      if (hasArguments) {
        
        for (String argument : getArguments(message, event)) {
          
          // help-message
          if (argument.equals("-help") || argument.equals("-h")) {
            
            /*
             print help and exit
              */
            printHelp(textChannel);
            return;
            
            /*
             custom channel
              */
          } else if (argument.startsWith("-c:")) {
            
            String value = argument.substring(argument.indexOf("-c:") + 3);
            boolean found = false;
            
            IGuild guild = event.getMessage().getGuild();
            
            // iterate through available channels to find the specified channel
            for (IVoiceChannel candidate : guild.getVoiceChannels()) {
              
              // set voicechannel
              if (candidate.getName().toLowerCase().equals(value)) {
                voiceChannel = candidate;
                found = true;
              }
              
            }
            
            /*
             if no channel was found
              */
            if (!found) {
              
              // invalid channel, report and exit
              Main.getInstance().writeMessage(textChannel,
                  "I could not find the voice-channel you specified. Select one of the following:\n" +
                      getVoiceChannelList(guild));
              return;
            }
            
            /*
             custom sound file
              */
          } else if (argument.startsWith("-f:")) {
            
            String value = argument.substring(argument.indexOf("-f:") + 3);
            
            // make * as wildcard work
            String pattern = ("\\Q" + value + "\\E").replace("*", "\\E.*\\Q");
            
            // get list of matching files
            ArrayList<File> candidates = getAudio(politician, pattern);
            
            if (candidates.size() == 0) {
              
              // no match found, cant continue. report and exit
              Main.getInstance().writeMessage(textChannel,
                  "I could not find a filename matching the pattern you specified."
              );
              return;
              
            } else if (candidates.size() > 1) {
              
              // multiple matches
              String matches = fileListToString(candidates);
              
              Main.getInstance().writeMessage(textChannel,
                  "I found multiple audios matching your pattern. Please select one of the following:\n\n" +
                      matches
              );
              return;
              
            } else {
              
              // set the only match as desired audio
              soundFile = candidates.get(0);
              
            }
            
            /*
              list all sounds
             */
          } else if (argument.equals("-sounds")) {
            
            File audio = new File("audio/" + politician.name());
            
            File[] files = audio.listFiles();
            
            String matches = fileListToString(files);
            
            Main.getInstance().writeMessage(textChannel,
                "Following files are available:\n\n" +
                    matches
            );
            return;
            
            /*
              print stats to channel
             */
          } else if (argument.equals("-stats")) {
            printStats(textChannel);
            return;
          } else {
            // unknown argument, print help and exit
            printHelp(textChannel);
            return;
          }
          
        }
      }
      
      if (voiceChannel == null) {
        Main.getInstance().writeMessage(event.getMessage().getChannel(),
            "Look, you have to be in a voicechannel (or specify one by adding '-c:<name of channel>' to do this.");
        return;
      }
      
      Main.getInstance().playAudio(voiceChannel, textChannel, soundFile);
      
    }
  }
  
  private void printStats(IChannel textChannel) {
    
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
    Date startDate = new Date(Main.getInstance().startedInMillis);
    
    String output =
        "Current stats:\n\n" +
            "Activated: " + readStat("played") + " times\n" +
            "Online since: " + sdf.format(startDate) + "\n" +
            "Uptime of current session: " + Main.getInstance().getUptime() + "\n" +
            "Currently active guilds: " + readStat("guildCount");
    
    Main.getInstance().writeMessage(textChannel,
        output
    );
  }
  
  private String readStat(String stat) {
    
    return Main.getInstance().getStatsAsJson().get(stat).getAsString();
    
  }
  
  private String fileListToString(File[] files) {
    // multiple matches
    String matches = "";
    for (File file : files) {
      matches += file.getName() + "\n";
    }
    
    return matches.trim();
  }
  
  private String fileListToString(ArrayList<File> files) {
    // multiple matches
    String matches = "";
    for (File file : files) {
      matches += file.getName() + "\n";
    }
    
    return matches.trim();
  }
  
  private void printHelp(IChannel textChannel) {
    Main.getInstance().writeMessage(textChannel,
        "Trump-Bot usage:\n" +
            "!trump [options]\n" +
            "!clinton [options]\n" +
            "!merkel [options]\n" +
            "!airhorn, !ah [options]\n" +
            "\nOptions:\n\n" +
            "\t'-help','-h'\t\t\t\t\t\tShow this message\n" +
            "\t'-c:<channel>'\t\t\t\tSpecify voice channel to join\n" +
            "\t'-f:<pattern-of-file>'\tSpecify sound file to play. Wildcards (\\*) are supported.\n" +
            "\t'-sounds'\t\t\t\t\t\t\tList all available sound files. Equals '-f:\\*'"
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
        Main.getInstance().writeMessage(event.getMessage().getChannel(),
            "Error while parsing arguments: " + e.getMessage() + "\n\n" + "i=" + i + "\nmark=" + mark);
      }
    }
    
    
    return args;
  }
  
  private ArrayList<File> getAudio(Main.Politician politician, String pattern) {
    
    File audio = new File("audio/" + politician.name());
    
    File[] files = audio.listFiles();
    ArrayList<File> candidates = new ArrayList<>();
    
    // iterate through available files to find matching ones
    if (files != null) {
      for (File candidate : files) {
        
        // get matches
        if (candidate.getName().matches(pattern)) {
          candidates.add(candidate);
        }
        
      }
    }
    
    return candidates;
  }
  
  private File getRandomAudio(Main.Politician politician) {
    
    // set path for selected politician
    File audio = new File("audio/" + politician.name());
    
    File soundFile = null;
    
    // pick a random audio
    File[] files = audio.listFiles();
    
    if (files != null && files.length > 0) {
      int random = new Random().nextInt(files.length);
      
      soundFile = files[random];
    }
    
    return soundFile;
  }
}