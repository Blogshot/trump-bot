module.exports = {

    log: function (shard, message) {
        var date = new Date();

        var shard_string = (shard == null) ? "" : " ==== " + shard;

        // "[30.1.2017 10:44:19] - Here is your message."
        console.log(
            "[" + date.toLocaleDateString() + " " + date.toLocaleTimeString() + "] " +
            shard_string + " " + JSON.stringify(message, null, 2)
        );
    },

    err: function (shard, message) {
        var date = new Date();

        var shard_string = (shard == null) ? "" : " ==== " + shard;

        // "[30.1.2017 10:44:19] - Here is your message."
        console.error(
            "[" + date.toLocaleDateString() + " " + date.toLocaleTimeString() + "] " +
            shard_string + " " + JSON.stringify(message, null, 2)
        );
    }
}
