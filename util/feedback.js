module.exports = {

  writeMessage: function (channel, text) {

    channel.send(text).catch(error => {
      // message could not be send, try to communicate using another channel
      var channels = Array.from(channel.guild.channels.values());

      // try first channel (i=0)
      checkChannel(channels,
        text + "\n\n(This message was meant to be sent to channel '"
        + channel.name + "', but I don't have permission to write there)", 0);
    });
  },
}

const helpText = {
  "color": 2552552,
  "thumbnail": {
    "url": "https://trump.knotti.org/images/favicon.png"
  },
  "fields": [
    {
      "name": "Usage",
      "value": "!trump | !clinton | !merkel\t[Options]"
    },
    {
      "name": "Options",
      "value": "-h, --help       \tShow this message\n-c <channel>\tSpecify voice channel to join\n-f <pattern>  \tSpecify sound file to play. Wildcard: *\n--sounds        \tPMs all available sound files\n--leave            \tForce-leave the current channel"
    },
    {
      "name": "Examples",
      "value": "__!trump -f big-*.mp3__\t- equals __!trump -f big-china.mp3__\n__!clinton --sounds__\t\t- Bot PMs all sounds of Clinton\n__!merkel -c General__\t  - Bot joins channel 'General'"
    },
    {
      "name": "Support",
      "value": "If you need help, contact Bloggi#7559 or join the [support-discord](https://discord.gg/MzfyfTm)."
    },
    {
      "name": "Donations",
      "value": "Keeping the server running costs money, please consider [donating](https://trump.knotti.org)."
    }
  ]
};

function checkChannel(channels, text, i) {

  var channel = channels[i];

  // abort if we couldn't write anywhere
  if (!channel) {
    return;
  }

  // skip any non-text channel 
  if (channel.type != "text") {
    checkChannel(channels, text, i + 1);
    return;
  }

  // send message again
  channel.send(text).catch(error => {
    // if message could not be send, try next channel
    checkChannel(channels, text, i + 1);
  });
}