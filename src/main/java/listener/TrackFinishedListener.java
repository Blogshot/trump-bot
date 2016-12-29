package listener;

import main.Main;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.audio.events.TrackFinishEvent;

public class TrackFinishedListener implements IListener<TrackFinishEvent> {

  @Override
  public void handle(TrackFinishEvent event) {

    // Leave current channel after audio finished
    System.out.println("Finished playing audio.\nLeaving voice channel.");

    Main.getInstance().leaveVoiceChannel(event.getPlayer().getGuild());

    System.out.println("Left voice channel.");

    // Update stats
    Main.getInstance().played++;
    Main.getInstance().saveStats();

    System.out.println("Increased 'played' and saved stats.");
  }
}
