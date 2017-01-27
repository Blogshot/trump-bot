const stats = require('./stats.json');
const config = require('./config.js');
const Discord = require('discord.js');
const feedback = require('./util/feedback');

const client = new Discord.Client();
const token = config.token;

var played = stats.played == null ? 0 : stats.played;
var trump = stats.trump == null ? 0 : stats.trump;
var clinton = stats.clinton == null ? 0 : stats.clinton;
var merkel = stats.merkel == null ? 0 : stats.merkel;

setListeners(client);

// log our bot in
client.login(token);

function setListeners(client) {

    process.on('uncaughtException', function (exception) {
        console.log("Global error: " + exception);
    });

    process.on("unhandledRejection", err => {
        console.error("Uncaught Promise Error: \n" + err.stack);
    });

    client.on('ready', () => {
        client.user.setStatus('online', '!trump --help');
        console.log("Ready!");
    });

    client.on("disconnect", () => {
        console.log("Disconnected!");
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

    if (politician == null) {
        return;
    }

    var options = new Object();

    // default, will be overwritten by argument if needed
    options.voiceChannel = message.member.voiceChannel;
    options.play = true;
    options.file = getRandomAudio(politician);
    options.author = message.author;

    // has arguments?
    content = content.replace("!" + politician, "").trim();

    if (content != "") {
        var argumentParser = require("./util/argumentParser");
        argumentParser.parse(options, client, content, politician, guild);
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
        options.message = "I am currently needed in Channel '" + isBusy.name + "'."
        options.play = false;
    }

    if (options.play) {

        if (options.voiceChannel == null) {
            textChannel.sendMessage('You have to be in a voice channel to do this.');
        } else {
            checkMilestones(textChannel, options.author);

            playAudio(options.voiceChannel, options.file, politician, textChannel);
        }
    }

    if (options.message) {
        feedback.writeMessage(textChannel, options.message);
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
    if (!voiceChannel.permissionsFor(client.user.id).hasPermission("CONNECT")) {
        feedback.writeMessage(textChannel, "No permission to join this channel.")
        return;
    };
    if (!voiceChannel.permissionsFor(client.user.id).hasPermission("SPEAK")) {
        feedback.writeMessage(textChannel, "No permission to speak in this channel.")
        return;
    };

    voiceChannel.join().then(connection => {

        connection.playFile(file).on("end", () => {

            connection.disconnect();
            voiceChannel.leave();
            played++;

            if (politician == "trump") {
                trump++;
            }
            if (politician == "clinton") {
                clinton++;
            }
            if (politician == "merkel") {
                merkel++;
            }

            writeStats();
        });

    }).catch(error => {
        feedback.writeMessage(textChannel, error.toString());
    });
}

function checkMilestones(textChannel, user) {

    // if the sound is a multiple of 10000
    if (played + 1 % 10000 == 0) {

        console.log("Milestone reached!");

        textChannel.sendMessage(
            user.username
            + " just broke the "
            + played + 1
            + "-milestone! Congratulations, have a friendly handshake! :handshake:");
    }

}

function getRandomAudio(politician) {

    var fs = require('fs');
    var files = fs.readdirSync("./audio/" + politician);

    var index = Math.floor(Math.random() * (files.length));

    return "./audio/" + politician + "/" + files[index];
}

var lastWrite;
function writeStats() {
    
    // return if lastWrite was less than 5secs
    if ((Date.now() - lastWrite) < 5000) {
        return;
    }

    // Set current time    
    lastWrite = Date.now();

    // write current stats
    var fs = require('fs');
    var fileName = './stats.json';
    var file = require(fileName);

    file.played = played;
    file.guildCount = client.guilds.size;
    file.trump = trump;
    file.clinton = clinton;
    file.merkel = merkel;

    fs.writeFile(
        fileName,
        JSON.stringify(file, null, 2),
        function (error) {
            if (error) return console.log(error);
        }
    );
}