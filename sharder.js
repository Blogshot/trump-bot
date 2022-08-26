
const Discord = require('discord.js');
const logger = require('./util/logger')
const config = require('./config.json');
const fs = require('fs');

// write PID-file
fs.writeFile(
    './trump.pid',
    parseInt(process.pid).toString(),
    function (error) {
        if (error) return logger.log(null, error);
    }
);


var options = new Object();

options.token = config.token;
options.totalShards = 'auto';

const manager = new Discord.ShardingManager('./bot.js', options);

manager.on('shardCreate', shard => {
    logger.log(null, "=== Launched shard " + shard.id);
});

logger.log(null, "Spawning shard(s)");
manager.spawn();

// write stats all 30 seconds
setInterval(function () {
    getStats(0, 0);
}, 30000);

function getStats(index, guildCount) {

    manager.fetchClientValues('guilds.cache.size').then(results => {

      // write current stats
      var fileName = './stats.json';
      var file = require(fileName);

      file.guildCount = results.reduce((prev, val) => prev + val, 0);
      file.shards = manager.shardList.length;

      fs.writeFile(
          fileName,
          JSON.stringify(file, null, 2),
          function (error) {
              if (error) return logger.log(null, error);
          }
      );
    })
    .catch(console.error);
}

