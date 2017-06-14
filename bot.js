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

        logger.log(client.shard, "Ready!");

        // wait 10 seconds after ready to ensure readiness and set status
        setTimeout(function () {
            logger.log(client.shard, "Status set");
            client.user.setStatus('online');
            client.user.setGame(isSharded ? "!trump --help (" + client.shard.id + ")" : "!trump --help");
        }, 10000);

        // write stats all 30 seconds
        setInterval(function () {
            writeStats();
        }, 30000);

    });

    client.on('reconnecting', () => {
        logger.log(client.shard, "Reconnecting shard");
    });

    client.on("disconnect", closeevent => {
        logger.log(client.shard, "Disconnected with code " + closeevent.code + " (" + closeevent.reason + ")!");

        // https://github.com/hammerandchisel/discord-api-docs/issues/33
        // 4005 == already authenticated
        // 4004 == authentication failed

        if (closeevent.code == 4005 ||
            closeevent.code == 4004) {
            return;
        }

        logger.log(client.shard, "Reconnecting automatically...");
        client.destroy().then(() => client.login(token))

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

    if (politician == null) {
        return;
    }

    logger.log(client.shard, "  Handling message: '" + content + "'")

    var options = new Object();

    // default, will be overwritten by argument if needed
    options.voiceChannel = message.member.voiceChannel;
    options.play = true;
    options.file = getRandomAudio(politician);

    // has arguments?
    content = content.replace("!" + politician, "").trim();

    if (content != "") {
        var argumentParser = require("./util/argumentParser");
        argumentParser.parse(options, client, content, politician, guild, author, textChannel);
    }

    if (options.leave) {
        var voiceConnection = client.voiceConnections.get(guild.id);

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

    if (options.play) {
        if (options.voiceChannel) {
            playAudio(options.voiceChannel, options.file, politician, textChannel);
        } else {
            textChannel.send("You have to be in a voice channel to do this.");
        }
    }
}

function isBusyInGuild(guild) {

    var connections = Array.from(client.voiceConnections.values());

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

    voiceChannel.join().then(connection => {

        connection.playFile(file).on("end", () => {
            connection.disconnect();
            voiceChannel.leave();
        });

    }).catch(error => {
        textChannel.send(error.toString());
    });
}

function getRandomAudio(politician) {

    var fs = require('fs');
    var files = fs.readdirSync("./audio/" + politician);

    var index = Math.floor(Math.random() * (files.length));

    return "./audio/" + politician + "/" + files[index];
}

function writeStats(stats) {


    // if there are no stats to write, create them!
    if (stats == null) {

        stats = new Object();
        stats.guildCount = 0;
        stats.shards = 0;       // 0: no sharding

        if (isSharded) {
            client.shard.fetchClientValues('guilds.size').then(results => {

                for (var i = 0; i < results.length; i++) {
                    stats.guildCount += results[i];
                }
                stats.shards = client.shard.count;

                // now that we've got stats, call it again this function again
                writeStats(stats);
                return;

            }).catch();
        } else {
            stats.guildCount = client.guilds.size;
        }
    }

    // write current stats
    var fileName = './stats.json';
    var file = require(fileName);

    file.guildCount = stats.guildCount;
    file.shards = stats.shards;

    fs.writeFile(
        fileName,
        JSON.stringify(file, null, 2),
        function (error) {
            if (error) return logger.log(client.shard, error);
        }
    );
}