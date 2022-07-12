const stats = require('./stats.json');
const config = require('./config.js');
const { Client, Intents } = require('discord.js');
const logger = require('./util/logger')
const fs = require('fs');

const client = new Client({ intents: [Intents.FLAGS.GUILDS, Intents.FLAGS.GUILD_MESSAGES, Intents.FLAGS.GUILD_VOICE_STATES] });
const token = config.token;

var isSharded = false;

const {	AudioPlayerStatus,
	AudioResource,
	createAudioResource,
	createAudioPlayer,
	entersState,
	joinVoiceChannel,
	VoiceConnectionStatus
} = require('@discordjs/voice');

// set listeners
setListeners(client);

// log our bot in
client.login(token);

function setListeners(client) {

    client.once('ready', () => {
        // if != null there are shards
        isSharded = (client.shard != null);

        if (!isSharded) {
            // write PID-file
            fs.writeFile(
                './trump.pid',
                parseInt(process.pid).toString(),
                function (error) {
                    if (error) {
			if (isSharded) {
        		  return logger.log(client.shard.ids, error);
			} else {
        		  return logger.log(null, error);
			}
		    }
                }
            );
        }

        client.user.setStatus('online');
        client.user.setPresence({ activities: [{name: "!trump --help"}],status: 'idle'});
    
        if (isSharded) {
            logger.log(client.shard.ids, "Ready!");
        } else {
            logger.log(null, "Ready!");
        }

        // write stats every 30 seconds
        // dont use if the bot is startet by sharder.js!
        if (!isSharded) {
            setInterval(function () {
                writeStats();
            }, 30000);
        }
        
    });

    client.on('reconnecting', () => {
	var text="Reconnecting shard";

	if (isSharded) {
          logger.log(client.shard.ids, text);
	} else {
          logger.log(null, text);
	}
    });

    client.on("disconnect", closeevent => {
        var text="Disconnected with code " + closeevent.code + " (" + closeevent.reason + ")!";
	if (isSharded) {
          logger.log(client.shard.ids, text);
	} else {
          logger.log(null, text);
	}

        // https://github.com/hammerandchisel/discord-api-docs/issues/33
        // 4005 == already authenticated
        // 4004 == authentication failed

        if (closeevent.code == 4005 ||
            closeevent.code == 4004) {
            return;
        }

	var text = "Reconnecting automatically...";
	if (isSharded) {
          logger.log(client.shard.ids, text);
	} else {
          logger.log(null, text);
	}

        client.destroy().then(() => client.login(token));

    });

    client.on('error', error => {

	if (isSharded) {
          logger.log(client.shard.ids, error);
	} else {
          logger.log(null, error);
	}
    });

    // create listener for messages
    client.on('messageCreate', message => {
        handleMessage(message);
    });

}

function handleMessage(message) {
    var content = message.content.toLowerCase();
    var textChannel = message.channel;
    var guild = message.guild;
    var author = message.author;

    var politician;

    if (content.startsWith("!trump")) {
        politician = "trump";
    }
    if (content.startsWith("!clinton")) {
        politician = "clinton";
    }
    if (content.startsWith("!merkel")) {
        politician = "merkel";
    }
    if (content.startsWith("!erdogan")) {
        politician = "erdogan";
    }
    if (content.startsWith("!farage")) {
        politician = "farage";
    }
    if (content.startsWith("!nippel")) {
        politician = "nippel";
    }

    if (politician == null) {
        return;
    }

    // make sure the text channel is a guild channel (type = text)
    if (textChannel.type != "GUILD_TEXT") {
        textChannel.send("I can't be invoked in private messages, only in guilds.");
        return;
    }

    var text = "Handling message: '" + content + "'";
    if (isSharded) {
      logger.log(client.shard.ids, text);
    } else {
      logger.log(null, text);
    }

    var options = new Object();

    // default, will be overwritten by argument if needed
    options.voiceChannel = message.member.voice.channel;
    options.file = getRandomAudio(politician);

    // has arguments?
    hasArguments = content.trim().indexOf(" ") != -1;

    if (hasArguments) {
        var argumentParser = require("./util/argumentParser");
        args = content.substring(content.trim().indexOf(" ") +1)

        logger.log(null, "Handling arguments " + args);
        argumentParser.parse(options, client, args, politician, guild, author, textChannel);
    }

    if (options.leave) {
        var voiceConnection = client.voice.connections.get(guild.id);

        if (voiceConnection) {
            voiceConnection.disconnect();
            voiceConnection.channel.leave();
        }
    }

    //var isBusy = isBusyInGuild(guild);
    var isBusy = false;

    if (isBusy) {
        textChannel.send("I am currently needed in Channel '" + isBusy.name + "'.");
        options.abort = true;
    }

    if (!options.abort) {
        if (options.voiceChannel) {
            playAudio(options.voiceChannel, options.file, textChannel);
        } else {
            textChannel.send("You have to be in a voice channel to do this.");
        }
    }
}

function isBusyInGuild(guild) {

    var connections = Array.from(client.voice.connections.values());

    for (i = 0; i < connections.length; i++) {
        var connection = connections[i];

        if (connection.channel.guild == guild) {
            return connection.channel;
        }
    }
    return false;
}


async function playAudio(voiceChannel, file, textChannel) {

    // check for permissions first
    if (!voiceChannel.permissionsFor(client.user.id).has("CONNECT")) {
        textChannel.send("No permission to join this channel.")
        return;
    };
    if (!voiceChannel.permissionsFor(client.user.id).has("SPEAK")) {
        textChannel.send("No permission to speak in this channel.")
        return;
    };

    var text = "Playing " + file;
    if (isSharded) {
      logger.log(client.shard.ids, text);
    } else {
      logger.log(null, text);
    }

    // https://github.com/discordjs/voice/blob/main/examples/music-bot/src/bot.ts#L70

    const voiceConnection = joinVoiceChannel({
	channelId: voiceChannel.id,
	guildId: voiceChannel.guild.id,
        adapterCreator: voiceChannel.guild.voiceAdapterCreator,
        selfMute: false,
        selfDeaf: false,
    });

    // Make sure the connection is ready before processing the user's request
    try {

	await entersState(voiceConnection, VoiceConnectionStatus.Ready, 20e3);

        const audioPlayer = createAudioPlayer();
        const resource = await createAudioResource(fs.createReadStream(file));

        audioPlayer.play(resource);
	voiceConnection.subscribe(audioPlayer);

        await entersState(audioPlayer, AudioPlayerStatus.Idle, 2**31-1);
        voiceConnection.destroy();

    } catch (error) {

        var text = JSON.stringify(error);
        if (isSharded) {
          logger.log(client.shard.ids, text);
        } else {
          logger.log(null, text);
        }

        textChannel.send("Failed to join voice channel.").catch(console.error);
        return;
    }
}

function getRandomAudio(politician) {

    var fs = require('fs');
    var files = fs.readdirSync("./audio/" + politician);

    var index = Math.floor(Math.random() * (files.length));

    return "./audio/" + politician + "/" + files[index];
}

function writeStats() {

    // write current stats
    var fileName = './stats.json';
    var file = require(fileName);

    file.guildCount = client.guilds.size;

    fs.writeFile(
        fileName,
        JSON.stringify(file, null, 2),
        function (error) {
            if (error) {
		if (isSharded) {
	          return logger.log(client.shard.ids, text);
		} else {
	          return logger.log(null, text);
		}
	    }
        }
    );
}
