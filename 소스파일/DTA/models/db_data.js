var mysql = require('mysql');
var async = require('async');

var db = require('./db_setting');

var pool = db.mysql_pool(mysql);

//datas = day
exports.getRank  = function(datas, callback) {
	var results = {};
	pool.getConnection(function (connErr, conn) {
		if (connErr) {
			console.error('connErr = ', connErr);
			results.message = "get Connection error";
			results.success = 0;
			results.results = [];
			callback(results);
		} else {
			var topicRankSQL = 'SELECT TOPIC, TOPIC_NO, TOPIC_RANK, TOPIC_YYYYDDMM FROM TOPIC WHERE TOPIC_YYYYDDMM=? ORDER BY TOPIC.TOPIC_RANK LIMIT 15';
			conn.query(topicRankSQL, datas, function (topicRankErr, topicRankRow) {
					if(topicRankErr) {
						console.error('topic rank sql error = ', topicRankErr);
						results.success = -1;
						results.message = 'topic rank sql error';
						results.results = [];
						conn.release();
						callback(results);
					} else {
						// 각 토픽들의 연관어를 찾아라.
						var topicWithAssociated = [];
						var topicAssociatedSQL = 'SELECT TOPIC_ASSOCIATED, ASSOCIATED_RANK FROM TOPIC_ASSOCIATED WHERE TOPIC_NO=? ORDER BY ASSOCIATED_RANK LIMIT 10';
						async.each(topicRankRow, function (topic, callback) {
							conn.query(topicAssociatedSQL, topic.TOPIC_NO, function (taErr, taRow) {
								topic.associate = taRow;
								topicWithAssociated.push(topic);
								callback();
							});
						}, function (err) {
							if (err) {
								console.error('async error = ', err);
							} else {
								var topicInfoSQL = 'SELECT NEWS.NEWS_NO, NEWS.NEWS_TITLE, NEWS.NEWS_URL, NEWS.NEWS_CO, NEWS_YYYYDDMM, NEWS_CONTENT FROM NEWS,NEWS_ANALYSIS WHERE NEWS_ANALYSIS.NEWS_NO=NEWS.NEWS_NO AND NEWS_ANALYSIS.NEWS_TOPIC=? AND NEWS.NEWS_YYYYDDMM=?';
								conn.query(topicInfoSQL, [ topicRankRow[0].TOPIC,datas], function (trErr, trRow) {
										if(trErr) {
											console.error('topic info sql error = ',trErr);
											results.success = -1;
											results.message = 'topic info sql error';
											results.results = [];
											conn.release();
											callback(results);
										} else {
											results.topicInfo = trRow;
											results.success = 1;
											results.message  = 'topic rank success';
											results.results = topicWithAssociated;
											conn.release();
											callback(results);
										}
								})
							}
						});
					}
			});
		}
	});
}

//datas = topic, YYYDDMM
exports.getTopicInfo = function (datas, callback) {
	var results = {};
	pool.getConnection(function (connErr, conn) {
		if (connErr) {
			console.error('connErr = ', connErr);
			results.message = "get Connection error";
			results.success = 0;
			results.results = [];
			callback(results);
		} else {
			var topicInfoSQL = 'SELECT NEWS.NEWS_NO, NEWS.NEWS_TITLE, NEWS.NEWS_URL, NEWS.NEWS_CO, NEWS.NEWS_CONTENT, NEWS_YYYYDDMM FROM NEWS,NEWS_ANALYSIS WHERE NEWS_ANALYSIS.NEWS_NO=NEWS.NEWS_NO AND NEWS_ANALYSIS.NEWS_TOPIC=? AND NEWS.NEWS_YYYYDDMM=?';
			conn.query(topicInfoSQL, datas, function (topicInfoErr, topicInfoRow) {
				if(topicInfoErr) {
					console.error('topic info sql error = ', topicInfoErr);
					results.success = -1;
					results.message = 'topic info sql error';
					results.results = [];
					conn.release();
					callback(results);
				} else {
					results.success = 1;
					results.message  = 'topic info success';
					results.topicInfo = topicInfoRow;
					var associateTopicSQL = 'SELECT TOPIC_ASSOCIATED.TOPIC_ASSOCIATED, TOPIC_ASSOCIATED.ASSOCIATED_RANK FROM TOPIC_ASSOCIATED, TOPIC WHERE TOPIC.TOPIC =? AND TOPIC.TOPIC_YYYYDDMM=? AND TOPIC.TOPIC_NO=TOPIC_ASSOCIATED.TOPIC_NO ORDER BY ASSOCIATED_RANK LIMIT 10';
					conn.query(associateTopicSQL, datas, function (atErr, atRow) {
						if(atErr) {
							console.error('associate topic sql error = ', topicInfoErr);
							results.success = -1;
							results.message = 'associate topic sql error';
							results.results = [];
							conn.release();
							callback(results);
						} else {
							var topicRankSQL = 'SELECT TOPIC, TOPIC_NO, TOPIC_RANK, TOPIC_YYYYDDMM FROM TOPIC WHERE TOPIC_YYYYDDMM=? ORDER BY TOPIC.TOPIC_RANK LIMIT 15';
							conn.query(topicRankSQL, datas[1], function (trErr, trRow) {
								if(trErr) {
									console.error('topic rank sql error = ', trErr);
									results.success = -1;
									results.message = 'topic rank sql error';
									results.results = [];
									conn.release();
									callback(results);
								} else {
									results.associate = atRow;
									results.topicRank = trRow;
									conn.release();
									callback(results);
								}
							});
						}
					});
				}
			});
		}
	});
}