exports.mysql_pool = function(mysql) {
	var pool = mysql.createPool({
	    connectionLimit: 150,
	    host: 'localhost',
	    user: 'root',
	    password: 'raviewme5',
	    database: 'mydb',
	});

	return pool;
}