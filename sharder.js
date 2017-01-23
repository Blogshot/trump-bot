
const Discord = require('discord.js');
const stats = require('./stats.json');

const shardLimit = 2500;
const buffer = Math.floor(shardLimit/10);

/* Create a new manager and spawn 2 shards */
const manager = new Discord.ShardingManager('./trump-bot.js');

manager.on('launch', shard => {
    console.log("===== Launched shard " + shard.id);
});

// simulate some guilds to leave a buffer for a restart
var guilds = stats.guildCount;
var shards = Math.floor((guilds + buffer) / shardLimit) + 1;

console.log("Spawning " + shards + " shard(s)");

manager.spawn(shards, 5500);