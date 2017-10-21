
const Discord = require('discord.js');
const logger = require('./util/logger')
const config = require('./config.js');
const fs = require('fs');

// write PID-file
fs.writeFile('./trump.pid', process.pid);

var options = new Object();

options.token = config.token;
options.totalShards = 'auto';

const manager = new Discord.ShardingManager('./bot.js', options);

manager.on('launch', shard => {
    logger.log(null, "=== Launched shard " + shard.id);
});

logger.log(null, "Spawning shard(s)");
manager.spawn();


// write stats all 30 seconds
setInterval(function () {
    getStats(0, 0);
}, 30000);

function getStats(index, guildCount) {

    var shardList = manager.shards.array();

    // if there are more shards to go
    if (index < shardList.length) {

        // get guildCount of that shard and recurse 
        shardList[index].fetchClientValue('guilds.size').then(count => {
            guildCount += count;

            index++;
            getStats(index, guildCount)
        });
        return;
    } else {
        // if all shards have been searched

        // write current stats
        var fileName = './stats.json';
        var file = require(fileName);

        file.guildCount = guildCount;
        file.shards = shardList.length;

        fs.writeFile(
            fileName,
            JSON.stringify(file, null, 2),
            function (error) {
                if (error) return logger.log(null, error);
            }
        );
    }
}

