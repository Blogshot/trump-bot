const stats = require('./stats.json');
const config = require('./config.js');
const Discord = require('discord.js');
const logger = require('./util/logger')
const fs = require('fs');

const client = new Discord.Client();
const token = config.token;

var isSharded = false;

// cache audios in memory
// TODO

// set listeners
setListeners(client);

// log our bot in
client.login(token);

function setListeners(client) {

    /*
    Disabled because this catches all kinds of errors that should be debugged instead of ignored.

    process.on('uncaughtException', function (exception) {
        logger.log("Global error: " + exception);
    });

    process.on("unhandledRejection", err => {
        logger.log("Uncaught Promise Error: \n" + err.stack);
    });
    */

    client.on('ready', () => {
        // if != null there are shards
        isSharded = (client.shard != null);

        if (!isSharded) {
            // write PID-file
            fs.writeFile(
                './trump.pid',
                process.pid,
                function (error) {
                    if (error) return logger.log(null, error);
                }
            );
        }

        client.user.setStatus('online');
        client.user.setPresence({ activity: { name: (isSharded ? "!trump --help (" + client.shard.ids + ")" : "!trump --help"), status: 'idle' } });

        logger.log(client.shard.ids, "Ready!");

        // write stats every 30 seconds
        // dont use if the bot is startet by sharder.js!
        if (!isSharded) {
            setInterval(function () {
                writeStats();
            }, 30000);
        }

    });

    client.on('reconnecting', () => {
        logger.log(client.shard.ids, "Reconnecting shard");
    });

    client.on("disconnect", closeevent => {
        logger.log(client.shard.ids, "Disconnected with code " + closeevent.code + " (" + closeevent.reason + ")!");

        // https://github.com/hammerandchisel/discord-api-docs/issues/33
        // 4005 == already authenticated
        // 4004 == authentication failed

        if (closeevent.code == 4005 ||
            closeevent.code == 4004) {
            return;
        }

        logger.log(client.shard.ids, "Reconnecting automatically...");
        client.destroy().then(() => client.login(token));

    });

    client.on('error', error => {
        logger.log(client.shard.ids, error);
    });

    // create listener for messages
    client.on('message', message => {
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
    if (textChannel.type != "text") {
        textChannel.send("I can't be invoked in private messages, only in guilds.");
        return;
    }

    logger.log(client.shard.ids, "Handling message: '" + content + "'");

    var options = new Object();

    // default, will be overwritten by argument if needed
    options.voiceChannel = message.member.voice.channel;
    options.play = true;
    options.file = getRandomAudio(politician);

    // has arguments?
    content = content.replace("!" + politician, "").trim();

    if (content != "") {
        var argumentParser = require("./util/argumentParser");
        argumentParser.parse(options, client, content, politician, guild, author, textChannel);
    }

    if (options.leave) {
        var voiceConnection = client.voice.connections.get(guild.id);

        if (voiceConnection) {
            voiceConnection.disconnect();
            voiceConnection.channel.leave();
        }
    }

    var isBusy = isBusyInGuild(guild);

    if (isBusy) {
        textChannel.send("I am currently needed in Channel '" + isBusy.name + "'.");
        options.play = false;
    }

    if (options.play && options.file !== "") {
        if (options.voiceChannel) {
            playAudio(options.voiceChannel, options.file, politician, textChannel);
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

function playAudio(voiceChannel, file, politician, textChannel) {

    // check for permissions first
    if (!voiceChannel.permissionsFor(client.user.id).has("CONNECT")) {
        textChannel.send("No permission to join this channel.")
        return;
    };
    if (!voiceChannel.permissionsFor(client.user.id).has("SPEAK")) {
        textChannel.send("No permission to speak in this channel.")
        return;
    };

    logger.log(client.shard.ids, "Playing " + file);

    voiceChannel.join().then(connection => {

        connection.play(file).on("speaking", speaking => {
            if (!speaking) {
                connection.disconnect();
                voiceChannel.leave();
            }
        });

    }).catch(error => {
	logger.log(client.shard.ids, JSON.stringify(error));
    });
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
            if (error) return logger.log(client.shard.ids, error);
        }
    );
}
