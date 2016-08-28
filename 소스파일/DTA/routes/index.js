var express = require('express');
var router = express.Router();
var db = require('../models/db_data');

associatedLink = function (topics, callback) {
  var links = [];
	var associatedTopics = [];
	for(var j = 0; j< topics.length; j++){
		for (var k = 0; k < topics.length; k++) {
			associatedTopics = topics[j].associate;
			for(var i = 0; i <associatedTopics.length; i++) {
				if(topics[k].TOPIC == associatedTopics[i].TOPIC_ASSOCIATED) {
					if(k > j){
						links.push(k+"-"+j);
					} else {
						links.push(j+"-"+k);
					}
				}
			}
		}
	}
	callback(links);
}

router.get('/', function (req, res, next) {
	res.redirect('/main');
});


/* GET home page. */
// router.get('/', function (req, res, next) {
//   res.render('index', { title: 'express',
//   											topic : ['topic1', 'topic2', 'topic3', 'topic4', 'topic5', 'topic6', 'topic7', 'topic8', 'topic9', 'topic10', 'topic11', 'topic12'] });
// });

// router.get('/example', function (req, res, next) {
//   res.render('example', { title: 'express',
//   											topic : ['topic1', 'topic2', 'topic3', 'topic4', 'topic5', 'topic6', 'topic7', 'topic8', 'topic9', 'topic10', 'topic11', 'topic12'] });
// });

//해당 날짜의 랭킹 리스트
router.get('/rank/:day', function (req, res, next) {
	var dt = new Date();
	var day = req.params.day;
	var today = new Date(dt);
	db.getRank(day, function (results){
		var yyyyddmm = results.results[0].TOPIC_YYYYDDMM;
		var topics = [];
		var datas = results.results;
		datas.forEach (function (topic) {
			topics.push(topic.TOPIC);
		});
		console.log('topics  = ', topics);
		console.log('yyyyddmm  = ', yyyyddmm);
		associatedLink ( datas, function (link) {
			console.log('link = ', link);
			res.render('Prototype/html/Main', {title : 'daily rank', topics: topics, yyyyddmm : yyyyddmm, link : link, topicInfo : results.topicInfo, today : today});
		});
		// res.render('daily_rank', {title : 'daily rank', topics: topics, yyyyddmm : yyyyddmm});
	})
});

//해당 토픽의 연관어와 기사들
router.get('/rank/:day/:topic', function (req, res, next) {
	var dt = new Date();
	var topic = req.params.topic;
	var day = req.params.day;
	var datas = [topic, day];

	var today = new Date(dt);
	db.getTopicInfo(datas, function (results) {
		var associate = results.associate;
		var topicInfo = results.topicInfo;
		var topics = [];
		var topicDatas = results.topicRank;
		var newsNo = [];
		var newsTitle = "";
		var newsUrl = [];
		var newsCo = [];
		var associateArray = [];
		topicDatas.forEach ( function (tr) {
			topics.push(tr.TOPIC);
		});
		associate.forEach (function (data) {
			associateArray.push(data.TOPIC_ASSOCIATED);
		});
		topicInfo.forEach (function (data) {
			newsNo.push(data.NEWS_NO);
			newsTitle = newsTitle + "\%\%"+data.NEWS_TITLE;
			newsUrl.push(data.NEWS_URL);
			newsCo.push(data.NEWS_CO);
		});
		console.log('topicInfo = ' ,topicInfo);
		console.log('topics = ', topics);

		res.render('Prototype/html/specific', {
			topicInfo : topicInfo,
			associate : associateArray,
			newsNo : newsNo,
			newsTitle : newsTitle,
			newsUrl : newsUrl,
			newsCo : newsCo,
			title : 'daily topic',
			yyyyddmm : day,
			topic : topic,
			topics : topics,
			today : today
		})
	});
});

router.get('/main', function (req, res, next) {
	var dt = new Date();
	var yesterday = new Date(dt);
	var today = new Date(dt);
	yesterday.setDate(dt.getDate() -1);

	var month = yesterday.getMonth()+1;
	if(month<10){
		month = '0'+month;
	}
	var day = yesterday.getDate();
	if(day < 10){
		day='0'+day;
	}
	var year = dt.getFullYear();
	var yyyyddmm = year+''+month+''+day;
	console.log('yyyyddmm = ', yyyyddmm);
	yyyyddmm = '20151208';
	db.getRank(yyyyddmm, function (results){
		var yyyyddmm = results.results[0].TOPIC_YYYYDDMM;
		var topics = [];
		var datas = results.results;
		datas.forEach (function (topic) {
			topics.push(topic.TOPIC);
		});
		console.log('topics  = ', topics);
		console.log('yyyyddmm  = ', yyyyddmm);
		console.log(results.topicInfo);
		associatedLink ( datas, function (link) {
			console.log('link = ', link);
			res.render('Prototype/html/Index', {title : 'daily trend analysis', topics: topics, yyyyddmm : yyyyddmm, link : link, topicInfo : results.topicInfo, today : today});
		});
		// res.render('daily_rank', {title : 'daily rank', topics: topics, yyyyddmm : yyyyddmm});
	})
});

module.exports = router;



