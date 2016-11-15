import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.audio.events.TrackStartEvent;

public class TrackStartedListener implements IListener<TrackStartEvent> {
  
   @Override
    public void handle(TrackStartEvent event) { // This is called when the ReadyEvent is dispatched
  
     Main.isBusy = true;
      
    }
}