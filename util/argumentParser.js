// tools.js
// ========
module.exports = {
    parse: function (options, client, argumentString, politician, guild) {

        var feedback = require('./feedback');

        var argumentList = getArguments(argumentString);

        options.play = false;

        for (a = 0; a < argumentList.length; a++) {

            var argument = argumentList[a].trim();

            // help-message
            if (argument == "--help" || argument == "-h") {

                /*
                print help and exit
                 */
                options.message = feedback.helpText;

                /*
                custom channel
                 */
            } else if (argument.startsWith("--channel ") || argument.startsWith("-c ")) {

                var value = argument.substring(argument.indexOf(" ") + 1);
                var found = false;

                var channels = Array.from(guild.channels.values());

                // iterate through available channels to find the specified channel
                for (i = 0; i < channels.length; i++) {

                    var channel = channels[i];

                    if (channel.type == "voice" && channel.name.toLowerCase() == value) {
                        options.voiceChannel = channel;
                        options.play = true;
                        found = true;
                        break;
                    }

                }

                /*
                if no channel was found
                 */
                if (!found) {

                    // invalid channel, report and exit
                    options.message = "I could not find the voice-channel you specified. Select one of the following:\n"
                        + getVoiceChannelList(channels);
                }

                /*
                custom sound file
                 */
            } else if (argument.startsWith("--file ") || argument.startsWith("-f ")) {

                var value = argument.substring(argument.indexOf(" ") + 1);

                // get list of matching files
                var candidates = getAudio(politician, value);

                if (candidates.length == 0) {

                    // no match found, cant continue. report and exit
                    options.message = "I could not find a filename matching the pattern you specified.";

                } else if (candidates.length > 1) {

                    // multiple matches
                    options.message = "I found multiple audios matching your pattern. Please select one of the following:\n\n"
                        + candidates.join("\n");

                } else {

                    // set the only match as desired audio
                    options.file = candidates[0];
                    options.play = true;
                }

                /*
                 list all sounds
                */
            } else if (argument == "--sounds") {

                //feedback.printSounds(client, textChannel);
                options.message = getSounds(politician);

                /*
                 print stats to channel
                */
            } else if (argument == "--stats" || argument == "--statistics") {

                //feedback.printStats(client, textChannel);
                options.message = getStats(client);

            } else if (argument == "--leave") {

                options.leave = true;

            } else {

                // unknown argument, print help and exit
                options.message = "You entered an unknown argument.";
                
            }
        }

        return JSON.stringify(options);
    }

};

function getArguments(argumentString) {

    argumentString = argumentString.split(" -").join(" --").split(" -");

    return argumentString;
}

function getVoiceChannelList(channels) {

    var voiceChannels = "";

    for (i = 0; i < channels.length; i++) {

        var channel = channels[i];

        if (channel.type == "voice") {
            voiceChannels += channel.name + "\n";
        }

    }

    return voiceChannels.trim();

}

function getStats(client) {
    const stats = require('../stats.json');

    // online for x milliseconds
    var milliseconds = client.uptime;

    var seconds = Math.floor((milliseconds / 1000) % 60);
    var minutes = Math.floor((milliseconds / (1000 * 60)) % 60);
    var hours = Math.floor(((milliseconds / (1000 * 60 * 60)) % 24));
    var days = Math.floor(((milliseconds / (1000 * 60 * 60 * 24)) % 30));

    var uptimeString = days + "d " + hours + "h " + minutes + "m " + seconds + "s";

    var seconds = Math.floor((milliseconds / 1000) % 60);
    var minutes = Math.floor((milliseconds / (1000 * 60)) % 60);
    var hours = Math.floor(((milliseconds / (1000 * 60 * 60)) % 24));
    var days = Math.floor(((milliseconds / (1000 * 60 * 60 * 24)) % 30));

    var date = new Date(Date.now() - client.uptime);

    var dateString = date.getFullYear() + "/" + (date.getMonth() + 1) + "/" + date.getDate() + " " + date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();

    return "Current stats:\n"
        + "```"
        + "Uptime of current session:\t"
        + uptimeString
        + "\n"
        + "                          \t(" + dateString + ")"
        + "\n"
        + "Currently active guilds:  \t"
        + stats.guildCount + " (" + stats.shards + " shards)"
        + "```";
}

function getAudio(politician, pattern) {

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
        if (file.match(regex)) {
            // add matched file
            candidates.push(folder + "/" + file);
        }
    }

    return candidates;
}

function getSounds(politician) {

    var fs = require('fs');
    var folder = "./audio/" + politician;
    var files = fs.readdirSync(folder);

    return files.join("\n");
}
