
const Discord = require('discord.js');
const logger = require('./util/logger')
const config = require('./config.js');

var options = new Object();

options.token = config.token;
options.totalShards = 'auto';

const manager = new Discord.ShardingManager('./bot.js', options);

manager.on('launch', shard => {
    logger.log(null, "=== Launched shard " + shard.id);
});

logger.log(null, "Spawning shard(s)");
manager.spawn();



