package listener;

import main.Main;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class ChatListener implements IListener<MessageReceivedEvent> {
  
  private final String helptext =
      "Trump-Bot usage:\n```\n"
          + "!trump   [options]\n"
          + "!clinton [options]\n"
          + "!merkel  [options]\n"
          + "\nOptions:\n\n"
          + "  -h, --help  \tShow this message\n"
          + "  -c <channel>\tSpecify voice channel to join\n"
          + "  -f <pattern>\tSpecify sound file to play. Wildcard: *\n"
          + "  --sounds    \tList all available sound files\n"
          + "  --stats     \tPrint a short summary of statistics\n"
          + "  --leave     \tForce-leave the current channel\n\n"
          + "Examples:\n"
          + "!trump -f big-china.mp3 -c General\n"
          + "!trump -f big-*.mp3 -c General\n"
          + "```\n"
          + "Keeping the server running costs money, please consider donating. Visit https://trump.knotti.org for more info.\n\n"
          + "If you need assistance or want to share feedback, contact Bloggi#7559 or join the support-discord: https://discord.gg/MzfyfTm"
      ;
  
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
      handleDefault(event, politician);
    }
  }
  
  private String[] getArguments(String message) {
    return message.replace(" -", " --").split(" -");
  }
  
  private void handleDefault(MessageReceivedEvent event, Main.Politician politician) {
    
    // get message content
    String message = event.getMessage().getContent().toLowerCase();
    IUser user = event.getAuthor();
    IGuild guild = event.getGuild();
    
  
    message = message.replace("!" + politician.name(), "").trim();
  
    boolean hasArguments = !message.equals("");

    /*
       INITIALISATION

       we have to initialise
       - text-channel to respond with feedback if needed
       - sound-file to play (requires politician)
       - voice-channel to connect to
    */
    
    // init text channel
    IChannel textChannel = event.getMessage().getChannel();
    
    // init sound with random
    ArrayList<URL> soundFiles = new ArrayList<>();
    
    // init voice channel with author's
    IVoiceChannel voiceChannel = null;
    
    // search for channel in guild which contains the user
    for (IVoiceChannel channel : user.getConnectedVoiceChannels()) {
      if (channel.getGuild() == guild) {
        voiceChannel = channel;
      }
    }

    /*
       HANDLE PARAMETERS
    */
    
    // has parameters
    if (hasArguments) {
      
      for (String argument : getArguments(message)) {
        
        // help-message
        if (argument.equals("--help") || argument.equals("-h")) {

          /*
          print help and exit
           */
          printHelp(textChannel);
          return;

          /*
          custom channel
           */
        } else if (argument.startsWith("--channel ") || argument.startsWith("-c ")) {
          
          String value = argument.substring(argument.indexOf(" ") + 1);
          boolean found = false;
          
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
            Main.getInstance()
                .writeMessage(
                    textChannel,
                    "I could not find the voice-channel you specified. Select one of the following:\n"
                        + getVoiceChannelList(guild));
            return;
          }

          /*
          custom sound file
           */
        } else if (argument.startsWith("--file ") || argument.startsWith("-f ")) {
          
          String value = argument.substring(argument.indexOf(" ") + 1);
          
          // make * as wildcard work
          String pattern = ("\\Q" + value + "\\E").replace("*", "\\E.*\\Q");
          
          // get list of matching files
          ArrayList<URL> candidates = getAudio(politician, pattern);
          
          if (candidates.size() == 0) {
            
            // no match found, cant continue. report and exit
            Main.getInstance()
                .writeMessage(
                    textChannel, "I could not find a filename matching the pattern you specified.");
            return;
            
          } else if (candidates.size() > 1) {
            
            // multiple matches
            String matches = fileListToString(candidates);
            
            Main.getInstance()
                .writeMessage(
                    textChannel,
                    "I found multiple audios matching your pattern. Please select one of the following:\n\n"
                        + matches);
            return;
            
          } else {
            
            // set the only match as desired audio
            soundFiles.add(candidates.get(0));
          }

          /*
           list all sounds
          */
        } else if (argument.equals("--sounds")) {
          
          File audio = new File("audio/" + politician.name());
          
          File[] files = audio.listFiles();
          
          String matches = fileListToString(files);
          
          Main.getInstance()
              .writeMessage(textChannel, "Following files are available:\n\n" + matches);
          return;

          /*
           print stats to channel
          */
        } else if (argument.equals("--stats") || argument.equals("--statistics")) {
          printStats(textChannel);
          return;
        } else if (argument.equals("--leave")) {
          Main.getInstance().leaveVoiceChannel(event.getMessage().getGuild());
          return;
        } else {
          // unknown argument, print help and exit
          printHelp("You entered an unknown argument.", textChannel);
          return;
        }
      }
    }
    
    // Abort if bot is busy
    // located here to allow parameters like '-stats' or '-help' to be displayed while bot is active
    
    IVoiceChannel usedChannel = Main.getInstance().isBusyInGuild(event.getMessage().getGuild());
    if (usedChannel != null) {
      Main.getInstance()
          .writeMessage(
              textChannel, "I am currently needed in Channel '" + usedChannel.getName() + "'.");
      return;
    }
    
    if (voiceChannel == null) {
      Main.getInstance()
          .writeMessage(
              event.getMessage().getChannel(),
              "You have to be in a voicechannel (or specify one by adding '-c <name of channel>' to do this.");
      return;
    }
    
    if (soundFiles.isEmpty()) {
      soundFiles.add(getRandomAudio(politician));
    }
    
    Main.getInstance().playAudio(voiceChannel, textChannel, soundFiles, user);
  }
  
  private void printStats(IChannel textChannel) {
    
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
    Date startDate = new Date(Main.getInstance().startedInMillis);
    
    String output =
        "Current stats:\n"
            + "```"
            + "Activated:                \t"
            + Main.getInstance().readStat("played")
            + " times\n"
            + "Online since:             \t"
            + sdf.format(startDate)
            + "\n"
            + "Uptime of current session:\t"
            + Main.getInstance().getUptime()
            + "\n"
            + "Currently active guilds:  \t"
            + Main.getInstance().client.getGuilds().size()
            + "```";
    
    Main.getInstance().writeMessage(textChannel, output);
  }
  
  private String fileListToString(File[] files) {
    // multiple matches
    String matches = "";
    for (File file : files) {
      matches += file.getName() + "\n";
    }
    
    return matches.trim();
  }
  
  private String fileListToString(ArrayList<URL> files) {
    // multiple matches
    String matches = "";
    for (URL file : files) {
      matches += file + "\n";
    }
    
    return matches.trim();
  }
  
  private void printHelp(String intro, IChannel textChannel) {
    Main.getInstance().writeMessage(textChannel, intro + "\n\n" + helptext);
  }
  
  private void printHelp(IChannel textChannel) {
    Main.getInstance().writeMessage(textChannel, helptext);
  }
  
  private String getVoiceChannelList(IGuild guild) {
    
    String list = "";
    
    for (IVoiceChannel channel : guild.getVoiceChannels()) {
      list += channel.getName() + "\n";
    }
    
    return list;
  }
  
  private ArrayList<URL> getAudio(Main.Politician politician, String pattern) {
    
    File audio = new File("audio/" + politician.name());
    
    File[] files = audio.listFiles();
    ArrayList<URL> candidates = new ArrayList<>();
    
    // iterate through available files to find matching ones
    if (files != null) {
      for (File candidate : files) {
        
        // get matches
        if (candidate.getName().matches(pattern)) {
          try {
            candidates.add(candidate.toURI().toURL());
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
        }
      }
    }
    
    return candidates;
  }
  
  private URL getRandomAudio(Main.Politician politician) {
    
    // set path for selected politician
    File audio = new File("audio/" + politician.name());
    
    URL soundFile = null;
    
    // pick a random audio
    File[] files = audio.listFiles();
    
    if (files != null && files.length > 0) {
      int random = new Random().nextInt(files.length);
      
      try {
        soundFile = files[random].toURI().toURL();
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
    
    return soundFile;
  }
}
