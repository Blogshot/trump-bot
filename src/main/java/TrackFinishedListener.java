import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.audio.events.TrackFinishEvent;

public class TrackFinishedListener implements IListener<TrackFinishEvent> {
  
  @Override
  public void handle(TrackFinishEvent event) { // This is called when the ReadyEvent is dispatched
  
    // Leave current channel after audio finished and set it to null again
    Main.removeGuildFromList(event.getPlayer().getGuild());
    
    // Update stats
    Main.played++;
    Main.saveStats();
    
  }
}