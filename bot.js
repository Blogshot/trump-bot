const stats = require('./stats.json');
const config = require('./config.json');
const { Client, GatewayIntentBits, SlashCommandBuilder, InteractionType, ChannelType, PermissionsBitField } = require('discord.js');
const {	AudioPlayerStatus, createAudioResource, createAudioPlayer, entersState, joinVoiceChannel, VoiceConnectionStatus} = require('@discordjs/voice');
const logger = require('./util/logger')
const fs = require('fs');

const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.GuildVoiceStates] });
const token = config.token;

var isSharded = false;

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
        client.user.setPresence({ activities: [{name: "/trump"}],status: 'idle'});

        registerCommands();
    
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
}

function registerCommands() {

    const { REST } = require('@discordjs/rest');
    const { Routes } = require('discord.js');
    
    const commands = [];
    
    var politicians = new Array("trump", "clinton", "merkel", "erdogan", "farage", "nippel")

    for (i=0; i<politicians.length; i++) {
        var politician = politicians[i];
       
        var data = new SlashCommandBuilder()
            .setName(politician)
            .setDescription('Plays a random quote from ' + politician)
            .addStringOption(option =>
                option.setName('quote')
                    .setDescription('Specify quote to play')
                    .setRequired(false)
                    .setAutocomplete(true)
            )
            .addChannelOption(option =>
                option.setName('channel')
                    .setDescription('Specify voice channel to join')
                    .setRequired(false)
                    .addChannelTypes(ChannelType.GuildVoice)
            );

        commands.push(data.toJSON());
    }

    const rest = new REST({ version: '10' }).setToken(token);
    
    (async () => {
        try {   
            await rest.put(
                Routes.applicationCommands(client.user.id),
                { body: commands },
            );
    
        } catch (error) {
            logger.err(null, error);
        }
    })();

}

client.on('interactionCreate', async interaction => {
    // only register slash commands and autocomplete
    if (!interaction.isChatInputCommand() && !interaction.isAutocomplete) {
        console.log("exit")
        return;
    } 
    
    if (interaction.type == InteractionType.ApplicationCommandAutocomplete) {

        const focusedValue = interaction.options.getFocused(true);

        if (focusedValue.name == 'quote') {
            const politician = interaction.commandName;

            var sounds = getAudio(politician, focusedValue.value, true);
        
            if (sounds.length >25) {
                sounds = sounds.slice(0,24);
            }

            await interaction.respond(
                sounds.map(choice => ({ name: choice, value: choice })),
            );
        }
    }

    if (interaction.type == InteractionType.ApplicationCommand) {

        var command = interaction.commandName;
        
        var politician = command;
        var author = interaction.member;
        var voiceChannel = interaction.options.getChannel('channel')
        var file = interaction.options.getString('quote');

        // fall back to author's voice channel if not specified
        if (voiceChannel == null) {
            voiceChannel = author.voice.channel;
        }

        // fall back to random audio if not specified
        if (file == null) {
            file = await getRandomAudio(politician)
        } else {
            file = "./audio/" + politician + "/" + file;
        }

        if (!fs.existsSync(file)) {

            console.log(file)
            await interaction.reply({
                content: 'You have entered an invalid quote. Please make sure to Discord\'s autocomplete feature.',
                ephemeral: true
            });
            return;
        }

        // throw error if voiceChannel is still null: No option was given and the author is not in a voice channel
        if (voiceChannel == null) {
            await interaction.reply({
                content: 'You are not in a voicechannel or have not specified a channel via the \'channel\' parameter.',
                ephemeral: true
            });
            return;
        }

        
        var fileWrap = file.substring(file.lastIndexOf("/") +1, file.lastIndexOf(".ogg"))
        await interaction.reply({
            content: `Playing \`${fileWrap}\` in channel ${voiceChannel}.`,
            ephemeral: true
        });

        playAudio(voiceChannel, file)
    }
    
});

async function playAudio(voiceChannel, file) {

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
        
    } catch (error) {

        if (isSharded) {
          logger.err(client.shard.ids, error.toString());
        } else {
          logger.err(null, error.toString());
        }
        return;
    }
    finally {
        voiceConnection.destroy();
    }
}

function getRandomAudio(politician) {

    var fs = require('fs');
    var files = fs.readdirSync("./audio/" + politician);

    var index = Math.floor(Math.random() * (files.length));

    return "./audio/" + politician + "/" + files[index];
}

function getAudio(politician, pattern, shorten) {

    // "shorten" is a boolean that tells the function to return only the file name, not the whole path
    var fs = require('fs');

    var folder = "./audio/" + politician;
    var files = fs.readdirSync(folder);

    var candidates = [];
    // "^.*" + pattern + ".*";
    var regex = "^" + pattern.split("*").join(".*");

    // iterate through available files to find matching ones
    for (i = 0; i < files.length; i++) {

        var file = files[i];

        // get matches
        if (file.indexOf(pattern) != -1 || file.match(regex)) {
            // add matched file
            if (shorten) {
                candidates.push(file);
            } else {
                candidates.push(folder + "/" + file);
            }
        }
    }

    return candidates;
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